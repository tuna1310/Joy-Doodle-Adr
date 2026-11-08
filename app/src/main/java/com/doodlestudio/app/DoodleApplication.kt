package com.doodlestudio.app

import android.app.Application
import com.doodlestudio.app.di.appModules
import com.google.android.gms.ads.MobileAds
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class DoodleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize AdMob SDK (must be called before loading any ads)
        MobileAds.initialize(this)

        startKoin {
            androidLogger()
            androidContext(this@DoodleApplication)
            modules(appModules)
        }
    }
}
