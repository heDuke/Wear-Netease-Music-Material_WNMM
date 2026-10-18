package com.refresh.netmusic.ui.library

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import com.refresh.netmusic.ncm.model.Playlist
import com.refresh.netmusic.ui.components.SongRow
import com.refresh.netmusic.ui.components.WearListScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryScreen(
    onPlaylist: (Playlist) -> Unit,
    onSettings: () -> Unit,
    vm: LibraryViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    WearListScreen(title = state.profile?.nickname ?: "我的") {
        item {
            Button(onClick = onSettings, modifier = Modifier.fillMaxWidth(), label = { Text("设置") })
        }
        state.playlists.forEach { pl ->
            item { SongRow(pl.name, "${pl.trackCount} 首") { onPlaylist(pl) } }
        }
    }
}
