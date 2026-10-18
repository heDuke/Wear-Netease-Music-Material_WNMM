package com.refresh.netmusic.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.MaterialTheme
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.refresh.netmusic.ui.components.WearListScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlayerScreen(vm: PlayerViewModel = koinViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val pager = rememberPagerState(pageCount = { 4 })
    HorizontalPager(state = pager, modifier = Modifier.fillMaxSize()) { page ->
        when (page) {
            0 -> ScreenScaffold {
                Column(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(state.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    Text(state.artist, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "${format(state.position)} / ${format(state.duration)}",
                        modifier = Modifier.padding(top = 8.dp),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                        Button(onClick = vm::prev, label = { Text("上一首") })
                        Button(onClick = vm::toggle, label = { Text(if (state.playing) "暂停" else "播放") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 8.dp)) {
                        Button(onClick = vm::next, label = { Text("下一首") })
                        Button(onClick = vm::like, label = { Text(if (state.liked) "已喜欢" else "喜欢") })
                    }
                }
            }
            1 -> {
                val line = state.lyrics.lines.lastOrNull { it.timeMs <= state.position }?.text ?: "暂无歌词"
                ScreenScaffold {
                    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(line, textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            2 -> WearListScreen("队列") {
                item { Text("左右滑动查看评论和歌词", modifier = Modifier.fillMaxWidth()) }
            }
            else -> WearListScreen("评论") {
                if (state.comments.isEmpty()) {
                    item { Text("暂无评论") }
                } else {
                    state.comments.forEach { c ->
                        item {
                            Text("${c.nickname}：${c.content}", maxLines = 4)
                        }
                    }
                }
            }
        }
    }
}

private fun format(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    return "${s / 60}:${(s % 60).toString().padStart(2, '0')}"
}
