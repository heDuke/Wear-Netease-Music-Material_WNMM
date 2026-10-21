package com.refresh.netmusic.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.refresh.netmusic.data.SessionStore
import com.refresh.netmusic.ncm.api.NeteaseApi
import com.refresh.netmusic.ncm.model.Comment
import com.refresh.netmusic.ncm.model.Lyrics
import com.refresh.netmusic.player.PlayerHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QueueItem(
    val id: Long,
    val title: String,
    val artist: String,
    val coverUrl: String,
)

data class PlayerUi(
    val songId: Long = 0,
    val title: String = "",
    val artist: String = "",
    val coverUrl: String = "",
    val playing: Boolean = false,
    val position: Long = 0,
    val duration: Long = 0,
    val lyrics: Lyrics = Lyrics(emptyList(), ""),
    val comments: List<Comment> = emptyList(),
    val liked: Boolean = false,
    val queue: List<QueueItem> = emptyList(),
    val queueIndex: Int = 0,
    val error: String? = null,
    val buffering: Boolean = false,
)

class PlayerViewModel(
    private val api: NeteaseApi,
    private val session: SessionStore,
) : ViewModel() {
    private val _state = MutableStateFlow(PlayerUi())
    val state = _state.asStateFlow()
    private var lastId: Long = -1
    private var scrobbledId: Long = -1

    init {
        viewModelScope.launch {
            while (true) {
                val exo = PlayerHolder.player
                if (exo != null) {
                    val meta = exo.mediaMetadata
                    val id = exo.currentMediaItem?.mediaId?.toLongOrNull() ?: 0L
                    val queue = buildList {
                        for (i in 0 until exo.mediaItemCount) {
                            val item = exo.getMediaItemAt(i)
                            val m = item.mediaMetadata
                            add(
                                QueueItem(
                                    id = item.mediaId.toLongOrNull() ?: 0L,
                                    title = m.title?.toString()?.ifBlank { null } ?: "歌曲 ${item.mediaId}",
                                    artist = m.artist?.toString()?.ifBlank { null }
                                        ?: m.albumTitle?.toString().orEmpty(),
                                    coverUrl = m.artworkUri?.toString().orEmpty(),
                                ),
                            )
                        }
                    }
                    val keep = _state.value.takeIf { it.songId == id }
                    _state.value = _state.value.copy(
                        songId = id,
                        title = meta.title?.toString()?.ifBlank { null }
                            ?: keep?.title?.ifBlank { null }
                            ?: if (id == 0L) "" else "歌曲 $id",
                        artist = meta.artist?.toString()?.ifBlank { null } ?: keep?.artist.orEmpty(),
                        coverUrl = meta.artworkUri?.toString()?.ifBlank { null } ?: keep?.coverUrl.orEmpty(),
                        playing = exo.isPlaying,
                        position = exo.currentPosition.coerceAtLeast(0),
                        duration = if (exo.duration > 0) exo.duration else keep?.duration ?: 0,
                        queue = queue,
                        queueIndex = exo.currentMediaItemIndex.coerceAtLeast(0),
                        error = PlayerHolder.nowPlaying.value?.error,
                        buffering = exo.playbackState == Player.STATE_BUFFERING,
                    )
                    if (id != 0L && id != lastId) {
                        lastId = id
                        loadExtras(id)
                    }
                    if (id != 0L && id != scrobbledId && exo.currentPosition >= 20_000) {
                        scrobbledId = id
                        runCatching { api.scrobble(id, 0, (exo.currentPosition / 1000).toInt()) }
                    }
                }
                delay(400)
            }
        }
    }

    private fun currentPlayer(): Player? = PlayerHolder.player

    private fun loadExtras(id: Long) {
        viewModelScope.launch {
            val lyrics = runCatching { api.lyricNew(id) }.getOrDefault(Lyrics(emptyList(), ""))
            val comments = runCatching { api.commentMusic(id) }.getOrDefault(emptyList())
            val liked = runCatching {
                val uid = session.uid()
                if (uid == 0L) false else api.likelist(uid).contains(id)
            }.getOrDefault(false)
            val detail = runCatching { api.songDetail(listOf(id)).firstOrNull() }.getOrNull()
            val current = _state.value
            if (current.songId != id) return@launch
            _state.value = current.copy(
                lyrics = lyrics,
                comments = comments,
                liked = liked,
                title = current.title.ifBlank { detail?.name.orEmpty() },
                artist = current.artist.ifBlank { detail?.artists.orEmpty() },
                coverUrl = current.coverUrl.ifBlank { detail?.coverUrl.orEmpty() },
            )
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

    fun playIndex(index: Int) {
        val p = currentPlayer() ?: return
        if (index in 0 until p.mediaItemCount) {
            p.seekTo(index, 0)
            p.play()
        }
    }

    fun like() {
        val id = currentPlayer()?.currentMediaItem?.mediaId?.toLongOrNull() ?: return
        val next = !_state.value.liked
        viewModelScope.launch {
            runCatching { api.like(id, next) }
            _state.value = _state.value.copy(liked = next)
        }
    }
}
