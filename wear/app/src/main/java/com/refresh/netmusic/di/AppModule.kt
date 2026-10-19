package com.refresh.netmusic.di

import com.refresh.netmusic.data.SessionStore
import com.refresh.netmusic.ncm.api.NeteaseApi
import com.refresh.netmusic.ncm.request.NeteaseRequest
import com.refresh.netmusic.ui.home.HomeViewModel
import com.refresh.netmusic.ui.library.LibraryViewModel
import com.refresh.netmusic.ui.login.LoginViewModel
import com.refresh.netmusic.ui.player.PlayerViewModel
import com.refresh.netmusic.ui.playlist.PlaylistViewModel
import com.refresh.netmusic.ui.search.SearchViewModel
import com.refresh.netmusic.ui.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    single { SessionStore(androidContext()) }
    single {
        val request = NeteaseRequest()
        val session = get<SessionStore>()
        val saved = session.loadCookies()
        if (saved.isNotEmpty()) request.cookies.replaceAll(saved)
        NeteaseApi(request)
    }
    viewModel { LoginViewModel(get(), get()) }
    viewModel { HomeViewModel(get()) }
    viewModel { LibraryViewModel(get(), get()) }
    viewModel { SearchViewModel(get()) }
    viewModel { PlaylistViewModel(get()) }
    viewModel { PlayerViewModel(get(), get()) }
    viewModel { SettingsViewModel(get(), get()) }
}
