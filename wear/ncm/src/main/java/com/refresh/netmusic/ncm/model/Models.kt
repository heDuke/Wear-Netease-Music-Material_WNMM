package com.refresh.netmusic.ncm.model

data class QrKey(val unikey: String)
data class QrCreate(val qrurl: String)
data class QrCheck(val code: Int, val message: String, val cookieHeader: String?)

data class AccountProfile(
    val userId: Long,
    val nickname: String,
    val avatarUrl: String,
)

data class Song(
    val id: Long,
    val name: String,
    val artists: String,
    val album: String,
    val albumId: Long,
    val coverUrl: String,
    val durationMs: Long,
    val fee: Int = 0,
)

data class Playlist(
    val id: Long,
    val name: String,
    val coverUrl: String,
    val trackCount: Int,
    val creator: String,
    val playCount: Long = 0,
)

data class LyricLine(val timeMs: Long, val text: String)
data class Lyrics(val lines: List<LyricLine>, val raw: String)

data class Comment(
    val id: Long,
    val nickname: String,
    val avatarUrl: String,
    val content: String,
    val likedCount: Int,
    val timeMs: Long,
)

data class SongUrl(val id: Long, val url: String?, val level: String?, val trial: Boolean)
