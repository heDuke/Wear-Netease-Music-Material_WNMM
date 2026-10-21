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
import com.refresh.netmusic.ncm.crypto.NeteaseCrypto
import com.refresh.netmusic.ncm.model.SongUrl
import com.refresh.netmusic.ncm.request.CryptoMode
import com.refresh.netmusic.ncm.request.NeteaseRequest
import com.refresh.netmusic.ncm.request.NeteaseResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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
        val wearLevel = when (level) {
            "standard", "exhigh" -> level
            else -> "higher"
        }
        val mp3 = fetchSongUrlV1(id, wearLevel, "mp3")
        if (mp3.isWatchSafe()) return mp3
        val aac = fetchSongUrlV1(id, wearLevel, "aac")
        if (aac.isWatchSafe()) return aac
        val br = if (wearLevel == "standard") 128000 else 320000
        val legacy = fetchSongUrlLegacy(id, br)
        return listOf(mp3, aac, legacy).firstOrNull { it.isWatchSafe() }
            ?: listOf(mp3, aac, legacy).firstOrNull { !it.url.isNullOrBlank() }
            ?: mp3.copy(message = mp3.message ?: emptyUrlMessage(mp3))
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
        val yrc = res.body.obj("yrc")?.str("lyric").orEmpty()
        val lines = parseLrc(lrc).ifEmpty { parseYrc(yrc) }
        return Lyrics(lines, lrc.ifBlank { yrc })
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

    private suspend fun fetchSongUrlV1(id: Long, level: String, encodeType: String): SongUrl {
        val res = request.call(
            "/api/song/enhance/player/url/v1",
            mapOf("ids" to "[$id]", "level" to level, "encodeType" to encodeType),
            CryptoMode.EAPI,
        )
        return parseSongUrl(res.body.arr("data")?.firstOrNull()?.jsonObject, id, level)
    }

    private suspend fun fetchSongUrlLegacy(id: Long, br: Int): SongUrl {
        val res = request.call(
            "/api/song/enhance/player/url",
            mapOf("ids" to "[\"$id\"]", "br" to br),
            CryptoMode.WEAPI,
        )
        return parseSongUrl(res.body.arr("data")?.firstOrNull()?.jsonObject, id, null)
    }

    private fun parseSongUrl(item: JsonObject?, id: Long, level: String?): SongUrl {
        val url = item?.str("url")?.takeIf { it.isNotBlank() }
        val code = item?.int("code") ?: if (url == null) 404 else 200
        return SongUrl(
            id = item?.long("id") ?: id,
            url = url,
            level = item?.str("level") ?: level,
            type = item?.str("type") ?: item?.str("encodeType"),
            trial = item?.obj("freeTrialInfo") != null,
            code = code,
            message = item?.str("exMsg") ?: item?.str("message"),
        )
    }

    private fun SongUrl.isWatchSafe(): Boolean {
        if (url.isNullOrBlank()) return false
        val t = type?.lowercase().orEmpty()
        return t !in LOSSLESS_TYPES
    }

    private fun emptyUrlMessage(url: SongUrl): String = when {
        url.trial -> "仅试听，无法完整播放"
        url.code == 404 -> "无版权或无法播放"
        else -> "无法获取播放地址"
    }

    private fun JsonElement.asSong(): Song? {
        var o = runCatching { jsonObject }.getOrNull() ?: return null
        o.obj("song")?.let { o = it }
        val id = o.long("id") ?: return null
        val artistNames = artistNames(o)
        val al = o.obj("al") ?: o.obj("album")
        val name = o.str("name").orEmpty()
        val extra = (o.arr("tns") ?: o.arr("alias") ?: o.arr("alia"))
            ?.mapNotNull { it.asPlainName() }
            ?.filter { it.isNotBlank() }
            ?.joinToString(" / ")
            .orEmpty()
        val displayName = if (extra.isNotBlank() && extra != name) "$name ($extra)" else name
        val cover = listOf(
            al?.str("picUrl"),
            o.str("picUrl"),
            al?.str("blurPicUrl"),
            o.str("albumPicUrl"),
            al?.str("pic_str")?.let(NeteaseCrypto::picUrl),
            al?.long("pic")?.takeIf { it != 0L }?.let { NeteaseCrypto.picUrl(it.toString()) },
        ).firstOrNull { !it.isNullOrBlank() }.orEmpty()
        return Song(
            id = id,
            name = displayName,
            artists = artistNames,
            album = al?.str("name").orEmpty(),
            albumId = al?.long("id") ?: 0,
            coverUrl = cover,
            durationMs = o.long("dt") ?: o.long("duration") ?: 0,
            fee = o.int("fee") ?: 0,
        )
    }

    private fun artistNames(o: JsonObject): String {
        val fromList = (o.arr("ar") ?: o.arr("artists"))
            ?.mapNotNull { it.asPlainName() }
            ?.filter { it.isNotBlank() }
            .orEmpty()
        if (fromList.isNotEmpty()) return fromList.joinToString(" / ")
        return o.str("artist").orEmpty()
    }

    private fun JsonElement.asPlainName(): String? {
        runCatching { jsonObject.str("name") }.getOrNull()?.let { return it }
        return (this as? JsonPrimitive)?.contentOrNull
    }

    private fun JsonElement.asPlaylist(): Playlist? {
        val o = runCatching { jsonObject }.getOrNull() ?: return null
        val id = o.long("id") ?: return null
        return Playlist(
            id = id,
            name = o.str("name") ?: "",
            coverUrl = listOf(
                o.str("picUrl"),
                o.str("coverImgUrl"),
                o.str("coverImgId_str")?.let(NeteaseCrypto::picUrl),
                o.long("coverImgId")?.takeIf { it != 0L }?.let { NeteaseCrypto.picUrl(it.toString()) },
            ).firstOrNull { !it.isNullOrBlank() }.orEmpty(),
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
        if (raw.isBlank()) return emptyList()
        val tag = Regex("""\[(\d{2}):(\d{2})(?:[\.:](\d{2,3}))?\]""")
        return raw.lineSequence().flatMap { line ->
            val trimmed = line.trim()
            val tags = tag.findAll(trimmed).toList()
            if (tags.isEmpty()) return@flatMap emptyList()
            val text = trimmed.substring(tags.last().range.last + 1).trim()
            if (text.isBlank()) return@flatMap emptyList()
            tags.map { m ->
                val min = m.groupValues[1].toLong()
                val sec = m.groupValues[2].toLong()
                val msPart = m.groupValues[3]
                val ms = when (msPart.length) {
                    2 -> msPart.toLong() * 10
                    3 -> msPart.toLong()
                    else -> 0
                }
                LyricLine(min * 60_000 + sec * 1000 + ms, text)
            }
        }.sortedBy { it.timeMs }.toList()
    }

    private fun parseYrc(raw: String): List<LyricLine> {
        if (raw.isBlank()) return emptyList()
        val wordTag = Regex("""\(\d+,\d+,\d+\)""")
        val timed = Regex("""\[(\d+),\d+](.*)""")
        return raw.lineSequence().mapNotNull { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("{")) {
                runCatching {
                    val o = lyricJson.parseToJsonElement(trimmed).jsonObject
                    val t = o.long("t") ?: return@runCatching null
                    val text = o.arr("c")?.joinToString("") { el ->
                        el.jsonObject.str("tx").orEmpty()
                    }.orEmpty()
                    text.takeIf { it.isNotBlank() }?.let { LyricLine(t, it) }
                }.getOrNull()
            } else {
                val m = timed.find(trimmed) ?: return@mapNotNull null
                val text = wordTag.replace(m.groupValues[2], "").trim()
                if (text.isBlank()) null else LyricLine(m.groupValues[1].toLong(), text)
            }
        }.sortedBy { it.timeMs }
    }

    companion object {
        private val LOSSLESS_TYPES = setOf("flac", "ape", "wav", "alac", "dsf", "dff")
        private val lyricJson = Json { ignoreUnknownKeys = true; isLenient = true }
    }
}

private fun JsonObject.obj(key: String): JsonObject? = this[key] as? JsonObject
private fun JsonObject.arr(key: String): List<JsonElement>? = (this[key] as? JsonArray)?.toList()
private fun JsonObject.str(key: String): String? = (this[key] as? JsonPrimitive)?.contentOrNull
private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.intOrNull
    ?: (this[key] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
private fun JsonObject.long(key: String): Long? = (this[key] as? JsonPrimitive)?.longOrNull
    ?: (this[key] as? JsonPrimitive)?.contentOrNull?.toLongOrNull()
private fun JsonObject.bool(key: String): Boolean? = (this[key] as? JsonPrimitive)?.booleanOrNull
