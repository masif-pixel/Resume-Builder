package com.pixelforge.ads.interstitial

import android.app.Activity
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAd
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.interstitial.InterstitialAdPreloader
import com.pixelforge.ads.appopen.AppOpenAdManager
import com.pixelforge.ads.common.AdShowCallback
import com.pixelforge.ads.common.AdShowResult
import com.pixelforge.ads.common.AdShowingDialog
import com.pixelforge.ads.common.GmaRuntime
import com.pixelforge.ads.common.adsLog
import com.pixelforge.ads.common.adsLogE
import com.pixelforge.ads.common.adsLogW
import com.pixelforge.ads.common.gmaCall
import com.pixelforge.ads.config.AdsConfig
import com.pixelforge.ads.firebase.FirebaseBootstrap
import com.pixelforge.ads.remoteconfig.AdFormat
import com.pixelforge.ads.remoteconfig.AdsEligibility
import com.pixelforge.ads.remoteconfig.AdsRemoteConfig
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resume

/**
 * Splash interstitial ([AdFormat.INTER_SPLASH] + [AdsConfig.unitIds.interstitialSplash])
 * and Home bottom-tab interstitial ([AdFormat.INTER_HOME] + interstitialHome) — separate RC + units.
 */
class InterstitialAdManager(
    private val config: AdsConfig,
    private val firebase: FirebaseBootstrap,
    private val eligibility: AdsEligibility,
    private val remoteConfig: AdsRemoteConfig,
    private val appOpen: AppOpenAdManager,
) {
    private val splashUnitId get() = config.unitIds.interstitialSplash
    private val homeUnitId get() = config.unitIds.interstitialHome

    private var splashOnDemandAd: InterstitialAd? = null
    private val splashLoading = AtomicBoolean(false)
    /** Force update owns splash: drop a loaded ad and never show it. */
    private val splashCancelled = AtomicBoolean(false)
    /** Splash left without showing; the late-loaded splash ad is used on the first Home inter click. */
    private val splashMissed = AtomicBoolean(false)
    private val homeSessionStarted = AtomicBoolean(false)
    private val homeReadyAtElapsedMs = AtomicLong(0L)
    /** One fullscreen interstitial at a time (splash, home, tools). */
    private val presenting = AtomicBoolean(false)
    /** Home/tools show claimed until that attempt finishes, so a second click cannot queue another. */
    private val homeShowClaimed = AtomicBoolean(false)

    /** True after any interstitial was shown this process (language/onboarding one-shot). */
    @Volatile
    var shownThisSession: Boolean = false
        private set

    /** Next-Gen preload for Home interstitial unit (also used by Tools placement). */
    fun startHomePreload() {
        if (!canUseHomeUnit()) {
            adsLogW("preload_skip", SCREEN_HOME, "inter home/tool rc/local off")
            return
        }
        startPreloadFor(homeUnitId, SCREEN_HOME)
    }

    private fun canUseHomeUnit(): Boolean =
        eligibility.canShow(AdFormat.INTER_HOME) // || eligibility.canShow(AdFormat.INTER_TOOL)

    private fun startPreloadFor(unitId: String, screen: String) {
        if (!GmaRuntime.isReady()) {
            adsLogW("preload_skip", screen, "MobileAds not initialized")
            return
        }
        adsLog("preload_start", screen, "interstitial unit=$unitId")
        gmaCall(false) {
            val request = AdRequest.Builder(unitId).build()
            val preload = PreloadConfiguration(request, bufferSize = config.fullscreenBufferSize)
            InterstitialAdPreloader.start(
                unitId,
                preload,
                object : PreloadCallback {
                    override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                        adsLog("preload_success", screen, "interstitial")
                    }

                    override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                        adsLogW("preload_failed", screen, "interstitial ${adError.message}")
                    }

                    override fun onAdsExhausted(preloadId: String) {
                        adsLogW("preload_exhausted", screen, "interstitial")
                    }
                }
            )
        }
    }

    /**
     * Home live: preload home unit + start RC cooldown ([AdsRemoteConfig.interHomeCooldownMs]).
     * Runs when [AdFormat.INTER_HOME] and/or [AdFormat.INTER_TOOL] is on (same unit).
     */
    fun prepareBottomNav() {
        if (!canUseHomeUnit()) {
            adsLogW("home_prepare_skip", SCREEN_HOME, "disabled")
            return
        }
        startHomePreload()
        if (homeSessionStarted.compareAndSet(false, true)) {
            armHomeCooldown("home")
        }
    }

    fun isHomeCooldownElapsed(): Boolean {
        val readyAt = homeReadyAtElapsedMs.get()
        return readyAt > 0L && SystemClock.elapsedRealtime() >= readyAt
    }

    private fun armHomeCooldown(reason: String) {
        val cooldownMs = remoteConfig.interHomeCooldownMs()
        homeReadyAtElapsedMs.set(SystemClock.elapsedRealtime() + cooldownMs)
        adsLog("home_timer_start", SCREEN_HOME, "reason=$reason cooldownMs=$cooldownMs")
    }

    private fun restartHomeCycle() {
        startHomePreload()
        armHomeCooldown("after_dismiss")
    }

    fun isHomeReady(): Boolean {
        if (!canUseHomeUnit()) return false
        return gmaCall(false) { InterstitialAdPreloader.isAdAvailable(homeUnitId) }
    }

    fun isSplashReady(): Boolean {
        if (!eligibility.canShow(AdFormat.INTER_SPLASH)) return false
        if (splashOnDemandAd != null) return true
        return gmaCall(false) { InterstitialAdPreloader.isAdAvailable(splashUnitId) }
    }

    /** @deprecated Prefer [isHomeReady] / [isSplashReady]. */
    fun isReady(): Boolean = isHomeReady() || isSplashReady()

    /**
     * On-demand load for Splash unit. Does not show.
     */
    suspend fun loadSplash(screen: String = SCREEN_SPLASH): Boolean {
        if (splashCancelled.get()) {
            adsLog("load_skip", screen, "inter splash force update")
            return false
        }
        if (!eligibility.canShow(AdFormat.INTER_SPLASH)) {
            adsLogW("load_skip", screen, "inter splash disabled")
            return false
        }
        if (splashOnDemandAd != null || gmaCall(false) {
                InterstitialAdPreloader.isAdAvailable(splashUnitId)
            }
        ) {
            adsLog("load_already_ready", screen, "inter splash")
            return true
        }
        if (!splashLoading.compareAndSet(false, true)) {
            adsLog("load_in_progress", screen, "inter splash")
            return false
        }
        adsLog("load_start", screen, "inter splash unit=$splashUnitId")
        if (!GmaRuntime.isReady()) {
            adsLogW("load_skip", screen, "MobileAds not initialized")
            splashLoading.set(false)
            return false
        }
        return try {
            val result = InterstitialAd.load(AdRequest.Builder(splashUnitId).build())
            when (result) {
                is AdLoadResult.Success -> {
                    if (splashCancelled.get()) {
                        adsLog("load_discard", screen, "inter splash force update")
                        false
                    } else {
                        splashOnDemandAd = result.ad
                        adsLog("load_success", screen, "inter splash")
                        true
                    }
                }
                is AdLoadResult.Failure -> {
                    adsLogW("load_failed", screen, "inter splash ${result.error}")
                    false
                }
            }
        } catch (t: Throwable) {
            adsLogE("load_exception", screen, "inter splash", t)
            false
        } finally {
            splashLoading.set(false)
        }
    }

    /** @deprecated Use [loadSplash]. */
    suspend fun load(screen: String): Boolean = loadSplash(screen)

    fun showSplash(activity: Activity, screen: String, callback: AdShowCallback? = null) {
        if (splashCancelled.get()) {
            adsLog("show_skip", screen, "inter splash force update")
            splashOnDemandAd = null
            callback?.onResult(AdShowResult.Disabled)
            return
        }
        if (!eligibility.canShow(AdFormat.INTER_SPLASH)) {
            adsLogW("show_skip", screen, "inter splash disabled")
            callback?.onResult(AdShowResult.Disabled)
            return
        }
        val ad = splashOnDemandAd?.also { splashOnDemandAd = null }
            ?: pollQuietly(splashUnitId, screen)
        if (ad == null) {
            adsLogW("show_not_ready", screen, "inter splash")
            callback?.onResult(AdShowResult.NotReady)
            return
        }
        present(ad, activity, screen, splashUnitId, destroyUnitId = splashUnitId, callback)
    }

    fun showHome(activity: Activity, screen: String = SCREEN_HOME, callback: AdShowCallback? = null) {
        // INTER_TOOL is commented out in AdsRemoteConfigKeys; every home-unit show uses INTER_HOME.
        val format = AdFormat.INTER_HOME // if (screen == SCREEN_TOOL) AdFormat.INTER_TOOL else AdFormat.INTER_HOME
        if (!eligibility.canShow(format)) {
            adsLogW("show_skip", screen, "inter disabled")
            callback?.onResult(AdShowResult.Disabled)
            return
        }
        val ad = pollQuietly(homeUnitId, screen)
        if (ad == null) {
            adsLogW("show_not_ready", screen, "inter home unit")
            callback?.onResult(AdShowResult.NotReady)
            return
        }
        present(ad, activity, screen, homeUnitId, destroyUnitId = homeUnitId, callback)
    }

    private fun pollQuietly(unitId: String, screen: String): InterstitialAd? =
        gmaCall(null) { InterstitialAdPreloader.pollAd(unitId) }

    /**
     * Shows splash interstitial when process is RESUMED.
     */
    fun showSplashWhenResumed(
        activity: Activity,
        screen: String,
        callback: AdShowCallback? = null,
    ) {
        val processLifecycle = ProcessLifecycleOwner.get().lifecycle
        if (processLifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            activity.runOnUiThread { showSplash(activity, screen, callback) }
            return
        }
        adsLog("show_wait_foreground", screen, "inter splash")
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                owner.lifecycle.removeObserver(this)
                if (splashCancelled.get()) {
                    adsLog("show_skip", screen, "inter splash force update")
                    callback?.onResult(AdShowResult.Disabled)
                    return
                }
                adsLog("show_foreground_resumed", screen, "inter splash")
                activity.runOnUiThread { showSplash(activity, screen, callback) }
            }
        }
        processLifecycle.addObserver(observer)
    }

    fun showHomeWhenResumed(
        activity: Activity,
        screen: String = SCREEN_HOME,
        callback: AdShowCallback? = null,
    ) {
        val processLifecycle = ProcessLifecycleOwner.get().lifecycle
        if (processLifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            activity.runOnUiThread { showHome(activity, screen, callback) }
            return
        }
        adsLog("show_wait_foreground", screen, "inter home unit")
        val observer = object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                owner.lifecycle.removeObserver(this)
                activity.runOnUiThread { showHome(activity, screen, callback) }
            }
        }
        processLifecycle.addObserver(observer)
    }

    /** @deprecated Use [showSplash] / [showHome]. */
    fun show(activity: Activity, screen: String, callback: AdShowCallback? = null) {
        when (screen) {
            SCREEN_HOME, SCREEN_BOTTOM_NAV, SCREEN_TOOL -> showHome(activity, screen, callback)
            else -> showSplash(activity, screen, callback)
        }
    }

    /** @deprecated Use [showSplashWhenResumed] / [showHomeWhenResumed]. */
    fun showWhenResumed(
        activity: Activity,
        screen: String,
        callback: AdShowCallback? = null,
    ) {
        when (screen) {
            SCREEN_HOME, SCREEN_BOTTOM_NAV, SCREEN_TOOL ->
                showHomeWhenResumed(activity, screen, callback)
            else -> showSplashWhenResumed(activity, screen, callback)
        }
    }

    /**
     * Splash: load [splashUnitId], show when ready & foreground, or give up after [timeoutMs].
     */
    suspend fun runSplashGate(
        activity: Activity,
        timeoutMs: Long = SPLASH_AD_TIMEOUT_MS,
    ) {
        val screen = SCREEN_SPLASH
        adsLog("splash_gate_start", screen, "timeoutMs=$timeoutMs")
        if (shownThisSession) {
            adsLog("splash_gate_skip", screen, "already shown this process")
            return
        }
        if (!eligibility.canShow(AdFormat.INTER_SPLASH)) {
            adsLogW("splash_gate_skip", screen, "disabled")
            return
        }
        val loaded = withTimeoutOrNull(timeoutMs) { loadSplash(screen) } ?: run {
            adsLogW("splash_gate_timeout", screen, "load exceeded ${timeoutMs}ms")
            return
        }
        if (!loaded || splashCancelled.get()) {
            splashOnDemandAd = null
            adsLogW("splash_gate_abort", screen, "no ad or force update")
            return
        }
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                var finished = false
                fun finish() {
                    if (finished) return
                    finished = true
                    if (cont.isActive) cont.resume(Unit)
                }
                showSplashWhenResumed(activity, screen) { result ->
                    adsLog("splash_gate_result", screen, result.toString())
                    when (result) {
                        AdShowResult.Shown,
                        AdShowResult.Impression -> Unit
                        AdShowResult.Dismissed,
                        AdShowResult.NotReady,
                        AdShowResult.Disabled,
                        is AdShowResult.Failed,
                        is AdShowResult.Rewarded -> finish()
                    }
                }
                cont.invokeOnCancellation { finished = true }
            }
        }
        adsLog("splash_gate_done", screen)
    }

    /**
     * True once, when a splash interstitial missed its window and is ready for the first Home click.
     */
    private fun consumeMissedSplashAd(): Boolean {
        if (!splashMissed.get() || splashCancelled.get()) return false
        if (isSplashReady()) {
            splashMissed.set(false)
            return true
        }
        if (!splashLoading.get()) splashMissed.set(false)
        return false
    }

    private suspend fun showSplashAndWait(activity: Activity, screen: String) {
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                var finished = false
                fun finish() {
                    if (finished) return
                    finished = true
                    if (cont.isActive) cont.resume(Unit)
                }
                showSplashWhenResumed(activity, screen) { result ->
                    adsLog("home_reuse_splash_result", screen, result.toString())
                    when (result) {
                        AdShowResult.Shown, AdShowResult.Impression -> Unit
                        AdShowResult.Dismissed -> {
                            restartHomeCycle()
                            finish()
                        }
                        else -> {
                            startHomePreload()
                            finish()
                        }
                    }
                }
                cont.invokeOnCancellation { finished = true }
            }
        }
    }

    /**
     * Language / onboarding fallback — uses **splash** inter unit + [AdFormat.INTER_SPLASH].
     * One-shot per process via [shownThisSession].
     */
    suspend fun showOrSkip(activity: Activity, screen: String, loadTimeoutMs: Long = 8_000L) {
        if (shownThisSession) {
            adsLog("show_or_skip_already_shown", screen)
            return
        }
        if (!eligibility.canShow(AdFormat.INTER_SPLASH)) {
            adsLogW("show_or_skip_disabled", screen)
            return
        }
        val ready = isSplashReady() || withTimeoutOrNull(loadTimeoutMs) { loadSplash(screen) } == true
        if (!ready) {
            adsLogW("show_or_skip_not_ready", screen)
            return
        }
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                var finished = false
                fun finish() {
                    if (finished) return
                    finished = true
                    if (cont.isActive) cont.resume(Unit)
                }
                showSplashWhenResumed(activity, screen) { result ->
                    adsLog("show_or_skip_result", screen, result.toString())
                    when (result) {
                        AdShowResult.Shown -> Unit
                        else -> finish()
                    }
                }
                cont.invokeOnCancellation { finished = true }
            }
        }
    }

    /**
     * Home bottom-tab / editor exit: [AdFormat.INTER_HOME] + home unit; shared cooldown.
     */
    suspend fun showBottomNavOrSkip(activity: Activity) {
        showHomeUnitOrSkip(activity, AdFormat.INTER_HOME, SCREEN_HOME)
    }

    // Unused in this app: INTER_TOOL is commented out in AdsRemoteConfigKeys.
    // Tools hub item click: INTER_TOOL RC gate, **same** homeUnitId + shared cooldown.
    // suspend fun showToolOrSkip(activity: Activity) {
    //     showHomeUnitOrSkip(activity, AdFormat.INTER_TOOL, SCREEN_TOOL)
    // }

    private suspend fun showHomeUnitOrSkip(
        activity: Activity,
        format: AdFormat,
        screen: String,
    ) {
        if (!eligibility.canShow(format)) {
            adsLogW("home_unit_skip", screen, "disabled")
            return
        }
        if (!isHomeCooldownElapsed()) {
            val remain =
                (homeReadyAtElapsedMs.get() - SystemClock.elapsedRealtime()).coerceAtLeast(0L)
            adsLog("home_timer_blocked", screen, "remainMs=$remain")
            return
        }
        if (format == AdFormat.INTER_HOME && consumeMissedSplashAd()) {
            adsLog("home_reuse_splash", screen, "missed splash ad")
            if (!homeShowClaimed.compareAndSet(false, true)) {
                adsLogW("home_unit_skip", screen, "show already in flight")
                return
            }
            try {
                showSplashAndWait(activity, screen)
            } finally {
                homeShowClaimed.set(false)
            }
            return
        }
        if (!isHomeReady()) {
            adsLogW("home_not_ready", screen)
            startHomePreload()
            return
        }
        if (!homeShowClaimed.compareAndSet(false, true)) {
            adsLogW("home_unit_skip", screen, "show already in flight")
            return
        }
        try {
        withContext(Dispatchers.Main) {
            suspendCancellableCoroutine { cont ->
                var finished = false
                fun finish() {
                    if (finished) return
                    finished = true
                    if (cont.isActive) cont.resume(Unit)
                }
                showHomeWhenResumed(activity, screen) { result ->
                    adsLog("home_unit_result", screen, result.toString())
                    when (result) {
                        AdShowResult.Shown, AdShowResult.Impression -> Unit
                        AdShowResult.Dismissed -> {
                            restartHomeCycle()
                            finish()
                        }
                        else -> {
                            startHomePreload()
                            finish()
                        }
                    }
                }
                cont.invokeOnCancellation { finished = true }
            }
        }
        } finally {
            homeShowClaimed.set(false)
        }
    }

    private fun present(
        ad: InterstitialAd,
        activity: Activity,
        screen: String,
        analyticsUnitId: String,
        destroyUnitId: String,
        callback: AdShowCallback?,
    ) {
        if (!presenting.compareAndSet(false, true)) {
            adsLogW("show_skip_busy", screen, "interstitial already showing")
            callback?.onResult(AdShowResult.Disabled)
            return
        }
        if (screen == SCREEN_SPLASH && splashCancelled.get()) {
            presenting.set(false)
            splashOnDemandAd = null
            adsLog("show_skip", screen, "inter splash force update")
            callback?.onResult(AdShowResult.Disabled)
            return
        }
        var released = false
        fun release() {
            if (released) return
            released = true
            presenting.set(false)
        }
        adsLog("show_start", screen, "interstitial unit=$analyticsUnitId")
        ad.adEventCallback = object : InterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                shownThisSession = true
                adsLog("show_success", screen, "interstitial")
                callback?.onResult(AdShowResult.Shown)
            }

            override fun onAdDismissedFullScreenContent() {
                adsLog("show_dismissed", screen, "interstitial")
                if (destroyUnitId == splashUnitId) splashOnDemandAd = null
                destroyPreloaderQuietly(destroyUnitId)
                appOpen.notifyFullscreenClosed()
                release()
                callback?.onResult(AdShowResult.Dismissed)
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                adsLogW("show_failed", screen, "interstitial ${error.message}")
                if (destroyUnitId == splashUnitId) splashOnDemandAd = null
                destroyPreloaderQuietly(destroyUnitId)
                appOpen.notifyFullscreenClosed()
                release()
                callback?.onResult(AdShowResult.Failed(error.message.orEmpty()))
            }

            override fun onAdImpression() {
                adsLog("impression", screen, "interstitial")
                firebase.logAdImpression("interstitial", analyticsUnitId)
            }

            override fun onAdClicked() {
                adsLog("click", screen, "interstitial")
                firebase.logAdClick("interstitial", analyticsUnitId)
            }
        }
        appOpen.notifyFullscreenShowing()
        val showAd: () -> Unit = {
            if (screen == SCREEN_SPLASH && splashCancelled.get()) {
                splashOnDemandAd = null
                appOpen.notifyFullscreenClosed()
                release()
                adsLog("show_skip", screen, "inter splash force update")
                callback?.onResult(AdShowResult.Disabled)
            } else {
                val started = gmaCall(false) {
                    ad.show(activity)
                    true
                }
                if (!started) {
                    release()
                    callback?.onResult(AdShowResult.Failed("show skipped"))
                }
            }
        }
        if (screen == SCREEN_SPLASH) {
            showAd()
        } else {
            AdShowingDialog.showThen(activity, showAd)
        }
    }

    /** Splash force-update: stop the in-flight load and never show that interstitial. */
    fun cancelSplashGate() {
        splashCancelled.set(true)
        splashMissed.set(false)
        splashOnDemandAd = null
        adsLog("splash_gate_cancel", SCREEN_SPLASH, "force update")
    }

    /** Splash moved on before this interstitial was ready. Keep the load for the first Home click. */
    fun markSplashMissed() {
        if (splashCancelled.get() || shownThisSession) return
        splashMissed.set(true)
        adsLog("splash_missed", SCREEN_SPLASH, "will reuse on first home click")
    }

    /**
     * Shows an interstitial that [loadSplash] already finished. Does not start another load.
     *
     * @param appearTimeoutMs how long to wait for the ad to actually reach the screen. Callers
     * on a deadline pass what is left of it, so a show that never materialises — the user
     * backgrounded the app as it was starting — cannot outlive their budget.
     */
    suspend fun showLoadedSplash(
        activity: Activity,
        appearTimeoutMs: Long = SPLASH_APPEAR_TIMEOUT_MS,
        onShown: () -> Unit = {},
    ) {
        val screen = SCREEN_SPLASH
        if (splashCancelled.get() || !isSplashReady()) {
            adsLogW("splash_show_skip", screen, "not ready or force update")
            return
        }
        val appeared = CompletableDeferred<Boolean>()
        val finished = CompletableDeferred<Unit>()
        withContext(Dispatchers.Main) {
            showSplashWhenResumed(activity, screen) { result ->
                adsLog("splash_gate_result", screen, result.toString())
                when (result) {
                    AdShowResult.Shown, AdShowResult.Impression -> {
                        onShown()
                        if (!appeared.isCompleted) appeared.complete(true)
                    }
                    AdShowResult.Dismissed -> {
                        if (!appeared.isCompleted) appeared.complete(false)
                        if (!finished.isCompleted) finished.complete(Unit)
                    }
                    else -> {
                        if (!appeared.isCompleted) appeared.complete(false)
                        if (!finished.isCompleted) finished.complete(Unit)
                    }
                }
            }
        }
        val didShow = withTimeoutOrNull(appearTimeoutMs) { appeared.await() } == true
        if (!didShow) {
            adsLogW("splash_show_skip", screen, "ad did not appear")
            return
        }
        finished.await()
    }

    fun destroy() {
        adsLog("destroy", detail = "interstitial splash+home")
        splashOnDemandAd = null
        splashLoading.set(false)
        shownThisSession = false
        homeSessionStarted.set(false)
        homeReadyAtElapsedMs.set(0L)
        presenting.set(false)
        homeShowClaimed.set(false)
        destroyPreloaderQuietly(splashUnitId)
        destroyPreloaderQuietly(homeUnitId)
    }

    private fun destroyPreloaderQuietly(unitId: String) {
        gmaCall(false) { InterstitialAdPreloader.destroy(unitId) }
    }

    companion object {
        const val SCREEN_SPLASH = "Splash"
        const val SCREEN_LANGUAGE = "Language"
        const val SCREEN_ONBOARDING = "Onboarding"
        const val SCREEN_HOME = "Home"
        const val SCREEN_TOOL = "Tool"
        /** @deprecated Alias of [SCREEN_HOME]. */
        const val SCREEN_BOTTOM_NAV = SCREEN_HOME
        const val SPLASH_AD_TIMEOUT_MS = 15_000L

        /** Default wait for a loaded splash ad to reach the screen once `show` is called. */
        const val SPLASH_APPEAR_TIMEOUT_MS = 8_000L
    }
}
