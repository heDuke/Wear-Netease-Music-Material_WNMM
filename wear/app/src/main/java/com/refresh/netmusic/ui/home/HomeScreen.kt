package com.refresh.netmusic.ui.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.transformedHeight
import com.refresh.netmusic.ncm.model.Playlist
import com.refresh.netmusic.ncm.model.Song
import com.refresh.netmusic.player.PlayerHolder
import com.refresh.netmusic.ui.components.LoadingItem
import com.refresh.netmusic.ui.components.MessageItem
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
    WearListScreen(
        title = "推荐",
        isRefreshing = state.loading,
        onRefresh = vm::refresh,
        edgeButton = nowPlaying?.let { np ->
            {
                EdgeButton(onClick = onNowPlaying) {
                    Text(np.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
    ) { spec ->
        item {
            Button(
                onClick = onSearch,
                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec),
                icon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                label = { Text("搜索") },
            )
        }
        item {
            Button(
                onClick = onLibrary,
                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec),
                icon = { Icon(Icons.Outlined.LibraryMusic, contentDescription = null) },
                label = { Text("我的") },
            )
        }
        if (state.loading && state.daily.isEmpty()) {
            item { LoadingItem() }
        }
        if (state.error != null) {
            item { MessageItem(state.error.orEmpty()) }
        }
        if (state.daily.isNotEmpty()) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) { Text("每日推荐") }
            }
            state.daily.take(10).forEachIndexed { index, song ->
                item(key = "daily-${song.id}") {
                    SongRow(
                        title = song.name,
                        subtitle = song.artists,
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
                        subtitle = "${pl.trackCount} 首",
                        coverUrl = pl.coverUrl,
                        transformation = SurfaceTransformation(spec),
                        modifier = Modifier.transformedHeight(this, spec),
                    ) { onPlaylist(pl) }
                }
            }
        }
    }
}
