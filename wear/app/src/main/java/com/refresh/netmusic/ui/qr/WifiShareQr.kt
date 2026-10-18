package com.refresh.netmusic.ui.qr

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder

@Composable
fun WifiShareQr(
    content: String,
    modifier: Modifier = Modifier,
    moduleColor: Color = Color(0xFFFFB4AB),
    background: Color = Color.Transparent,
) {
    val matrix = remember(content) {
        val code = Encoder.encode(
            content,
            ErrorCorrectionLevel.M,
            mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.CHARACTER_SET to "UTF-8"),
        )
        code.matrix
    }
    Canvas(modifier.fillMaxWidth(0.78f).aspectRatio(1f)) {
        drawRoundRect(background, cornerRadius = CornerRadius(size.minDimension * 0.12f))
        val n = matrix.width
        val cell = size.minDimension / n
        val radius = cell * 0.38f
        for (y in 0 until n) {
            for (x in 0 until n) {
                if (matrix.get(x, y).toInt() != 1) continue
                drawRoundRect(
                    color = moduleColor,
                    topLeft = Offset(x * cell, y * cell),
                    size = Size(cell, cell),
                    cornerRadius = CornerRadius(radius, radius),
                    style = Fill,
                )
            }
        }
    }
}
