package com.refresh.netmusic.ncm.api

import com.refresh.netmusic.ncm.model.AccountProfile
import com.refresh.netmusic.ncm.model.Comment
import com.refresh.netmusic.ncm.model.LyricLine
import com.refresh.netmusic.ncm.model.Lyrics
import com.refresh.netmusic.ncm.model.Playlist
import com.refresh.netmusic.ncm.model.QrCheck
import com.refresh.netmusic.ncm.model.QrCreate
import com.refresh.netmusic.ncm.model.QrKey
import com.refresh.netmusic.ncm.model.Song
import com.refresh.netmusic.ncm.model.SongUrl
import com.refresh.netmusic.ncm.request.CryptoMode
import com.refresh.netmusic.ncm.request.NeteaseRequest
import com.refresh.netmusic.ncm.request.NeteaseResponse
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Kotlin port of api-enhanced module JS files used by Wear v1.
 * Function names follow the js file names in camelCase.
 */
class NeteaseApi(val request: NeteaseRequest = NeteaseRequest()) {

    suspend fun loginQrKey(): QrKey {
        val res = request.call("/api/login/qrcode/unikey", mapOf("type" to 3), CryptoMode.EAPI)
        val unikey = res.body.obj("data")?.str("unikey")
            ?: res.body.str("unikey")
            ?: error("qr key missing: ${res.body}")
        return QrKey(unikey)
    }

    suspend fun loginQrCreate(key: String): QrCreate {
        return QrCreate("https://music.163.com/login?codekey=$key")
    }

    suspend fun loginQrCheck(key: String): QrCheck {
        val res = request.call(
            "/api/login/qrcode/client/login",
            mapOf("key" to key, "type" to 3),
            CryptoMode.EAPI,
        )
        val code = res.body.int("code") ?: 0
        val cookie = if (code == 803) request.cookies.asHeader() else null
        return QrCheck(code, res.body.str("message") ?: "", cookie)
    }

    suspend fun loginStatus(): AccountProfile? {
        val res = request.call("/api/w/nuser/account/get", crypto = CryptoMode.WEAPI)
        val profile = res.body.obj("profile") ?: res.body.obj("data")?.obj("profile")
        val userId = profile?.long("userId") ?: return null
        return AccountProfile(
            userId = userId,
            nickname = profile.str("nickname") ?: "",
            avatarUrl = profile.str("avatarUrl") ?: "",
        )
    }

    suspend fun loginRefresh() {
        request.call("/api/login/token/refresh", crypto = CryptoMode.EAPI)
    }

    suspend fun logout() {
        request.call("/api/logout", crypto = CryptoMode.EAPI)
        request.cookies.clear()
    }

    suspend fun recommendSongs(): List<Song> {
        val res = request.call("/api/v3/discovery/recommend/songs", crypto = CryptoMode.WEAPI)
        val list = res.body.obj("data")?.arr("dailySongs") ?: res.body.arr("recommend") ?: return emptyList()
        return list.mapNotNull { it.asSong() }
    }

    suspend fun recommendResource(): List<Playlist> {
        val res = request.call("/api/v1/discovery/recommend/resource", crypto = CryptoMode.WEAPI)
        val list = res.body.arr("recommend") ?: return emptyList()
        return list.mapNotNull { it.asPlaylist() }
    }

    suspend fun personalized(limit: Int = 30): List<Playlist> {
        val res = request.call(
            "/api/personalized/playlist",
            mapOf("limit" to limit, "total" to true, "n" to 1000),
            CryptoMode.WEAPI,
        )
        val list = res.body.arr("result") ?: return emptyList()
        return list.mapNotNull { it.asPlaylist() }
    }

    suspend fun userPlaylist(uid: Long, limit: Int = 50, offset: Int = 0): List<Playlist> {
        val res = request.call(
            "/api/user/playlist",
            mapOf("uid" to uid, "limit" to limit, "offset" to offset, "includeVideo" to true),
            CryptoMode.WEAPI,
        )
        val list = res.body.arr("playlist") ?: return emptyList()
        return list.mapNotNull { it.asPlaylist() }
    }

    suspend fun playlistDetail(id: Long): Pair<Playlist?, List<Song>> {
        val res = request.call(
            "/api/v6/playlist/detail",
            mapOf("id" to id, "n" to 100000, "s" to 8),
            CryptoMode.EAPI,
        )
        val pl = res.body.obj("playlist")
        val playlist = pl?.asPlaylist()
        val tracks = pl?.arr("tracks")?.mapNotNull { it.asSong() }.orEmpty()
        return playlist to tracks
    }

    suspend fun playlistTrackAll(id: Long, limit: Int = 200, offset: Int = 0): List<Song> {
        val res = request.call(
            "/api/v6/playlist/detail",
            mapOf("id" to id, "n" to 100000, "s" to 8),
            CryptoMode.EAPI,
        )
        val ids = res.body.obj("playlist")?.arr("trackIds")
            ?.mapNotNull { it.jsonObject.long("id") }
            ?.drop(offset)
            ?.take(limit)
            .orEmpty()
        if (ids.isEmpty()) return emptyList()
        return songDetail(ids)
    }

    suspend fun cloudsearch(keywords: String, type: Int = 1, limit: Int = 30, offset: Int = 0): List<Song> {
        val res = request.call(
            "/api/cloudsearch/pc",
            mapOf(
                "s" to keywords,
                "type" to type,
                "limit" to limit,
                "offset" to offset,
                "total" to true,
            ),
            CryptoMode.EAPI,
        )
        val songs = res.body.obj("result")?.arr("songs") ?: return emptyList()
        return songs.mapNotNull { it.asSong() }
    }

    suspend fun searchHot(): List<String> {
        val res = request.call("/api/search/hot", mapOf("type" to 1111), CryptoMode.EAPI)
        val list = res.body.obj("result")?.arr("hots") ?: return emptyList()
        return list.mapNotNull { it.jsonObject.str("first") }
    }

    suspend fun songDetail(ids: List<Long>): List<Song> {
        if (ids.isEmpty()) return emptyList()
        val c = ids.joinToString(",", prefix = "[", postfix = "]") { "{\"id\":$it}" }
        val res = request.call("/api/v3/song/detail", mapOf("c" to c), CryptoMode.WEAPI)
        val songs = res.body.arr("songs") ?: return emptyList()
        return songs.mapNotNull { it.asSong() }
    }

    suspend fun songUrlV1(id: Long, level: String = "higher"): SongUrl {
        val v1 = request.call(
            "/api/song/enhance/player/url/v1",
            mapOf("ids" to "[$id]", "level" to level, "encodeType" to "flac"),
            CryptoMode.EAPI,
        )
        var item = v1.body.arr("data")?.firstOrNull()?.jsonObject
        var url = item?.str("url")?.takeIf { it.isNotBlank() }
        if (url == null) {
            val br = when (level) {
                "standard" -> 128000
                "exhigh" -> 999000
                else -> 320000
            }
            val legacy = request.call(
                "/api/song/enhance/player/url",
                mapOf("ids" to "[$id]", "br" to br),
                CryptoMode.WEAPI,
            )
            item = legacy.body.arr("data")?.firstOrNull()?.jsonObject
            url = item?.str("url")?.takeIf { it.isNotBlank() }
        }
        return SongUrl(
            id = item?.long("id") ?: id,
            url = url,
            level = item?.str("level") ?: level,
            trial = item?.obj("freeTrialInfo") != null,
        )
    }

    suspend fun lyricNew(id: Long): Lyrics {
        val res = request.call(
            "/api/song/lyric/v1",
            mapOf(
                "id" to id,
                "cp" to false,
                "tv" to 0,
                "lv" to 0,
                "rv" to 0,
                "kv" to 0,
                "yv" to 0,
                "ytv" to 0,
                "yrv" to 0,
            ),
            CryptoMode.EAPI,
        )
        val lrc = res.body.obj("lrc")?.str("lyric").orEmpty()
        return Lyrics(parseLrc(lrc), lrc)
    }

    suspend fun like(id: Long, like: Boolean = true) {
        request.call(
            "/api/radio/like",
            mapOf("alg" to "itembased", "trackId" to id, "like" to like, "time" to "3"),
            CryptoMode.WEAPI,
        )
    }

    suspend fun likelist(uid: Long): Set<Long> {
        val res = request.call("/api/song/like/get", mapOf("uid" to uid), CryptoMode.EAPI)
        return res.body.arr("ids")?.mapNotNull { it.jsonPrimitive.longOrNull }?.toSet().orEmpty()
    }

    suspend fun scrobble(id: Long, sourceId: Long, timeSec: Int) {
        val logs = """[{"action":"play","json":{"download":0,"end":"playend","id":$id,"sourceId":$sourceId,"time":$timeSec,"type":"song","wifi":0,"source":"list"}}]"""
        request.call("/api/feedback/weblog", mapOf("logs" to logs), CryptoMode.WEAPI)
    }

    suspend fun commentMusic(id: Long, limit: Int = 20, offset: Int = 0): List<Comment> {
        val res = request.call(
            "/api/v1/resource/comments/R_SO_4_$id",
            mapOf("rid" to id, "limit" to limit, "offset" to offset, "beforeTime" to 0),
            CryptoMode.WEAPI,
        )
        val hot = res.body.arr("hotComments").orEmpty()
        val comments = res.body.arr("comments").orEmpty()
        return (hot + comments).mapNotNull { it.asComment() }
    }

    fun close() = request.close()

    private fun JsonElement.asSong(): Song? {
        val o = runCatching { jsonObject }.getOrNull() ?: return null
        val id = o.long("id") ?: return null
        val artists = o.arr("ar") ?: o.arr("artists")
        val artistNames = artists?.mapNotNull { it.jsonObject.str("name") }?.joinToString(" / ").orEmpty()
        val al = o.obj("al") ?: o.obj("album")
        return Song(
            id = id,
            name = o.str("name") ?: "",
            artists = artistNames,
            album = al?.str("name") ?: "",
            albumId = al?.long("id") ?: 0,
            coverUrl = al?.str("picUrl") ?: o.str("picUrl") ?: "",
            durationMs = o.long("dt") ?: o.long("duration") ?: 0,
            fee = o.int("fee") ?: 0,
        )
    }

    private fun JsonElement.asPlaylist(): Playlist? {
        val o = runCatching { jsonObject }.getOrNull() ?: return null
        val id = o.long("id") ?: return null
        return Playlist(
            id = id,
            name = o.str("name") ?: "",
            coverUrl = o.str("picUrl") ?: o.str("coverImgUrl") ?: "",
            trackCount = o.int("trackCount") ?: 0,
            creator = o.obj("creator")?.str("nickname") ?: "",
            playCount = o.long("playCount") ?: 0,
        )
    }

    private fun JsonElement.asComment(): Comment? {
        val o = runCatching { jsonObject }.getOrNull() ?: return null
        val user = o.obj("user")
        return Comment(
            id = o.long("commentId") ?: return null,
            nickname = user?.str("nickname") ?: "",
            avatarUrl = user?.str("avatarUrl") ?: "",
            content = o.str("content") ?: "",
            likedCount = o.int("likedCount") ?: 0,
            timeMs = o.long("time") ?: 0,
        )
    }

    private fun parseLrc(raw: String): List<LyricLine> {
        val regex = Regex("""\[(\d{2}):(\d{2})(?:\.(\d{2,3}))?\](.*)""")
        return raw.lineSequence().mapNotNull { line ->
            val m = regex.find(line.trim()) ?: return@mapNotNull null
            val min = m.groupValues[1].toLong()
            val sec = m.groupValues[2].toLong()
            val msPart = m.groupValues[3]
            val ms = when (msPart.length) {
                2 -> msPart.toLong() * 10
                3 -> msPart.toLong()
                else -> 0
            }
            LyricLine(min * 60_000 + sec * 1000 + ms, m.groupValues[4].trim())
        }.filter { it.text.isNotBlank() }.toList()
    }
}

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
private fun JsonObject.arr(key: String): List<JsonElement>? = (this[key] as? JsonArray)?.toList()
private fun JsonObject.str(key: String): String? = this[key]?.jsonPrimitive?.contentOrNull
private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.intOrNull
private fun JsonObject.long(key: String): Long? = this[key]?.jsonPrimitive?.longOrNull
private fun JsonObject.bool(key: String): Boolean? = this[key]?.jsonPrimitive?.booleanOrNull
