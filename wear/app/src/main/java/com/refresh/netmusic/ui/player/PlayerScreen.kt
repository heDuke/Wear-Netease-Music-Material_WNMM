package com.refresh.netmusic.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.foundation.pager.HorizontalPager
import androidx.wear.compose.foundation.pager.rememberPagerState
import androidx.wear.compose.foundation.rotary.rotaryScrollable
import androidx.wear.compose.material3.AnimatedPage
import androidx.wear.compose.material3.ButtonGroup
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.HorizontalPagerScaffold
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.IconButton
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.PagerScaffoldDefaults
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.transformedHeight
import coil3.compose.AsyncImage
import com.google.android.horologist.audio.ui.VolumeViewModel
import com.google.android.horologist.audio.ui.material3.VolumeLevelIndicator
import com.google.android.horologist.audio.ui.material3.VolumeScreen
import com.google.android.horologist.audio.ui.material3.volumeRotaryBehavior
import com.google.android.horologist.media.ui.material3.components.animated.AnimatedMediaControlButtons
import com.google.android.horologist.media.ui.material3.screens.player.PlayerScreen as HorologistPlayerScreen
import com.google.android.horologist.media.ui.state.model.TrackPositionUiModel
import com.refresh.netmusic.ui.components.MessageItem
import com.refresh.netmusic.ui.components.SongRow
import com.refresh.netmusic.ui.components.WearListScreen
import kotlin.time.Duration.Companion.milliseconds
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayerScreen(vm: PlayerViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pager = rememberPagerState(pageCount = { 4 })
    val volumeViewModel: VolumeViewModel = viewModel(factory = VolumeViewModel.Factory)
    val volumeUiState by volumeViewModel.volumeUiState.collectAsStateWithLifecycle()
    var showVolume by remember { mutableStateOf(false) }

    if (showVolume) {
        val volumeScroll = rememberTransformingLazyColumnState()
        ScreenScaffold(
            scrollState = volumeScroll,
            edgeButton = {
                EdgeButton(onClick = { showVolume = false }) { Text("完成") }
            },
        ) {
            VolumeScreen(volumeViewModel = volumeViewModel)
        }
        return
    }

    HorizontalPagerScaffold(pagerState = pager) {
        HorizontalPager(
            state = pager,
            flingBehavior = PagerScaffoldDefaults.snapWithSpringFlingBehavior(state = pager),
        ) { page ->
            AnimatedPage(pageIndex = page, pagerState = pager) {
                when (page) {
                    0 -> ControlsPage(
                        state = state,
                        volumeViewModel = volumeViewModel,
                        volumeUiStateProvider = { volumeUiState },
                        onToggle = vm::toggle,
                        onPrev = vm::prev,
                        onNext = vm::next,
                        onLike = vm::like,
                        onVolume = { showVolume = true },
                    )
                    1 -> LyricsPage(state)
                    2 -> WearListScreen(title = "队列") { spec ->
                        if (state.queue.isEmpty()) {
                            item { MessageItem("队列为空") }
                        } else {
                            state.queue.forEachIndexed { index, item ->
                                item(key = "${item.id}-$index") {
                                    SongRow(
                                        title = if (index == state.queueIndex) "▶ ${item.title}" else item.title,
                                        subtitle = item.artist,
                                        coverUrl = item.coverUrl,
                                        transformation = SurfaceTransformation(spec),
                                        modifier = Modifier.transformedHeight(this, spec),
                                    ) { vm.playIndex(index) }
                                }
                            }
                        }
                    }
                    else -> WearListScreen(title = "评论") { spec ->
                        if (state.comments.isEmpty()) {
                            item { MessageItem("暂无评论") }
                        } else {
                            state.comments.forEach { c ->
                                item(key = c.id) {
                                    SongRow(
                                        title = c.nickname,
                                        subtitle = c.content,
                                        coverUrl = c.avatarUrl,
                                        transformation = SurfaceTransformation(spec),
                                        modifier = Modifier.transformedHeight(this, spec),
                                        onClick = {},
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ControlsPage(
    state: PlayerUi,
    volumeViewModel: VolumeViewModel,
    volumeUiStateProvider: () -> com.google.android.horologist.audio.ui.VolumeUiState,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onLike: () -> Unit,
    onVolume: () -> Unit,
) {
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
    val positionModel = if (state.duration > 0) {
        TrackPositionUiModel.Actual(
            percent = (state.position.toFloat() / state.duration).coerceIn(0f, 1f),
            duration = state.duration.milliseconds,
            position = state.position.milliseconds,
            shouldAnimate = state.playing,
        )
    } else {
        TrackPositionUiModel.Hidden
    }
    ScreenScaffold {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .rotaryScrollable(
                    behavior = volumeRotaryBehavior(
                        volumeUiStateProvider = volumeUiStateProvider,
                        onRotaryVolumeInput = volumeViewModel::setVolume,
                    ),
                    focusRequester = focusRequester,
                ),
        ) {
            HorologistPlayerScreen(
                mediaDisplay = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            state.title.ifBlank { "未播放" },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            state.artist,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                        )
                    }
                },
                controlButtons = {
                    AnimatedMediaControlButtons(
                        onPlayButtonClick = onToggle,
                        onPauseButtonClick = onToggle,
                        playPauseButtonEnabled = state.title.isNotBlank(),
                        playing = state.playing,
                        onSeekToPreviousButtonClick = onPrev,
                        seekToPreviousButtonEnabled = true,
                        onSeekToNextButtonClick = onNext,
                        seekToNextButtonEnabled = true,
                        trackPositionUiModel = positionModel,
                    )
                },
                buttons = {
                    ButtonGroup {
                        IconButton(onClick = onLike) {
                            Icon(
                                if (state.liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "喜欢",
                            )
                        }
                        IconButton(onClick = onVolume) {
                            Icon(Icons.Outlined.VolumeUp, contentDescription = "音量")
                        }
                    }
                },
                background = {
                    if (state.coverUrl.isNotBlank()) {
                        AsyncImage(
                            model = state.coverUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().alpha(0.28f),
                        )
                    }
                },
            )
            VolumeLevelIndicator(
                volumeUiState = volumeUiStateProvider,
                displayIndicatorEvents = volumeViewModel.displayIndicatorEvents,
            )
        }
    }
}

@Composable
private fun LyricsPage(state: PlayerUi) {
    val line = state.lyrics.lines.lastOrNull { it.timeMs <= state.position }?.text ?: "暂无歌词"
    ScreenScaffold {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                line,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
