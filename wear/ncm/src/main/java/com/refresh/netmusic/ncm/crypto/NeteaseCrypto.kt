package com.refresh.netmusic.ncm.crypto

import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * Port of api-enhanced `util/crypto.js` weapi / eapi / linuxapi.
 */
object NeteaseCrypto {
    private const val PRESET_KEY = "0CoJUm6Qyw8W8jud"
    private const val IV = "0102030405060708"
    private const val LINUX_KEY = "rFgB&h#%2?^eDg:Q"
    private const val EAPI_KEY = "e82ckenh8dichen8"
    private const val BASE62 = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private val RSA_MODULUS = BigInteger(
        "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e70203",
        16,
    )
    private val RSA_EXPONENT = BigInteger("010001", 16)
    private val random = SecureRandom()

    data class WeapiPayload(val params: String, val encSecKey: String)
    data class EapiPayload(val params: String)
    data class LinuxPayload(val eparams: String)

    fun weapi(json: String, secretKey: String = randomSecretKey()): WeapiPayload {
        val first = aesCbcBase64(json, PRESET_KEY)
        val params = aesCbcBase64(first, secretKey)
        val encSecKey = rsaEncrypt(secretKey.reversed())
        return WeapiPayload(params, encSecKey)
    }

    fun eapi(url: String, json: String): EapiPayload {
        val message = "nobody${url}use${json}md5forencrypt"
        val digest = md5Hex(message)
        val data = "$url-36cd479b6b5-$json-36cd479b6b5-$digest"
        return EapiPayload(aesEcbHex(data, EAPI_KEY))
    }

    fun linuxapi(json: String): LinuxPayload {
        return LinuxPayload(aesEcbHex(json, LINUX_KEY))
    }

    fun eapiDecrypt(hexCipher: String): String {
        return aesEcbDecrypt(hexCipher, EAPI_KEY)
    }

    fun randomSecretKey(): String {
        val chars = CharArray(16)
        for (i in chars.indices) {
            chars[i] = BASE62[random.nextInt(BASE62.length)]
        }
        return String(chars)
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun aesCbcBase64(plain: String, key: String): String {
        val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"),
            IvParameterSpec(IV.toByteArray(Charsets.UTF_8)),
        )
        return Base64.encode(cipher.doFinal(plain.toByteArray(Charsets.UTF_8)))
    }

    private fun aesEcbHex(plain: String, key: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"))
        return cipher.doFinal(plain.toByteArray(Charsets.UTF_8)).toHex().uppercase()
    }

    private fun aesEcbDecrypt(hexCipher: String, key: String): String {
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key.toByteArray(Charsets.UTF_8), "AES"))
        return String(cipher.doFinal(hexCipher.hexToBytes()), Charsets.UTF_8)
    }

    private fun rsaEncrypt(text: String): String {
        val data = BigInteger(1, text.toByteArray(Charsets.UTF_8))
        return data.modPow(RSA_EXPONENT, RSA_MODULUS).toString(16).padStart(256, '0')
    }

    private fun md5Hex(text: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(text.toByteArray(Charsets.UTF_8))
        return digest.toHex()
    }

    private fun ByteArray.toHex(): String {
        val out = StringBuilder(size * 2)
        for (b in this) {
            out.append(((b.toInt() ushr 4) and 0xF).toString(16))
            out.append((b.toInt() and 0xF).toString(16))
        }
        return out.toString()
    }

    private fun String.hexToBytes(): ByteArray {
        val clean = if (length % 2 == 0) this else "0$this"
        return ByteArray(clean.length / 2) { i ->
            clean.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }
}
