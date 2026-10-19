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
    val loading: Boolean = true,
    val error: String? = null,
)

class LibraryViewModel(
    private val api: NeteaseApi,
    private val session: SessionStore,
) : ViewModel() {
    private val _state = MutableStateFlow(LibraryState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                val profile = api.loginStatus()
                val lists = profile?.let { api.userPlaylist(it.userId) }.orEmpty()
                if (profile != null) session.saveUid(profile.userId)
                session.saveCookies(api.request.cookies.asHeader())
                _state.value = LibraryState(profile, lists, loading = false)
            }.onFailure {
                _state.value = _state.value.copy(loading = false, error = it.message)
            }
        }
    }
}
