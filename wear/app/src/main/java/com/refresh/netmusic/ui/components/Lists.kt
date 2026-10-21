package com.refresh.netmusic.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.TransformingLazyColumnScope
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.CircularProgressIndicator
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.TransformationSpec
import androidx.wear.compose.material3.lazy.rememberTransformationSpec
import androidx.wear.compose.material3.lazy.transformedHeight
import coil3.compose.AsyncImage
import com.refresh.netmusic.player.NowPlaying

@Composable
fun WearListScreen(
    title: String,
    edgeButton: (@Composable BoxScope.() -> Unit)? = null,
    content: TransformingLazyColumnScope.(spec: TransformationSpec) -> Unit,
) {
    val columnState = rememberTransformingLazyColumnState()
    val spec = rememberTransformationSpec()
    val body: @Composable (PaddingValues) -> Unit = { contentPadding ->
        TransformingLazyColumn(
            state = columnState,
            contentPadding = contentPadding,
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                ListHeader(
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                ) {
                    Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            content(spec)
        }
    }
    if (edgeButton != null) {
        ScreenScaffold(scrollState = columnState, edgeButton = edgeButton) { padding ->
            body(padding)
        }
    } else {
        ScreenScaffold(scrollState = columnState) { padding ->
            body(padding)
        }
    }
}

@Composable
fun NowPlayingCard(
    nowPlaying: NowPlaying,
    transformation: SurfaceTransformation? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    TitleCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        transformation = transformation,
        title = {
            Text(
                nowPlaying.title.ifBlank { "正在播放" },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        subtitle = {
            val line = when {
                nowPlaying.error != null && nowPlaying.artist.isNotBlank() ->
                    "${nowPlaying.error} · ${nowPlaying.artist}"
                nowPlaying.error != null -> nowPlaying.error
                nowPlaying.playing -> nowPlaying.artist.ifBlank { "正在播放" }
                nowPlaying.artist.isNotBlank() -> "已暂停 · ${nowPlaying.artist}"
                else -> "已暂停"
            }
            Text(line, maxLines = 2, overflow = TextOverflow.Ellipsis)
        },
    ) {
        if (nowPlaying.coverUrl.isNotBlank()) {
            AsyncImage(
                model = nowPlaying.coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}

@Composable
fun SongRow(
    title: String,
    subtitle: String,
    coverUrl: String = "",
    transformation: SurfaceTransformation? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        transformation = transformation,
        icon = if (coverUrl.isNotBlank()) {
            {
                AsyncImage(
                    model = coverUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)),
                )
            }
        } else {
            null
        },
        label = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        secondaryLabel = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    )
}

@Composable
fun PlaylistCard(
    name: String,
    subtitle: String,
    coverUrl: String = "",
    transformation: SurfaceTransformation? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    TitleCard(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        transformation = transformation,
        title = { Text(name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        subtitle = { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) },
    ) {
        if (coverUrl.isNotBlank()) {
            AsyncImage(
                model = coverUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)),
            )
        }
    }
}

@Composable
fun LoadingItem() {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun MessageItem(text: String, onRetry: (() -> Unit)? = null) {
    if (onRetry != null) {
        FilledTonalButton(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(text, maxLines = 2, overflow = TextOverflow.Ellipsis) },
            secondaryLabel = { Text("点按重试") },
        )
    } else {
        Text(text, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, maxLines = 3)
    }
}
