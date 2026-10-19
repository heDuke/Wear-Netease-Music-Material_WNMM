package com.refresh.netmusic.player

import androidx.media3.common.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NowPlaying(
    val id: Long,
    val title: String,
    val artist: String,
    val coverUrl: String,
    val playing: Boolean,
)

object PlayerHolder {
    @Volatile
    var player: Player? = null

    private val _nowPlaying = MutableStateFlow<NowPlaying?>(null)
    val nowPlaying: StateFlow<NowPlaying?> = _nowPlaying.asStateFlow()

    fun updateFrom(player: Player?) {
        if (player == null) {
            _nowPlaying.value = null
            return
        }
        val meta = player.mediaMetadata
        val id = player.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        _nowPlaying.value = NowPlaying(
            id = id,
            title = meta.title?.toString().orEmpty(),
            artist = meta.artist?.toString().orEmpty(),
            coverUrl = meta.artworkUri?.toString().orEmpty(),
            playing = player.isPlaying,
        )
    }
}
