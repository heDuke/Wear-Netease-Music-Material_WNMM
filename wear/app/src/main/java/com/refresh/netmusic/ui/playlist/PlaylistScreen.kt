package com.refresh.netmusic.ui.playlist

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.refresh.netmusic.ncm.model.Song
import com.refresh.netmusic.ui.components.SongRow
import com.refresh.netmusic.ui.components.WearListScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlaylistScreen(
    id: Long,
    name: String,
    onPlay: (List<Song>, Int) -> Unit,
    vm: PlaylistViewModel = koinViewModel(),
) {
    LaunchedEffect(id) { vm.load(id) }
    val songs by vm.songs.collectAsStateWithLifecycle()
    WearListScreen(title = name) {
        songs.forEachIndexed { index, song ->
            item { SongRow(song.name, song.artists) { onPlay(songs, index) } }
        }
    }
}
