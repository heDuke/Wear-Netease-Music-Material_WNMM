package com.refresh.netmusic.ncm.request

import com.refresh.netmusic.ncm.crypto.NeteaseCrypto
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.parametersOf
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.UUID
import kotlin.random.Random

enum class CryptoMode { WEAPI, EAPI, API }

data class NeteaseResponse(
    val status: Int,
    val body: JsonObject,
    val cookies: List<String>,
)

class NeteaseRequest(
    val cookies: NeteaseCookieStore = NeteaseCookieStore(),
    private val json: Json = Json { ignoreUnknownKeys = true; isLenient = true },
) {
    private val deviceId: String = cookies.get("deviceId") ?: UUID.randomUUID().toString().replace("-", "")
    private val wnmcid: String = cookies.get("WNMCID") ?: run {
        val prefix = (1..6).map { 'a' + Random.nextInt(26) }.joinToString("")
        "$prefix.${System.currentTimeMillis()}.01.0"
    }

    val http: HttpClient = HttpClient(CIO) {
        expectSuccess = false
        install(HttpTimeout) {
            requestTimeoutMillis = 20_000
            connectTimeoutMillis = 10_000
        }
        defaultRequest {
            header("Accept", "*/*")
        }
    }

    suspend fun call(
        uri: String,
        data: Map<String, Any?> = emptyMap(),
        crypto: CryptoMode = CryptoMode.EAPI,
    ): NeteaseResponse {
        ensureBaseCookies()
        val payload = data.mapValues { (_, v) -> v?.toString() ?: "" }.toMutableMap()
        val csrf = cookies.get("__csrf").orEmpty()

        val headers = linkedMapOf<String, String>()
        val form: Parameters
        val url: String

        when (crypto) {
            CryptoMode.WEAPI -> {
                payload["csrf_token"] = csrf
                val we = NeteaseCrypto.weapi(toJsonObject(payload).toString())
                form = parametersOf("params" to listOf(we.params), "encSecKey" to listOf(we.encSecKey))
                url = "$DOMAIN/weapi/${uri.removePrefix("/api/")}"
                headers["Referer"] = DOMAIN
                headers["User-Agent"] = UA_WEAPI
                headers["Cookie"] = fullCookieHeader()
            }
            CryptoMode.EAPI -> {
                val header = eapiHeader(csrf)
                payload["header"] = header.toString()
                val e = NeteaseCrypto.eapi(uri, toJsonObject(payload).toString())
                form = parametersOf("params" to listOf(e.params))
                url = "$EAPI_DOMAIN/eapi/${uri.removePrefix("/api/")}"
                headers["User-Agent"] = UA_IPHONE
                headers["Cookie"] = headerCookie(header)
            }
            CryptoMode.API -> {
                form = Parameters.build {
                    payload.forEach { (k, v) -> append(k, v) }
                }
                url = "$API_DOMAIN$uri"
                headers["User-Agent"] = UA_IPHONE
                headers["Cookie"] = fullCookieHeader()
            }
        }

        val response = http.submitForm(url = url, formParameters = form) {
            headers.forEach { (k, v) -> header(k, v) }
        }
        val setCookies = response.headers.getAll("Set-Cookie").orEmpty()
        cookies.applySetCookie(setCookies.map { it.substringBefore(';') })

        val text = response.bodyAsText()
        val body = parseBody(text)
        val code = body["code"]?.jsonPrimitive?.contentOrNull?.toIntOrNull() ?: response.status.value
        val status = if (code in SPECIAL_OK) 200 else code
        return NeteaseResponse(status, body, setCookies)
    }

    fun close() {
        http.close()
    }

    private fun ensureBaseCookies() {
        cookies.put("os", cookies.get("os") ?: "android")
        cookies.put("appver", cookies.get("appver") ?: APPVER)
        cookies.put("osver", cookies.get("osver") ?: "14")
        cookies.put("channel", cookies.get("channel") ?: "xiaomi")
        cookies.put("deviceId", deviceId)
        cookies.put("WNMCID", wnmcid)
        cookies.put("__remember_me", "true")
    }

    private fun eapiHeader(csrf: String): JsonObject = buildJsonObject {
        put("osver", cookies.get("osver") ?: "14")
        put("deviceId", deviceId)
        put("os", cookies.get("os") ?: "android")
        put("appver", cookies.get("appver") ?: APPVER)
        put("versioncode", "140")
        put("mobilename", "")
        put("buildver", (System.currentTimeMillis() / 1000).toString())
        put("resolution", "1920x1080")
        put("__csrf", csrf)
        put("channel", cookies.get("channel") ?: "xiaomi")
        put("requestId", "${System.currentTimeMillis()}_${Random.nextInt(0, 1000).toString().padStart(4, '0')}")
        cookies.get("MUSIC_U")?.let { put("MUSIC_U", it) }
        cookies.get("MUSIC_A")?.let { put("MUSIC_A", it) }
    }

    private fun headerCookie(header: JsonObject): String =
        header.entries.joinToString("; ") { (k, v) ->
            "$k=${(v as? JsonPrimitive)?.content.orEmpty()}"
        }

    private fun fullCookieHeader(): String {
        ensureBaseCookies()
        return cookies.asHeader()
    }

    private fun parseBody(text: String): JsonObject {
        runCatching { return json.parseToJsonElement(text).jsonObject }
        val trimmed = text.trim()
        if (trimmed.isNotEmpty() && trimmed.all { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }) {
            runCatching {
                return json.parseToJsonElement(NeteaseCrypto.eapiDecrypt(trimmed)).jsonObject
            }
        }
        return buildJsonObject { put("raw", text) }
    }

    private fun toJsonObject(map: Map<String, String>): JsonObject = buildJsonObject {
        map.forEach { (k, v) ->
            if (k == "header") {
                runCatching { put(k, json.parseToJsonElement(v)) }
                    .onFailure { put(k, v) }
            } else {
                put(k, v)
            }
        }
    }

    companion object {
        const val DOMAIN = "https://music.163.com"
        const val API_DOMAIN = "https://interface.music.163.com"
        const val EAPI_DOMAIN = "https://interface3.music.163.com"
        const val APPVER = "8.20.20.231215173437"
        const val UA_WEAPI =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Edg/124.0.0.0"
        const val UA_IPHONE = "NeteaseMusic 9.0.90/5038 (iPhone; iOS 16.2; zh_CN)"
        private val SPECIAL_OK = setOf(200, 201, 302, 400, 502, 800, 801, 802, 803)
    }
}
