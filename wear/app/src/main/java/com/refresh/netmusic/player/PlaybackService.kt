package com.refresh.netmusic.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.refresh.netmusic.MainActivity
import com.refresh.netmusic.R
import com.refresh.netmusic.data.SessionStore
import com.refresh.netmusic.ncm.api.NeteaseApi
import com.refresh.netmusic.ncm.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import java.io.IOException

class PlaybackService : MediaSessionService() {
    private val api: NeteaseApi by inject()
    private val sessionStore: SessionStore by inject()
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
        val resolving = ResolvingDataSource.Factory(DefaultHttpDataSource.Factory()) { dataSpec ->
            val uri = dataSpec.uri
            if (uri.scheme != "ncm") return@Factory dataSpec
            val id = uri.host?.toLongOrNull() ?: throw IOException("bad ncm uri")
            val url = runBlocking(Dispatchers.IO) {
                api.songUrlV1(id, sessionStore.quality()).url
            } ?: throw IOException("empty url")
            dataSpec.buildUpon().setUri(Uri.parse(url)).build()
        }
        val exo = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(this).setDataSourceFactory(resolving))
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
        exo.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                PlayerHolder.updateFrom(player)
                applyOngoing(player)
            }
        })
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
                        .setArtworkUri(song.coverUrl.takeIf { it.isNotBlank() }?.let(Uri::parse))
                        .build(),
                )
                .build()
        }
        exo.setMediaItems(items, startIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0)), 0)
        exo.prepare()
        exo.play()
        PlayerHolder.updateFrom(exo)
    }

    private fun applyOngoing(player: Player) {
        val title = player.mediaMetadata.title?.toString() ?: return
        val artist = player.mediaMetadata.artist?.toString().orEmpty()
        if (!player.isPlaying && player.playbackState == Player.STATE_IDLE) return
        val launch = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_music)
            .setContentTitle(title)
            .setContentText(artist)
            .setContentIntent(launch)
            .setOngoing(player.isPlaying)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        OngoingActivity.Builder(applicationContext, ONGOING_ID, notification)
            .setAnimatedIcon(R.drawable.ic_stat_music)
            .setStaticIcon(R.drawable.ic_stat_music)
            .setTouchIntent(launch)
            .setTitle(title)
            .setStatus(Status.Builder().addTemplate(artist.ifBlank { "NetMusic" }).build())
            .build()
            .apply(applicationContext)
    }

    override fun onDestroy() {
        instance = null
        PlayerHolder.player = null
        PlayerHolder.updateFrom(null)
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    companion object {
        const val CHANNEL = "netmusic_playback"
        const val ONGOING_ID = 1001
        var instance: PlaybackService? = null
            private set
    }
}
