package com.refresh.netmusic.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.refresh.netmusic.data.SessionStore
import com.refresh.netmusic.ncm.api.NeteaseApi
import com.refresh.netmusic.ncm.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class PlaybackService : MediaSessionService() {
    private val api: NeteaseApi by inject()
    private val sessionStore: SessionStore by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(
                    NotificationChannel(CHANNEL, "NetMusic", NotificationManager.IMPORTANCE_LOW),
                )
        }
        val exo = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .build()
        player = exo
        PlayerHolder.player = exo
        instance = this
        mediaSession = MediaSession.Builder(this, exo).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    fun playSongs(songs: List<Song>, startIndex: Int) {
        val exo = player ?: return
        val items = songs.map { song ->
            MediaItem.Builder()
                .setMediaId(song.id.toString())
                .setUri("ncm://${song.id}")
                .setMediaMetadata(
                    androidx.media3.common.MediaMetadata.Builder()
                        .setTitle(song.name)
                        .setArtist(song.artists)
                        .setArtworkUri(android.net.Uri.parse(song.coverUrl.ifBlank { "https://p2.music.126.net" }))
                        .build(),
                )
                .build()
        }
        scope.launch {
            val start = songs.getOrNull(startIndex) ?: return@launch
            val url = api.songUrlV1(start.id, sessionStore.quality()).url ?: return@launch
            val realItems = items.mapIndexed { index, item ->
                if (index == startIndex) item.buildUpon().setUri(url).build() else item
            }
            exo.setMediaItems(realItems, startIndex, 0)
            exo.prepare()
            exo.play()
            exo.addListener(object : Player.Listener {
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    val id = mediaItem?.mediaId?.toLongOrNull() ?: return
                    scope.launch { resolveIfNeeded(id) }
                }
            })
        }
    }

    private suspend fun resolveIfNeeded(id: Long) {
        val exo = player ?: return
        val current = exo.currentMediaItem ?: return
        val uri = current.localConfiguration?.uri?.toString().orEmpty()
        if (uri.startsWith("http")) return
        val url = api.songUrlV1(id, sessionStore.quality()).url ?: return
        val idx = exo.currentMediaItemIndex
        exo.replaceMediaItem(idx, current.buildUpon().setUri(url).build())
        if (!exo.isPlaying) exo.play()
    }

    override fun onDestroy() {
        instance = null
        PlayerHolder.player = null
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL = "netmusic_playback"
        var instance: PlaybackService? = null
            private set
    }
}
