package com.resume.builder

import android.app.Application
import com.pixelforge.ads.AdsSdk
import com.pixelforge.ads.di.adsModule
import com.resume.builder.di.appModules
import com.resume.builder.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.logger.Level
import org.koin.core.context.startKoin

class ResumeApp : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val adsSdk: AdsSdk by inject()
    private val preferences: PreferencesRepository by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@ResumeApp)
            modules(appModules + adsModule(adsEnabled = true))
        }
        // A running ads-free window must gate the splash interstitial too, so load it before init.
        adsSdk.syncAdFreeUntil(preferences.adFreeUntilEpochMs.value)
        // Firebase + Remote Config fetch + Mobile Ads init, off the main thread. Splash waits on it.
        appScope.launch {
            adsSdk.initialize()
        }
    }
}
