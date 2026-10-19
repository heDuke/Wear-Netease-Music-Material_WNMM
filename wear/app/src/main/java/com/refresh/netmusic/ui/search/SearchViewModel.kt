package com.refresh.netmusic.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.refresh.netmusic.ncm.api.NeteaseApi
import com.refresh.netmusic.ncm.model.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SearchState(
    val query: String = "",
    val hots: List<String> = emptyList(),
    val results: List<Song> = emptyList(),
    val loading: Boolean = false,
)

class SearchViewModel(private val api: NeteaseApi) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val hots = runCatching { api.searchHot() }.getOrDefault(emptyList())
            _state.value = _state.value.copy(hots = hots)
        }
    }

    fun search(q: String) {
        _state.value = _state.value.copy(query = q, loading = true)
        viewModelScope.launch {
            val songs = runCatching { api.cloudsearch(q) }.getOrDefault(emptyList())
            _state.value = _state.value.copy(results = songs, loading = false)
        }
    }
}
