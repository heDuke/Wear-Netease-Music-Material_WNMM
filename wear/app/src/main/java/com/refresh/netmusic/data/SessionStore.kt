package com.refresh.netmusic.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private val Context.dataStore by preferencesDataStore("netmusic")

class SessionStore(private val context: Context) {
    private val cookiesKey = stringPreferencesKey("cookies")
    private val qualityKey = stringPreferencesKey("quality")

    fun loadCookies(): Map<String, String> = runBlocking {
        val raw = context.dataStore.data.first()[cookiesKey].orEmpty()
        if (raw.isBlank()) emptyMap()
        else raw.split(';').mapNotNull { part ->
            val eq = part.indexOf('=')
            if (eq <= 0) null else part.substring(0, eq).trim() to part.substring(eq + 1).trim()
        }.toMap()
    }

    suspend fun saveCookies(header: String) {
        context.dataStore.edit { it[cookiesKey] = header }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(cookiesKey) }
    }

    fun quality(): String = runBlocking {
        context.dataStore.data.first()[qualityKey] ?: "higher"
    }

    suspend fun setQuality(level: String) {
        context.dataStore.edit { it[qualityKey] = level }
    }
}
