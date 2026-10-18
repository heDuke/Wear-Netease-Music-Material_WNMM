package com.refresh.netmusic.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.refresh.netmusic.ncm.api.NeteaseApi
import com.refresh.netmusic.ncm.model.Comment
import com.refresh.netmusic.ncm.model.Lyrics
import com.refresh.netmusic.player.PlayerHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PlayerUi(
    val title: String = "",
    val artist: String = "",
    val playing: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val lyrics: Lyrics = Lyrics(emptyList(), ""),
    val comments: List<Comment> = emptyList(),
    val liked: Boolean = false,
)

class PlayerViewModel(
    private val api: NeteaseApi,
) : ViewModel() {
    private val _state = MutableStateFlow(PlayerUi())
    val state = _state.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                val exo = PlayerHolder.player
                if (exo != null) {
                    val meta = exo.mediaMetadata
                    val id = exo.currentMediaItem?.mediaId?.toLongOrNull()
                    _state.value = _state.value.copy(
                        title = meta.title?.toString().orEmpty(),
                        artist = meta.artist?.toString().orEmpty(),
                        playing = exo.isPlaying,
                        position = exo.currentPosition,
                        duration = exo.duration.coerceAtLeast(0),
                    )
                    if (id != null && id != lastId) {
                        lastId = id
                        loadExtras(id)
                    }
                }
                delay(400)
            }
        }
    }

    private var lastId: Long = -1

    private fun currentPlayer(): Player? = PlayerHolder.player

    private fun loadExtras(id: Long) {
        viewModelScope.launch {
            val lyrics = runCatching { api.lyricNew(id) }.getOrDefault(Lyrics(emptyList(), ""))
            val comments = runCatching { api.commentMusic(id) }.getOrDefault(emptyList())
            _state.value = _state.value.copy(lyrics = lyrics, comments = comments)
        }
    }

    fun toggle() {
        val p = currentPlayer() ?: return
        if (p.isPlaying) p.pause() else p.play()
    }

    fun next() {
        currentPlayer()?.seekToNext()
    }

    fun prev() {
        currentPlayer()?.seekToPrevious()
    }

    fun like() {
        val id = currentPlayer()?.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        viewModelScope.launch {
            runCatching { api.like(id, true) }
            _state.value = _state.value.copy(liked = true)
        }
    }
}
