package com.pixelforge.ads.appopen

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAd
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.appopen.AppOpenAdPreloader
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.pixelforge.ads.common.AdShowCallback
import com.pixelforge.ads.common.AdShowResult
import com.pixelforge.ads.common.AdShowingDialog
import com.pixelforge.ads.common.adsLog
import com.pixelforge.ads.common.adsLogW
import com.pixelforge.ads.common.gmaCall
import com.pixelforge.ads.common.GmaRuntime
import com.pixelforge.ads.config.AdsConfig
import com.pixelforge.ads.firebase.FirebaseBootstrap
import com.pixelforge.ads.remoteconfig.AdFormat
import com.pixelforge.ads.remoteconfig.AdsEligibility
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Shows an app-open ad when the process returns from background (warm start).
 * Suppressed during splash / first-run via [setSuppressed]. Never shows on cold start.
 */
class AppOpenAdManager(
    private val application: Application,
    private val config: AdsConfig,
    private val firebase: FirebaseBootstrap,
    private val eligibility: AdsEligibility,
) : DefaultLifecycleObserver, Application.ActivityLifecycleCallbacks {

    private val unitId get() = config.unitIds.appOpen
    private var currentActivity: Activity? = null
    private var showingAd = false
    private val lifecycleRegistered = AtomicBoolean(false)
    private val preloadStarted = AtomicBoolean(false)
    /** Named reasons so splash + paywall can suppress independently. */
    private val suppressReasons = mutableSetOf(REASON_SPLASH)
    /** After interstitial/AdActivity, the next process ON_START is not a real app resume. */
    private val skipNextWarmStart = AtomicBoolean(false)
    /** Cleared on the first process ON_START (even if suppressed), so splash does not burn the skip. */
    private var isColdStart = true

    fun startPreload() {
        ensurePreloaded()
    }

    /** Start / refresh the preloader so the next foreground show is ready. */
    fun ensurePreloaded() {
        if (!GmaRuntime.isReady()) {
            adsLogW("preload_skip", SCREEN, "MobileAds not initialized")
            return
        }
        if (!eligibility.canShow(AdFormat.APP_OPEN)) {
            adsLogW("preload_skip", SCREEN, "app open disabled")
            return
        }
        val alreadyReady = gmaCall(false) { AppOpenAdPreloader.isAdAvailable(unitId) }
        if (alreadyReady) {
            adsLog("preload_already_ready", SCREEN)
            return
        }
        if (!preloadStarted.compareAndSet(false, true)) {
            adsLog("preload_in_progress", SCREEN)
            return
        }
        adsLog("preload_start", SCREEN, "unit=$unitId")
        val started = gmaCall(false) {
            val request = AdRequest.Builder(unitId).build()
            val preload = PreloadConfiguration(request, bufferSize = config.fullscreenBufferSize)
            AppOpenAdPreloader.start(
                unitId,
                preload,
                object : PreloadCallback {
                    override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                        adsLog("preload_success", SCREEN)
                    }

                    override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                        preloadStarted.set(false)
                        adsLogW("preload_failed", SCREEN, adError.message)
                    }

                    override fun onAdsExhausted(preloadId: String) {
                        preloadStarted.set(false)
                        adsLogW("preload_exhausted", SCREEN)
                    }
                }
            )
        }
        if (!started) preloadStarted.set(false)
    }

    /** Drop a consumed preloader and start a fresh load for the next warm start. */
    private fun reloadNow() {
        val available = gmaCall(false) { AppOpenAdPreloader.isAdAvailable(unitId) }
        if (available) {
            adsLog("preload_already_ready", SCREEN)
            return
        }
        preloadStarted.set(false)
        gmaCall(false) { AppOpenAdPreloader.destroy(unitId) }
        ensurePreloaded()
    }

    fun registerLifecycle() {
        if (!lifecycleRegistered.compareAndSet(false, true)) return
        adsLog("lifecycle_registered", SCREEN)
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /** Pause app-open during splash, onboarding, paywall, etc. Reasons stack independently. */
    fun setSuppressed(value: Boolean, reason: String = REASON_SPLASH) {
        val nowSuppressed: Boolean
        val changed: Boolean
        synchronized(suppressReasons) {
            val before = suppressReasons.isNotEmpty()
            if (value) suppressReasons.add(reason) else suppressReasons.remove(reason)
            nowSuppressed = suppressReasons.isNotEmpty()
            changed = before != nowSuppressed
        }
        adsLog(
            if (value) "suppress_on" else "suppress_off",
            SCREEN,
            "reason=$reason active=${synchronized(suppressReasons) { suppressReasons.toList() }}"
        )
        if (changed && !nowSuppressed) ensurePreloaded()
    }

    fun isSuppressed(): Boolean = synchronized(suppressReasons) { suppressReasons.isNotEmpty() }

    /**
     * Call immediately before any fullscreen ad [show] so AdActivity cannot look like a warm start.
     */
    fun notifyFullscreenShowing() {
        skipNextWarmStart.set(true)
        setSuppressed(true, REASON_FULLSCREEN)
    }

    /** Call from fullscreen dismiss / fail. The following ON_START is also skipped. */
    fun notifyFullscreenClosed() {
        skipNextWarmStart.set(true)
        setSuppressed(false, REASON_FULLSCREEN)
    }

    fun isReady(): Boolean =
        eligibility.canShow(AdFormat.APP_OPEN) &&
            gmaCall(false) { AppOpenAdPreloader.isAdAvailable(unitId) }

    fun show(activity: Activity, callback: AdShowCallback? = null) {
        if (!eligibility.canShow(AdFormat.APP_OPEN)) {
            adsLogW("show_skip", SCREEN, "disabled")
            callback?.onResult(AdShowResult.Disabled)
            return
        }
        if (isSuppressed()) {
            adsLogW("show_skip", SCREEN, "suppressed")
            callback?.onResult(AdShowResult.Disabled)
            return
        }
        if (showingAd) {
            adsLogW("show_skip", SCREEN, "already showing")
            return
        }
        if (isFullscreenAdUi(activity)) {
            adsLogW("show_skip", SCREEN, "ad_activity")
            skipNextWarmStart.set(true)
            callback?.onResult(AdShowResult.Disabled)
            return
        }
        val ad = gmaCall(null) { AppOpenAdPreloader.pollAd(unitId) } ?: run {
            adsLogW("show_not_ready", SCREEN)
            ensurePreloaded()
            callback?.onResult(AdShowResult.NotReady)
            return
        }
        present(ad, activity, callback)
    }

    override fun onStart(owner: LifecycleOwner) {
        val cold = isColdStart
        isColdStart = false

        if (!eligibility.canShow(AdFormat.APP_OPEN)) {
            adsLogW("show_skip", SCREEN, "disabled")
            return
        }
        if (isSuppressed()) {
            adsLog("show_skip_suppressed", SCREEN, if (cold) "cold_start" else "warm_start")
            ensurePreloaded()
            return
        }
        if (showingAd) return

        val activity = currentActivity
        if (isFullscreenAdUi(activity)) {
            adsLog("show_skip_ad_activity", SCREEN)
            skipNextWarmStart.set(true)
            return
        }
        if (skipNextWarmStart.compareAndSet(true, false)) {
            adsLog("show_skip_after_fullscreen", SCREEN)
            ensurePreloaded()
            return
        }

        if (cold && !config.showAppOpenOnFirstLaunch) {
            adsLog("show_skip_cold_start", SCREEN)
            ensurePreloaded()
            return
        }

        if (activity == null) {
            adsLogW("show_skip", SCREEN, "no activity")
            return
        }
        adsLog("show_warm_start", SCREEN)
        show(activity)
    }

    override fun onStop(owner: LifecycleOwner) {
        if (showingAd) return
        // A real background clears the one-shot skip left by our own AdActivity,
        // so the next foreground can show the ad that was loaded on dismiss.
        if (!isSuppressed()) {
            skipNextWarmStart.set(false)
        }
        adsLog("background_preload", SCREEN)
        ensurePreloaded()
    }

    private fun present(ad: AppOpenAd, activity: Activity, callback: AdShowCallback?) {
        showingAd = true
        adsLog("show_start", SCREEN)
        ad.adEventCallback = object : AppOpenAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                adsLog("show_success", SCREEN)
                callback?.onResult(AdShowResult.Shown)
            }

            override fun onAdDismissedFullScreenContent() {
                showingAd = false
                // Closing our own ad resumes the app. That resume is not a background return.
                skipNextWarmStart.set(true)
                adsLog("show_dismissed", SCREEN)
                reloadNow()
                callback?.onResult(AdShowResult.Dismissed)
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                showingAd = false
                skipNextWarmStart.set(true)
                adsLogW("show_failed", SCREEN, error.message)
                reloadNow()
                callback?.onResult(AdShowResult.Failed(error.message.orEmpty()))
            }

            override fun onAdImpression() {
                adsLog("impression", SCREEN)
                firebase.logAdImpression("app_open", unitId)
            }

            override fun onAdClicked() {
                adsLog("click", SCREEN)
                firebase.logAdClick("app_open", unitId)
            }
        }
        AdShowingDialog.showThen(activity) {
            gmaCall(Unit) { ad.show(activity) }
        }
    }

    fun destroy() {
        adsLog("destroy", SCREEN)
        showingAd = false
        preloadStarted.set(false)
        gmaCall(false) { AppOpenAdPreloader.destroy(unitId) }
        if (lifecycleRegistered.compareAndSet(true, false)) {
            runCatching {
                ProcessLifecycleOwner.get().lifecycle.removeObserver(this)
                application.unregisterActivityLifecycleCallbacks(this)
            }
        }
        currentActivity = null
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityStarted(activity: Activity) {
        currentActivity = activity
        if (isFullscreenAdUi(activity)) {
            skipNextWarmStart.set(true)
        }
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = activity
    }

    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) {
        if (currentActivity === activity)         currentActivity = null
    }

    companion object {
        const val SCREEN = "AppOpen"
        const val REASON_SPLASH = "splash"
        const val REASON_PAYWALL = "paywall"
        const val REASON_ADS_FREE = "ads_free"
        const val REASON_FULLSCREEN = "fullscreen"

        fun isFullscreenAdUi(activity: Activity?): Boolean {
            val name = activity?.javaClass?.name ?: return false
            return name.contains("AdActivity")
        }
    }
}
