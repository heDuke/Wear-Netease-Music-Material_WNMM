package com.refresh.netmusic.ui.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material3.AlertDialog
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.FilledTonalButton
import androidx.wear.compose.material3.ListHeader
import androidx.wear.compose.material3.RadioButton
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.lazy.transformedHeight
import com.refresh.netmusic.ui.components.WearListScreen
import org.koin.androidx.compose.koinViewModel

private val qualityOptions = listOf(
    "standard" to "标准",
    "higher" to "较高",
    "exhigh" to "极高",
)

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit,
    vm: SettingsViewModel = koinViewModel(),
) {
    val quality by vm.quality.collectAsStateWithLifecycle()
    val loggedOut by vm.loggedOut.collectAsStateWithLifecycle()
    var confirmLogout by remember { mutableStateOf(false) }
    LaunchedEffect(loggedOut) { if (loggedOut) onLoggedOut() }
    WearListScreen(title = "设置") { spec ->
        item {
            ListHeader(
                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec),
            ) { Text("音质") }
        }
        qualityOptions.forEach { (level, label) ->
            item(key = level) {
                RadioButton(
                    selected = quality == level,
                    onSelect = { vm.setQuality(level) },
                    modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                    transformation = SurfaceTransformation(spec),
                    label = { Text(label) },
                    secondaryLabel = { Text(level) },
                )
            }
        }
        item {
            Button(
                onClick = { confirmLogout = true },
                modifier = Modifier.fillMaxWidth().transformedHeight(this, spec),
                transformation = SurfaceTransformation(spec),
                label = { Text("退出登录") },
            )
        }
    }
    AlertDialog(
        visible = confirmLogout,
        onDismissRequest = { confirmLogout = false },
        title = { Text("退出登录") },
        text = { Text("将清除加密保存的登录信息") },
        confirmButton = {
            Button(
                onClick = {
                    confirmLogout = false
                    vm.logout()
                },
            ) { Text("退出") }
        },
        dismissButton = {
            FilledTonalButton(onClick = { confirmLogout = false }) { Text("取消") }
        },
    )
}
