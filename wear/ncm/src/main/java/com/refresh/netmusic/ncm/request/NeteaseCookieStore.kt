package com.refresh.netmusic.ncm.request

class NeteaseCookieStore {
    private val values = linkedMapOf<String, String>()

    @Synchronized
    fun snapshot(): Map<String, String> = values.toMap()

    @Synchronized
    fun get(name: String): String? = values[name]

    @Synchronized
    fun put(name: String, value: String) {
        values[name] = value
    }

    @Synchronized
    fun putAll(map: Map<String, String>) {
        values.putAll(map)
    }

    @Synchronized
    fun replaceAll(map: Map<String, String>) {
        values.clear()
        values.putAll(map)
    }

    @Synchronized
    fun clear() {
        values.clear()
    }

    @Synchronized
    fun asHeader(): String = values.entries.joinToString("; ") { "${it.key}=${it.value}" }

    @Synchronized
    fun applySetCookie(headers: List<String>) {
        for (header in headers) {
            val first = header.split(';', limit = 2).firstOrNull() ?: continue
            val eq = first.indexOf('=')
            if (eq <= 0) continue
            val name = first.substring(0, eq).trim()
            val value = first.substring(eq + 1).trim()
            if (name.isNotEmpty()) values[name] = value
        }
    }

    fun hasLogin(): Boolean = get("MUSIC_U") != null
}
