package com.refresh.netmusic.player

import androidx.media3.common.Player

object PlayerHolder {
    @Volatile
    var player: Player? = null
}
