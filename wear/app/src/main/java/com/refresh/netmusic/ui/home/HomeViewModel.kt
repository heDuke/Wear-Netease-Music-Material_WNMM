package com.refresh.netmusic.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.refresh.netmusic.ncm.api.NeteaseApi
import com.refresh.netmusic.ncm.model.Playlist
import com.refresh.netmusic.ncm.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class HomeState(
    val daily: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class HomeViewModel(private val api: NeteaseApi) : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            runCatching {
                val daily = api.recommendSongs()
                val rec = api.recommendResource()
                val extra = if (rec.isEmpty()) api.personalized() else rec
                _state.value = HomeState(daily = daily, playlists = extra, loading = false)
            }.onFailure {
                _state.value = _state.value.copy(loading = false, error = it.message)
            }
        }
    }
}
