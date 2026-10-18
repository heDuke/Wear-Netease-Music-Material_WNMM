package com.refresh.netmusic.nav

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface Route : NavKey {
    @Serializable
    data object Login : Route

    @Serializable
    data object Home : Route

    @Serializable
    data object Search : Route

    @Serializable
    data object Library : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data class Playlist(val id: Long, val name: String) : Route

    @Serializable
    data object Player : Route
}
