package com.refresh.netmusic.ui.search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import com.refresh.netmusic.ncm.model.Song
import com.refresh.netmusic.ui.components.SongRow
import com.refresh.netmusic.ui.components.WearListScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun SearchScreen(
    onPlay: (List<Song>, Int) -> Unit,
    vm: SearchViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    WearListScreen(title = "搜索") {
        if (state.results.isEmpty()) {
            state.hots.take(12).forEach { hot ->
                item {
                    Button(
                        onClick = { vm.search(hot) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text(hot) },
                    )
                }
            }
        } else {
            state.results.forEachIndexed { index, song ->
                item { SongRow(song.name, song.artists) { onPlay(state.results, index) } }
            }
        }
    }
}
