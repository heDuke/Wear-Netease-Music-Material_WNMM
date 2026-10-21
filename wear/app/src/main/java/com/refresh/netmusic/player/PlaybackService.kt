package com.refresh.netmusic.player

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.wear.ongoing.OngoingActivity
import androidx.wear.ongoing.Status
import com.refresh.netmusic.MainActivity
import com.refresh.netmusic.R
import com.refresh.netmusic.data.SessionStore
import com.refresh.netmusic.ncm.api.NeteaseApi
import com.refresh.netmusic.ncm.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject
import java.io.IOException

class PlaybackService : MediaSessionService() {
    private val api: NeteaseApi by inject()
    private val sessionStore: SessionStore by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null
    private var errorRetries = 0

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java)
                .createNotificationChannel(
                    NotificationChannel(CHANNEL, "NetMusic", NotificationManager.IMPORTANCE_LOW),
                )
        }
        val notificationProvider = DefaultMediaNotificationProvider.Builder(this)
            .setChannelId(CHANNEL)
            .setNotificationId(ONGOING_ID)
            .build()
        notificationProvider.setSmallIcon(R.drawable.ic_stat_music)
        setMediaNotificationProvider(notificationProvider)
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(MEDIA_UA)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(20_000)
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(
                mapOf(
                    "Referer" to "https://music.163.com/",
                    "Origin" to "https://music.163.com",
                ),
            )
        val resolving = ResolvingDataSource.Factory(http) { dataSpec ->
            val uri = dataSpec.uri
            if (uri.scheme != "ncm") return@Factory dataSpec
            val id = uri.host?.toLongOrNull() ?: throw IOException("bad ncm uri")
            val resolved = runBlocking(Dispatchers.IO) {
                api.songUrlV1(id, sessionStore.quality())
            }
            val raw = resolved.url?.takeIf { it.isNotBlank() }
                ?: throw IOException(resolved.message ?: if (resolved.trial) "仅试听，无法完整播放" else "无法获取播放地址")
            val cookie = sessionStore.loadCookies().entries.joinToString("; ") { "${it.key}=${it.value}" }
            val headers = buildMap {
                put("Referer", "https://music.163.com/")
                put("Origin", "https://music.163.com")
                if (cookie.isNotBlank()) put("Cookie", cookie)
            }
            val builder = dataSpec.buildUpon()
                .setUri(Uri.parse(raw))
                .setHttpRequestHeaders(headers)
            val mime = mimeTypeOf(resolved.type)
            if (mime != null) {
                builder.setMimeType(mime)
            }
            builder.build()
        }
        val renderers = DefaultRenderersFactory(this).setEnableDecoderFallback(true)
        val exo = ExoPlayer.Builder(this, renderers)
            .setMediaSourceFactory(
                DefaultMediaSourceFactory(this)
                    .setDataSourceFactory(resolving)
                    .setLoadErrorHandlingPolicy(object : DefaultLoadErrorHandlingPolicy() {
                        override fun getRetryDelayMsFor(
                            loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo,
                        ): Long {
                            val httpError = loadErrorInfo.exception as? HttpDataSource.InvalidResponseCodeException
                                ?: loadErrorInfo.exception.cause as? HttpDataSource.InvalidResponseCodeException
                            if (httpError != null && httpError.responseCode in setOf(403, 404, 410)) return 400
                            if (loadErrorInfo.exception is IOException) return 700
                            return super.getRetryDelayMsFor(loadErrorInfo)
                        }

                        override fun getMinimumLoadableRetryCount(dataType: Int): Int = 4
                    }),
            )
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        exo.skipSilenceEnabled = false
        exo.trackSelectionParameters = exo.trackSelectionParameters.buildUpon()
            .setAudioOffloadPreferences(
                TrackSelectionParameters.AudioOffloadPreferences.Builder()
                    .setAudioOffloadMode(
                        TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED,
                    )
                    .build(),
            )
            .build()
        player = exo
        PlayerHolder.player = exo
        instance = this
        exo.addListener(object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                PlayerHolder.updateFrom(player)
                applyOngoing(player)
            }

            override fun onPlayerError(error: PlaybackException) {
                val exoPlayer = player ?: return
                val retryable = error.errorCode in RETRYABLE_ERRORS && errorRetries < 2
                if (retryable) {
                    errorRetries++
                    PlayerHolder.setError("正在重试播放")
                    exoPlayer.seekToDefaultPosition()
                    exoPlayer.prepare()
                    exoPlayer.play()
                    return
                }
                PlayerHolder.setError(friendlyError(error))
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    errorRetries = 0
                    PlayerHolder.setError(null)
                }
            }
        })
        mediaSession = MediaSession.Builder(this, exo).build()
        flushPending()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        flushPending()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    fun playSongs(songs: List<Song>, startIndex: Int) {
        val exo = player ?: return
        if (songs.isEmpty()) return
        errorRetries = 0
        val items = songs.map { toMediaItem(it) }
        PlayerHolder.setError(null)
        exo.setMediaItems(items, startIndex.coerceIn(0, items.lastIndex), 0L)
        exo.prepare()
        exo.playWhenReady = true
        exo.play()
        PlayerHolder.updateFrom(exo)
        fillMissingMetadata(songs)
    }

    private fun fillMissingMetadata(songs: List<Song>) {
        val missing = songs.filter { it.artists.isBlank() || it.coverUrl.isBlank() || it.name.isBlank() }
            .map { it.id }
        if (missing.isEmpty()) return
        scope.launch(Dispatchers.IO) {
            val details = runCatching { api.songDetail(missing) }.getOrDefault(emptyList())
            if (details.isEmpty()) return@launch
            launch(Dispatchers.Main) {
                val exo = player ?: return@launch
                details.forEach { song ->
                    val index = (0 until exo.mediaItemCount).firstOrNull { i ->
                        exo.getMediaItemAt(i).mediaId == song.id.toString()
                    } ?: return@forEach
                    exo.replaceMediaItem(index, toMediaItem(song))
                }
                PlayerHolder.updateFrom(exo)
            }
        }
    }

    private fun toMediaItem(song: Song): MediaItem {
        val title = song.name.ifBlank { "歌曲 ${song.id}" }
        val artist = song.artists.ifBlank { song.album }
        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri("ncm://${song.id}")
            .setMediaMetadata(
                androidx.media3.common.MediaMetadata.Builder()
                    .setTitle(title)
                    .setArtist(artist)
                    .setAlbumTitle(song.album.takeIf { it.isNotBlank() })
                    .setArtworkUri(song.coverUrl.takeIf { it.isNotBlank() }?.let(Uri::parse))
                    .build(),
            )
            .build()
    }

    private fun flushPending() {
        val pending = takePending() ?: return
        playSongs(pending.first, pending.second)
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
        scope.cancel()
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
        private const val MEDIA_UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel Watch) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
        private val RETRYABLE_ERRORS = setOf(
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_DECODING_FAILED,
            PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        )

        private fun mimeTypeOf(type: String?): String? = when (type?.lowercase()) {
            "mp3", "mpeg" -> MimeTypes.AUDIO_MPEG
            "aac" -> MimeTypes.AUDIO_AAC
            "m4a", "mp4" -> MimeTypes.AUDIO_MP4
            "ogg", "oga" -> MimeTypes.AUDIO_OGG
            "wav" -> MimeTypes.AUDIO_WAV
            "flac" -> MimeTypes.AUDIO_FLAC
            else -> null
        }

        private fun friendlyError(error: PlaybackException): String {
            val cause = error.cause?.message.orEmpty()
            return when (error.errorCode) {
                PlaybackException.ERROR_CODE_DECODING_FAILED,
                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
                -> "无法解码该音频"
                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                -> "网络或播放地址失效"
                else -> cause.ifBlank { error.message ?: "无法播放" }
            }
        }
        var instance: PlaybackService? = null
            private set

        @Volatile
        private var pending: Pair<List<Song>, Int>? = null

        @Synchronized
        private fun takePending(): Pair<List<Song>, Int>? {
            val value = pending
            pending = null
            return value
        }

        fun play(context: Context, songs: List<Song>, index: Int) {
            if (songs.isEmpty()) return
            pending = songs to index
            instance?.flushPending()
            context.startService(Intent(context, PlaybackService::class.java))
        }
    }
}
