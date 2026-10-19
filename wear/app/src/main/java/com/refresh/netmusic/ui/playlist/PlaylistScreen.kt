package com.refresh.netmusic.ui.playlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.transformedHeight
import com.refresh.netmusic.ncm.model.Song
import com.refresh.netmusic.player.PlayerHolder
import com.refresh.netmusic.ui.components.LoadingItem
import com.refresh.netmusic.ui.components.MessageItem
import com.refresh.netmusic.ui.components.SongRow
import com.refresh.netmusic.ui.components.WearListScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlaylistScreen(
    id: Long,
    name: String,
    onPlay: (List<Song>, Int) -> Unit,
    onNowPlaying: () -> Unit,
    vm: PlaylistViewModel = koinViewModel(),
) {
    LaunchedEffect(id) { vm.load(id) }
    val state by vm.state.collectAsStateWithLifecycle()
    val nowPlaying by PlayerHolder.nowPlaying.collectAsStateWithLifecycle()
    WearListScreen(
        title = name,
        isRefreshing = state.loading,
        onRefresh = { vm.load(id) },
        edgeButton = nowPlaying?.let { np ->
            {
                EdgeButton(onClick = onNowPlaying) {
                    Text(np.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        },
    ) { spec ->
        if (state.loading && state.songs.isEmpty()) {
            item { LoadingItem() }
        }
        if (state.error != null) {
            item { MessageItem(state.error.orEmpty()) }
        }
        if (!state.loading && state.songs.isEmpty() && state.error == null) {
            item { MessageItem("空歌单") }
        }
        state.songs.forEachIndexed { index, song ->
            item(key = song.id) {
                SongRow(
                    title = song.name,
                    subtitle = song.artists,
                    coverUrl = song.coverUrl,
                    transformation = SurfaceTransformation(spec),
                    modifier = Modifier.transformedHeight(this, spec),
                ) { onPlay(state.songs, index) }
            }
        }
    }
}
