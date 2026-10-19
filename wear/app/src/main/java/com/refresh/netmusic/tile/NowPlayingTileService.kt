package com.refresh.netmusic.tile

import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.types.layoutString
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.TileService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.refresh.netmusic.player.PlayerHolder

class NowPlayingTileService : TileService() {
    override fun onTileRequest(requestParams: RequestBuilders.TileRequest): ListenableFuture<TileBuilders.Tile> {
        val np = PlayerHolder.nowPlaying.value
        val title = np?.title?.ifBlank { "NetMusic" } ?: "NetMusic"
        val artist = np?.artist?.ifBlank { "for WearOS" } ?: "for WearOS"
        val root = materialScope(
            context = this,
            deviceConfiguration = requestParams.deviceConfiguration,
        ) {
            primaryLayout(
                titleSlot = { text("正在播放".layoutString, typography = Typography.TITLE_SMALL) },
                mainSlot = {
                    text("$title\n$artist".layoutString, typography = Typography.BODY_LARGE)
                },
            )
        }
        val layout = LayoutElementBuilders.Layout.Builder().setRoot(root).build()
        val timeline = TimelineBuilders.Timeline.Builder()
            .addTimelineEntry(
                TimelineBuilders.TimelineEntry.Builder().setLayout(layout).build(),
            )
            .build()
        return Futures.immediateFuture(
            TileBuilders.Tile.Builder()
                .setResourcesVersion("1")
                .setTileTimeline(timeline)
                .build(),
        )
    }

    override fun onTileResourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest,
    ): ListenableFuture<ResourceBuilders.Resources> {
        return Futures.immediateFuture(
            ResourceBuilders.Resources.Builder().setVersion("1").build(),
        )
    }
}
