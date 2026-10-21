package com.refresh.netmusic.ui.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.transformedHeight
import com.refresh.netmusic.ncm.model.Playlist
import com.refresh.netmusic.ncm.model.Song
import com.refresh.netmusic.player.PlayerHolder
import com.refresh.netmusic.ui.components.LoadingItem
import com.refresh.netmusic.ui.components.MessageItem
import com.refresh.netmusic.ui.components.NowPlayingCard
import com.refresh.netmusic.ui.components.PlaylistCard
import com.refresh.netmusic.ui.components.SongRow
import com.refresh.netmusic.ui.components.WearListScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onSong: (List<Song>, Int) -> Unit,
    onPlaylist: (Playlist) -> Unit,
    onSearch: () -> Unit,
    onLibrary: () -> Unit,
    onNowPlaying: () -> Unit,
    vm: HomeViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nowPlaying by PlayerHolder.nowPlaying.collectAsStateWithLifecycle()
    WearListScreen(title = "推荐") { spec ->
        nowPlaying?.let { np ->
            item(key = "now-playing") {
                NowPlayingCard(
                    nowPlaying = np,
                    transformation = SurfaceTransformation(spec),
                    modifier = Modifier.transformedHeight(this, spec),
                    onClick = onNowPlaying,
                )
            }
        }
        item {
            ButtonGroup(modifier = Modifier.fillMaxWidth().transformedHeight(this, spec)) {
                IconButton(onClick = onSearch) {
                    Icon(Icons.Outlined.Search, contentDescription = "搜索")
                }
                IconButton(onClick = onLibrary) {
                    Icon(Icons.Outlined.LibraryMusic, contentDescription = "我的")
                }
            }
        }
        if (state.loading && state.daily.isEmpty()) {
            item { LoadingItem() }
        }
        if (state.error != null) {
            item {
                MessageItem(state.error.orEmpty(), onRetry = vm::refresh)
            }
        }
        if (state.daily.isNotEmpty()) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) { Text("每日推荐") }
            }
            state.daily.forEachIndexed { index, song ->
                item(key = "daily-${song.id}") {
                    SongRow(
                        title = song.name.ifBlank { "未知歌曲" },
                        subtitle = song.lineSubtitle.ifBlank { "未知艺人" },
                        coverUrl = song.coverUrl,
                        transformation = SurfaceTransformation(spec),
                        modifier = Modifier.transformedHeight(this, spec),
                    ) { onSong(state.daily, index) }
                }
            }
        }
        if (state.playlists.isNotEmpty()) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) { Text("推荐歌单") }
            }
            state.playlists.forEach { pl ->
                item(key = "pl-${pl.id}") {
                    PlaylistCard(
                        name = pl.name,
                        subtitle = listOfNotNull(
                            pl.trackCount.takeIf { it > 0 }?.let { "$it 首" },
                            pl.creator.takeIf { it.isNotBlank() },
                        ).joinToString(" · ").ifBlank { "歌单" },
                        coverUrl = pl.coverUrl,
                        transformation = SurfaceTransformation(spec),
                        modifier = Modifier.transformedHeight(this, spec),
                    ) { onPlaylist(pl) }
                }
            }
        }
    }
}
