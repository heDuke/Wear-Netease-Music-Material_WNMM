package com.refresh.netmusic.ui.playlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.refresh.netmusic.ncm.api.NeteaseApi
import com.refresh.netmusic.ncm.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlaylistUiState(
    val songs: List<Song> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class PlaylistViewModel(private val api: NeteaseApi) : ViewModel() {
    private val _state = MutableStateFlow(PlaylistUiState())
    val state = _state.asStateFlow()

    fun load(id: Long) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching { api.playlistTrackAll(id) }
                .onSuccess { _state.value = PlaylistUiState(songs = it, loading = false) }
                .onFailure { _state.value = _state.value.copy(loading = false, error = it.message) }
        }
    }
}
