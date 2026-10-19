package com.refresh.netmusic.ui.search

import android.app.RemoteInput
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.transformedHeight
import androidx.wear.input.RemoteInputIntentHelper
import com.refresh.netmusic.ncm.model.Song
import com.refresh.netmusic.ui.components.LoadingItem
import com.refresh.netmusic.ui.components.MessageItem
import com.refresh.netmusic.ui.components.SongRow
import com.refresh.netmusic.ui.components.WearListScreen
import org.koin.androidx.compose.koinViewModel

private const val KEY_QUERY = "search_query"

@Composable
fun SearchScreen(
    onPlay: (List<Song>, Int) -> Unit,
    vm: SearchViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val results = RemoteInput.getResultsFromIntent(result.data) ?: return@rememberLauncherForActivityResult
        val query = results.getCharSequence(KEY_QUERY)?.toString()?.trim().orEmpty()
        if (query.isNotBlank()) vm.search(query)
    }

    fun launchInput() {
        val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
        val remoteInputs = listOf(
            RemoteInput.Builder(KEY_QUERY)
                .setLabel("搜索歌曲")
                .setAllowFreeFormInput(true)
                .setEditChoicesBeforeSending(RemoteInput.EDIT_CHOICES_BEFORE_SENDING_AUTO)
                .build(),
        )
        RemoteInputIntentHelper.putRemoteInputsExtra(intent, remoteInputs)
        launcher.launch(intent)
    }

    WearListScreen(
        title = state.query.ifBlank { "搜索" },
        isRefreshing = state.loading,
        onRefresh = { if (state.query.isNotBlank()) vm.search(state.query) },
        edgeButton = {
            EdgeButton(onClick = ::launchInput) {
                Text("输入")
            }
        },
    ) { spec ->
        item {
            Button(
                onClick = ::launchInput,
                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec),
                icon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                label = { Text("用表盘输入搜索") },
            )
        }
        if (state.loading) {
            item { LoadingItem() }
        }
        if (state.results.isEmpty() && !state.loading) {
            if (state.hots.isEmpty()) {
                item { MessageItem("热搜加载中") }
            } else {
                state.hots.take(12).forEach { hot ->
                    item(key = "hot-$hot") {
                        Button(
                            onClick = { vm.search(hot) },
                            modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                            transformation = SurfaceTransformation(spec),
                            label = { Text(hot) },
                        )
                    }
                }
            }
        } else {
            state.results.forEachIndexed { index, song ->
                item(key = song.id) {
                    SongRow(
                        title = song.name,
                        subtitle = song.artists,
                        coverUrl = song.coverUrl,
                        transformation = SurfaceTransformation(spec),
                        modifier = Modifier.transformedHeight(this, spec),
                    ) { onPlay(state.results, index) }
                }
            }
        }
    }
}
