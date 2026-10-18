package com.refresh.netmusic.ui.home

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import com.refresh.netmusic.ncm.model.Playlist
import com.refresh.netmusic.ncm.model.Song
import com.refresh.netmusic.ui.components.SongRow
import com.refresh.netmusic.ui.components.WearListScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
    onSong: (List<Song>, Int) -> Unit,
    onPlaylist: (Playlist) -> Unit,
    onSearch: () -> Unit,
    onLibrary: () -> Unit,
    vm: HomeViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    WearListScreen(title = "推荐") {
        item {
            Button(onClick = onSearch, modifier = Modifier.fillMaxWidth(), label = { Text("搜索") })
        }
        item {
            Button(onClick = onLibrary, modifier = Modifier.fillMaxWidth(), label = { Text("我的") })
        }
        if (state.error != null) {
            item { Text(state.error.orEmpty()) }
        }
        state.daily.take(10).forEachIndexed { index, song ->
            item {
                SongRow(song.name, song.artists) { onSong(state.daily, index) }
            }
        }
        state.playlists.forEach { pl ->
            item {
                SongRow(pl.name, "${pl.trackCount} 首") { onPlaylist(pl) }
            }
        }
    }
}
