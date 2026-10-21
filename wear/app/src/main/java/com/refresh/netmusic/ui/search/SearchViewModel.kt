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
    val error: String? = null,
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
        _state.value = _state.value.copy(query = q, loading = true, error = null)
        viewModelScope.launch {
            runCatching { api.cloudsearch(q) }
                .onSuccess { songs ->
                    _state.value = _state.value.copy(results = songs, loading = false, error = null)
                }
                .onFailure {
                    _state.value = _state.value.copy(loading = false, error = it.message ?: "搜索失败")
                }
        }
    }
}
