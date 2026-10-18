package com.refresh.netmusic.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.refresh.netmusic.data.SessionStore
import com.refresh.netmusic.ncm.api.NeteaseApi
import com.refresh.netmusic.ncm.model.AccountProfile
import com.refresh.netmusic.ncm.model.Playlist
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryState(
    val profile: AccountProfile? = null,
    val playlists: List<Playlist> = emptyList(),
)

class LibraryViewModel(
    private val api: NeteaseApi,
    private val session: SessionStore,
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val profile = runCatching { api.loginStatus() }.getOrNull()
            val lists = profile?.let { runCatching { api.userPlaylist(it.userId) }.getOrDefault(emptyList()) }.orEmpty()
            _state.value = LibraryState(profile, lists)
            session.saveCookies(api.request.cookies.asHeader())
        }
    }
}
