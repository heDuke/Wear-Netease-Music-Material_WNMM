package com.refresh.netmusic.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.refresh.netmusic.data.SessionStore
import com.refresh.netmusic.ncm.api.NeteaseApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val api: NeteaseApi,
    private val session: SessionStore,
) : ViewModel() {
    private val _quality = MutableStateFlow(session.quality())
    val quality = _quality.asStateFlow()
    private val _loggedOut = MutableStateFlow(false)
    val loggedOut = _loggedOut.asStateFlow()

    fun setQuality(level: String) {
        viewModelScope.launch {
            session.setQuality(level)
            _quality.value = level
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { api.logout() }
            session.clear()
            _loggedOut.value = true
        }
    }
}
