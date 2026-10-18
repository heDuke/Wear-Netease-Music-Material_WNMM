package com.refresh.netmusic.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.Text
import com.refresh.netmusic.ui.components.WearListScreen
import org.koin.androidx.compose.koinViewModel

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit,
    vm: SettingsViewModel = koinViewModel(),
) {
    val quality by vm.quality.collectAsStateWithLifecycle()
    val loggedOut by vm.loggedOut.collectAsStateWithLifecycle()
    LaunchedEffect(loggedOut) { if (loggedOut) onLoggedOut() }
    WearListScreen(title = "设置") {
        listOf("standard", "higher", "exhigh").forEach { level ->
            item {
                Button(
                    onClick = { vm.setQuality(level) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (quality == level) "音质 · $level ✓" else "音质 · $level") },
                )
            }
        }
        item {
            Button(onClick = vm::logout, modifier = Modifier.fillMaxWidth(), label = { Text("退出登录") })
        }
    }
}
