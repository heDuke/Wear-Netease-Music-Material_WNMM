package com.refresh.netmusic.ui.library

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.transformedHeight
import com.refresh.netmusic.ncm.model.Playlist
import com.refresh.netmusic.player.PlayerHolder
import com.refresh.netmusic.ui.components.LoadingItem
import com.refresh.netmusic.ui.components.MessageItem
import com.refresh.netmusic.ui.components.PlaylistCard
import com.refresh.netmusic.ui.components.WearListScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun LibraryScreen(
    onPlaylist: (Playlist) -> Unit,
    onSettings: () -> Unit,
    onNowPlaying: () -> Unit,
    vm: LibraryViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val nowPlaying by PlayerHolder.nowPlaying.collectAsStateWithLifecycle()
    WearListScreen(
        title = state.profile?.nickname?.ifBlank { "我的" } ?: "我的",
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
                onClick = onSettings,
                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec),
                icon = { Icon(Icons.Outlined.Settings, contentDescription = null) },
                label = { Text("设置") },
            )
        }
        if (state.loading && state.playlists.isEmpty()) {
            item { LoadingItem() }
        }
        if (state.error != null) {
            item { MessageItem(state.error.orEmpty()) }
        }
        state.playlists.forEach { pl ->
            item(key = pl.id) {
                PlaylistCard(
                    name = pl.name,
                    subtitle = "${pl.trackCount} 首 · ${pl.creator}",
                    coverUrl = pl.coverUrl,
                    transformation = SurfaceTransformation(spec),
                    modifier = Modifier.transformedHeight(this, spec),
                ) { onPlaylist(pl) }
            }
        }
    }
}
