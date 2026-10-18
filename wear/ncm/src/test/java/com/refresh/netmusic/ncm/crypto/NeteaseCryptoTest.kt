package com.refresh.netmusic.ncm.crypto

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NeteaseCryptoTest {
    @Test
    fun eapiIsDeterministicHex() {
        val payload = NeteaseCrypto.eapi("/api/login/qrcode/unikey", """{"type":"3"}""")
        assertEquals(payload.params.uppercase(), payload.params)
        assertTrue(payload.params.length > 32)
        assertTrue(payload.params.matches(Regex("[0-9A-F]+")))
    }

    @Test
    fun eapiRoundTrip() {
        val json = """{"hello":"world"}"""
        val url = "/api/song/lyric/v1"
        val encrypted = NeteaseCrypto.eapi(url, json).params
        val decrypted = NeteaseCrypto.eapiDecrypt(encrypted)
        assertTrue(decrypted.contains(json))
        assertTrue(decrypted.startsWith(url))
    }

    @Test
    fun weapiWithFixedKey() {
        val we = NeteaseCrypto.weapi("""{"csrf_token":""}""", "0123456789abcdef")
        assertTrue(we.params.isNotBlank())
        assertEquals(256, we.encSecKey.length)
    }

    @Test
    fun linuxapiHex() {
        val linux = NeteaseCrypto.linuxapi("""{"method":"POST"}""")
        assertTrue(linux.eparams.matches(Regex("[0-9A-F]+")))
    }
}
