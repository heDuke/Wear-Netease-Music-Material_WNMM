package com.refresh.netmusic.ui.theme

import android.graphics.Bitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import androidx.wear.compose.material3.ColorScheme
import androidx.wear.compose.material3.MaterialTheme
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.allowHardware
import coil3.toBitmap
import com.refresh.netmusic.player.PlayerHolder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

val Mint = Color(0xFFA4D5D2)
val DeepTeal = Color(0xFF0E3B3A)

fun mintColorScheme(): ColorScheme = ColorScheme(
    primary = Mint,
    primaryDim = Color(0xFF7BB8B4),
    primaryContainer = Color(0xFF1A4F4C),
    onPrimary = DeepTeal,
    onPrimaryContainer = Mint,
    secondary = Color(0xFF8EC4C1),
    secondaryDim = Color(0xFF6AA09D),
    secondaryContainer = Color(0xFF163F3D),
    onSecondary = DeepTeal,
    onSecondaryContainer = Color(0xFFC9EBE8),
    tertiary = Color(0xFFB8D4C8),
    tertiaryDim = Color(0xFF8FB3A6),
    tertiaryContainer = Color(0xFF1E3D34),
    onTertiary = Color(0xFF0A2A22),
    onTertiaryContainer = Color(0xFFD4EDE4),
)

fun colorSchemeFromSeed(seed: Color): ColorScheme {
    val dim = seed.copy(alpha = 0.78f)
    val onSeed = if (seed.luminance() > 0.45f) Color(0xFF101414) else Color(0xFFF4F7F6)
    val container = seed.copy(alpha = 1f).darken(0.55f)
    return mintColorScheme().copy(
        primary = seed,
        primaryDim = dim,
        primaryContainer = container,
        onPrimary = onSeed,
        onPrimaryContainer = seed.lighten(0.25f),
        secondary = seed.lighten(0.08f),
        secondaryDim = seed.darken(0.12f),
        tertiary = seed.lighten(0.12f),
        tertiaryContainer = container,
        onTertiaryContainer = seed.lighten(0.3f),
    )
}

@Composable
fun NetMusicTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val coverFlow = remember { PlayerHolder.nowPlaying.map { it?.coverUrl.orEmpty() } }
    var scheme by remember { mutableStateOf(mintColorScheme()) }
    LaunchedEffect(Unit) {
        coverFlow.collect { url ->
            scheme = if (url.isBlank()) {
                mintColorScheme()
            } else {
                extractSeed(context, url)?.let { colorSchemeFromSeed(it) } ?: mintColorScheme()
            }
        }
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

private suspend fun extractSeed(context: android.content.Context, url: String): Color? =
    withContext(Dispatchers.IO) {
        runCatching {
            val request = ImageRequest.Builder(context)
                .data(url)
                .allowHardware(false)
                .build()
            val image = context.imageLoader.execute(request).image ?: return@runCatching null
            val bitmap: Bitmap = image.toBitmap()
            val palette = Palette.from(bitmap).clearFilters().generate()
            val rgb = palette.vibrantSwatch?.rgb
                ?: palette.lightVibrantSwatch?.rgb
                ?: palette.dominantSwatch?.rgb
                ?: return@runCatching null
            Color(rgb)
        }.getOrNull()
    }

private fun Color.darken(amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = red * (1f - t),
        green = green * (1f - t),
        blue = blue * (1f - t),
        alpha = 1f,
    )
}

private fun Color.lighten(amount: Float): Color {
    val t = amount.coerceIn(0f, 1f)
    return Color(
        red = red + (1f - red) * t,
        green = green + (1f - green) * t,
        blue = blue + (1f - blue) * t,
        alpha = 1f,
    )
}
