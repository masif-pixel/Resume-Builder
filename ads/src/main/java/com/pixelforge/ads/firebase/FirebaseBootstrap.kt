package com.pixelforge.ads.firebase

import android.content.Context
import android.os.Bundle
import com.google.firebase.FirebaseApp
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.perf.FirebasePerformance

/**
 * Firebase bootstrap for Analytics, Crashlytics, Performance, and FCM token access.
 * Requires `google-services.json` + Google Services / Crashlytics / Perf plugins on the app module.
 */
class FirebaseBootstrap(context: Context) {
    private val appContext = context.applicationContext

    private val analytics: FirebaseAnalytics by lazy {
        ensureInitialized()
        FirebaseAnalytics.getInstance(appContext)
    }

    val crashlytics: FirebaseCrashlytics by lazy {
        ensureInitialized()
        FirebaseCrashlytics.getInstance()
    }

    val performance: FirebasePerformance by lazy {
        ensureInitialized()
        FirebasePerformance.getInstance()
    }

    val messaging: FirebaseMessaging by lazy {
        ensureInitialized()
        FirebaseMessaging.getInstance()
    }

    fun ensureInitialized(): FirebaseApp {
        val existing = FirebaseApp.getApps(appContext)
        return if (existing.isEmpty()) {
            FirebaseApp.initializeApp(appContext)
                ?: error("FirebaseApp.initializeApp returned null — check google-services.json")
        } else {
            FirebaseApp.getInstance()
        }
    }

    fun logEvent(name: String, params: Bundle = Bundle()) {
        analytics.logEvent(name, params)
    }

    fun logAdImpression(format: String, adUnitId: String) {
        logEvent(
            "ad_impression",
            Bundle().apply {
                putString("ad_format", format)
                putString("ad_unit_id", adUnitId)
            }
        )
    }

    fun logAdClick(format: String, adUnitId: String) {
        logEvent(
            "ad_click",
            Bundle().apply {
                putString("ad_format", format)
                putString("ad_unit_id", adUnitId)
            }
        )
    }

    fun recordException(throwable: Throwable) {
        crashlytics.recordException(throwable)
    }

    fun setCrashlyticsCollectionEnabled(enabled: Boolean) {
        crashlytics.isCrashlyticsCollectionEnabled = enabled
    }

    fun setPerformanceCollectionEnabled(enabled: Boolean) {
        performance.isPerformanceCollectionEnabled = enabled
    }
}
