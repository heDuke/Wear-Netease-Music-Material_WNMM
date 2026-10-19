package com.refresh.netmusic.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val Context.dataStore by preferencesDataStore("netmusic")

@Serializable
private data class SessionPayload(val cookies: String = "", val uid: Long = 0)

class SessionStore(private val context: Context) {
    private val cookiesKey = stringPreferencesKey("cookies")
    private val sessionKey = stringPreferencesKey("session_enc")
    private val qualityKey = stringPreferencesKey("quality")
    private val json = Json { ignoreUnknownKeys = true }

    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, KEYSET_NAME, PREF_FILE)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    }

    fun loadCookies(): Map<String, String> = parseCookies(loadPayload().cookies)

    fun uid(): Long = loadPayload().uid

    suspend fun saveCookies(header: String) {
        persist(loadPayload().copy(cookies = header))
    }

    suspend fun saveUid(id: Long) {
        persist(loadPayload().copy(uid = id))
    }

    suspend fun clear() {
        context.dataStore.edit {
            it.remove(sessionKey)
            it.remove(cookiesKey)
        }
    }

    fun quality(): String = runBlocking {
        context.dataStore.data.first()[qualityKey] ?: "higher"
    }

    suspend fun setQuality(level: String) {
        context.dataStore.edit { it[qualityKey] = level }
    }

    private fun loadPayload(): SessionPayload = runBlocking {
        val enc = context.dataStore.data.first()[sessionKey]
        if (!enc.isNullOrBlank()) {
            return@runBlocking runCatching { decrypt(enc) }.getOrDefault(SessionPayload())
        }
        val legacy = context.dataStore.data.first()[cookiesKey].orEmpty()
        if (legacy.isBlank()) SessionPayload()
        else {
            val migrated = SessionPayload(cookies = legacy)
            persist(migrated)
            context.dataStore.edit { it.remove(cookiesKey) }
            migrated
        }
    }

    private suspend fun persist(payload: SessionPayload) {
        val encoded = encrypt(payload)
        context.dataStore.edit {
            it[sessionKey] = encoded
            it.remove(cookiesKey)
        }
    }

    private fun encrypt(payload: SessionPayload): String {
        val plain = json.encodeToString(SessionPayload.serializer(), payload).toByteArray(Charsets.UTF_8)
        val cipher = aead.encrypt(plain, ByteArray(0))
        return Base64.encodeToString(cipher, Base64.NO_WRAP)
    }

    private fun decrypt(encoded: String): SessionPayload {
        val cipher = Base64.decode(encoded, Base64.NO_WRAP)
        val plain = aead.decrypt(cipher, ByteArray(0))
        return json.decodeFromString(SessionPayload.serializer(), String(plain, Charsets.UTF_8))
    }

    private fun parseCookies(raw: String): Map<String, String> {
        if (raw.isBlank()) return emptyMap()
        return raw.split(';').mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq <= 0) null else part.substring(0, eq).trim() to part.substring(eq + 1).trim()
        }.toMap()
    }

    companion object {
        private const val PREF_FILE = "netmusic_tink"
        private const val KEYSET_NAME = "netmusic_aead"
        private const val MASTER_KEY_URI = "android-keystore://netmusic_master_key"
    }
}
