package com.refresh.netmusic

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.wear.compose.material3.AppScaffold
import androidx.wear.compose.navigation3.rememberSwipeDismissableSceneStrategy
import com.refresh.netmusic.ncm.model.Song
import com.refresh.netmusic.nav.Route
import com.refresh.netmusic.player.PlaybackService
import com.refresh.netmusic.ui.home.HomeScreen
import com.refresh.netmusic.ui.library.LibraryScreen
import com.refresh.netmusic.ui.login.LoginScreen
import com.refresh.netmusic.ui.player.PlayerScreen
import com.refresh.netmusic.ui.playlist.PlaylistScreen
import com.refresh.netmusic.ui.search.SearchScreen
import com.refresh.netmusic.ui.settings.SettingsScreen
import com.refresh.netmusic.ui.theme.NetMusicTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startService(Intent(this, PlaybackService::class.java))
        setContent {
            NetMusicTheme {
                AppScaffold {
                    NetMusicNav()
                }
            }
        }
    }
}

@Composable
private fun NetMusicNav() {
    val backStack = rememberNavBackStack(Route.Login)
    val strategy = rememberSwipeDismissableSceneStrategy<Route>()

    fun play(songs: List<Song>, index: Int) {
        PlaybackService.instance?.playSongs(songs, index)
        backStack.add(Route.Player)
    }

    NavDisplay(
        backStack = backStack,
        sceneStrategy = strategy,
        entryProvider = { key ->
            when (val route = key as Route) {
                Route.Login -> NavEntry(route) {
                    LoginScreen(onLoggedIn = {
                        backStack.clear()
                        backStack.add(Route.Home)
                    })
                }
                Route.Home -> NavEntry(route) {
                    HomeScreen(
                        onSong = ::play,
                        onPlaylist = { backStack.add(Route.Playlist(it.id, it.name)) },
                        onSearch = { backStack.add(Route.Search) },
                        onLibrary = { backStack.add(Route.Library) },
                    )
                }
                Route.Search -> NavEntry(route) {
                    SearchScreen(onPlay = ::play)
                }
                Route.Library -> NavEntry(route) {
                    LibraryScreen(
                        onPlaylist = { backStack.add(Route.Playlist(it.id, it.name)) },
                        onSettings = { backStack.add(Route.Settings) },
                    )
                }
                Route.Settings -> NavEntry(route) {
                    SettingsScreen(onLoggedOut = {
                        backStack.clear()
                        backStack.add(Route.Login)
                    })
                }
                is Route.Playlist -> NavEntry(route) {
                    PlaylistScreen(id = route.id, name = route.name, onPlay = ::play)
                }
                Route.Player -> NavEntry(route) {
                    PlayerScreen()
                }
            }
        },
    )
}
