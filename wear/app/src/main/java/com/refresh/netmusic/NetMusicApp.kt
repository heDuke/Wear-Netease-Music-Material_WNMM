package com.refresh.netmusic

import android.app.Application
import com.refresh.netmusic.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class NetMusicApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@NetMusicApp)
            modules(appModule)
        }
    }
}
