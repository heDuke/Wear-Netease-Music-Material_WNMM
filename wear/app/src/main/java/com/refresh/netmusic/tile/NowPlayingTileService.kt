package com.refresh.netmusic.tile

import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.material3.MaterialScope
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.titleCard
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.timeline.Timeline
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.Material3TileService
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.refresh.netmusic.MainActivity
import com.refresh.netmusic.player.PlayerHolder

class NowPlayingTileService : Material3TileService() {
    override suspend fun MaterialScope.tileResponse(
        requestParams: RequestBuilders.TileRequest,
    ): TileBuilders.Tile {
        val np = PlayerHolder.nowPlaying.value
        val title = np?.title?.ifBlank { "NetMusic" } ?: "NetMusic"
        val artist = np?.artist?.ifBlank { "for WearOS" } ?: "for WearOS"
        val click = clickable(
            ActionBuilders.LaunchAction.Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity.Builder()
                        .setClassName(MainActivity::class.java.name)
                        .setPackageName(packageName)
                        .build(),
                )
                .build(),
        )
        return tile(
            timeline = Timeline.fromLayoutElement(
                primaryLayout(
                    titleSlot = { text("正在播放".layoutString, typography = Typography.TITLE_SMALL) },
                    mainSlot = {
                        titleCard(
                            onClick = click,
                            title = { text(title.layoutString, typography = Typography.TITLE_MEDIUM) },
                            content = { text(artist.layoutString, typography = Typography.BODY_SMALL) },
                        )
                    },
                ),
            ),
        )
    }
}
