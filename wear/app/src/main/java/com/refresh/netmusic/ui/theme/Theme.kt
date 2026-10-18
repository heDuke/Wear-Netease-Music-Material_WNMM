package com.refresh.netmusic.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material3.MaterialTheme

val NeteaseRed = Color(0xFFEC4141)

@Composable
fun NetMusicTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}
