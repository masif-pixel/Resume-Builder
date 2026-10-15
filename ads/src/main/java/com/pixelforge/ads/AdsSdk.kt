package com.pixelforge.ads

import android.app.Application
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.pixelforge.ads.adfree.AdFreeSession
import com.pixelforge.ads.appopen.AppOpenAdManager
import com.pixelforge.ads.banner.RetainedBannerStore
import com.pixelforge.ads.common.GmaRuntime
import com.pixelforge.ads.common.adsLog
import com.pixelforge.ads.common.adsLogE
import com.pixelforge.ads.common.adsLogW
import com.pixelforge.ads.config.AdsConfig
import com.pixelforge.ads.firebase.FirebaseBootstrap
import com.pixelforge.ads.interstitial.InterstitialAdManager
import com.pixelforge.ads.nativead.RetainedNativeStore
import com.pixelforge.ads.remoteconfig.AdFormat
import com.pixelforge.ads.remoteconfig.AdsEligibility
import com.pixelforge.ads.remoteconfig.AdsRemoteConfig
import com.pixelforge.ads.rewarded.RewardedAdManager
import com.pixelforge.ads.rewarded.RewardedInterstitialAdManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Single entry-point for AdMob Next-Gen + Firebase init.
 * Call [initialize] once from `Application.onCreate`.
 *
 * [initialized] becomes true after bootstrap finishes (or ads are skipped).
 * [mobileAdsReady] is true only after [MobileAds.initialize] succeeds — never call
 * GMA preloaders/loaders until then.
 */
class AdsSdk(
    private val application: Application,
    private val config: AdsConfig,
    private val firebase: FirebaseBootstrap,
    private val remoteConfig: AdsRemoteConfig,
    private val eligibility: AdsEligibility,
    private val retainedBanners: RetainedBannerStore,
    private val retainedNatives: RetainedNativeStore,
    val adFreeSession: AdFreeSession,
    val interstitial: InterstitialAdManager,
    val rewarded: RewardedAdManager,
    val rewardedInterstitial: RewardedInterstitialAdManager,
    val appOpen: AppOpenAdManager,
) {
    private var job = SupervisorJob()
    private var scope = CoroutineScope(job + Dispatchers.IO)
    private val bootstrapStarted = AtomicBoolean(false)
    private val _initialized = MutableStateFlow(false)
    val initialized: StateFlow<Boolean> = _initialized.asStateFlow()
    private val _mobileAdsReady = MutableStateFlow(false)
    val mobileAdsReady: StateFlow<Boolean> = _mobileAdsReady.asStateFlow()

    /**
     * True once this launch's Remote Config fetch has answered: fresh values activated, or the
     * fetch failed and the last activated values stay. Splash waits on it so a console change
     * applies to the first session, not the next one.
     */
    val remoteConfigReady: StateFlow<Boolean> get() = remoteConfig.ready

    /**
     * Call once from `Application.onCreate` (background is fine).
     * Splash may request an interstitial only after Remote Config has answered.
     * Mobile Ads init runs beside that fetch so the ad call is not stuck behind SDK startup.
     */
    fun initialize(onComplete: (() -> Unit)? = null) {
        if (!bootstrapStarted.compareAndSet(false, true)) {
            scope.launch {
                initialized.first { it }
                onComplete?.invoke()
            }
            return
        }
        scope.launch {
            bootstrap(onComplete)
        }
    }

    /**
     * Two signals, deliberately separate:
     *
     * [mobileAdsReady] flips as soon as GMA init succeeds, so splash can *request* an ad beside
     * the Remote Config fetch rather than behind it — cached RC values are enough to decide
     * whether a request is worth making.
     *
     * [initialized] flips only once the Remote Config fetch has answered. That is the gate splash
     * uses before *showing* anything, which is what keeps a freshly flipped RC key authoritative.
     */
    private suspend fun bootstrap(onComplete: (() -> Unit)?) {
        if (!config.adsEnabled) {
            scope.launch { fetchRemoteConfig() }
            finishBootstrap(mobileAdsReady = false, onComplete)
            return
        }

        // Started first: during startup the main thread is busy with Koin, the Activity and the
        // first Compose frame, and GMA init is the one thing the splash ad load genuinely waits on.
        val warm = scope.async { warmMobileAds() }
        // Remote Config's fetch has no timeout of its own worth relying on — it waits ~60s when
        // the device is associated to a network that has no route out, and splash blocks on
        // `initialized` behind it. Launched on the SDK scope rather than as a child of the
        // timeout: when the gate expires we stop *waiting* on it, but it keeps running, so the
        // fresh RC values still land for every screen after splash.
        val remoteConfigJob = scope.async { fetchRemoteConfig() }
        val rcGated = withTimeoutOrNull(AdsRemoteConfig.FETCH_TIMEOUT_MS) { remoteConfigJob.await() } != null
        if (!rcGated) {
            adsLogW("bootstrap_gate_timeout", detail = "rc exceeded ${AdsRemoteConfig.FETCH_TIMEOUT_MS}ms")
        }

        if (shouldSkipAds()) {
            warm.cancel()
            interstitial.cancelSplashGate()
            adsLog("sdk_skip", detail = "placements off via Remote Config")
            finishBootstrap(mobileAdsReady = false, onComplete)
            return
        }

        val ready = warm.await()
        // App-open preload deliberately not started here: it would put a second ad request on
        // the wire next to the splash interstitial on a cold, slow connection. Home calls
        // ensurePreloaded() once it is live, which is the first moment it can show.
        withContext(Dispatchers.Main) {
            setMobileAdsReady(ready)
            if (ready && eligibility.canShow(AdFormat.APP_OPEN)) appOpen.registerLifecycle()
            _initialized.value = true
            adsLog("sdk_initialized", detail = "mobileAdsReady=$ready reason=bootstrap")
            onComplete?.invoke()
        }
    }

    private suspend fun fetchRemoteConfig() {
        runCatching { firebase.ensureInitialized() }
            .onFailure { adsLogE("firebase_init_failed", detail = it.message, error = it) }
        remoteConfig.awaitDefaults()
        remoteConfig.fetchAndActivate()
    }

    /**
     * Initializes GMA and publishes [mobileAdsReady] the moment it succeeds — not at the end of
     * bootstrap. Splash gates its ad *load* on that flag so the request goes out beside the
     * Remote Config fetch instead of behind it; the *show* still waits for [initialized].
     */
    private suspend fun warmMobileAds(): Boolean {
        adsLog("mobile_ads_start", detail = "parallel_with_rc")
        val ready = withTimeoutOrNull(MOBILE_ADS_INIT_TIMEOUT_MS) {
            awaitMobileAdsInitialized()
        } == true
        if (!ready) {
            adsLogE("mobile_ads_init_not_ready", detail = "timeout or failure — ad loads will no-op")
            return false
        }
        // Published straight from this thread. Both writes behind it are a @Volatile flag and a
        // StateFlow, so neither needs the main thread.
        setMobileAdsReady(true)
        adsLog("mobile_ads_ready_published", detail = "splash may load now")
        return true
    }

    private fun shouldSkipAds(): Boolean =
        AdFormat.entries.none { eligibility.canShow(it) }

    private suspend fun finishBootstrap(mobileAdsReady: Boolean, onComplete: (() -> Unit)?) {
        withContext(Dispatchers.Main) {
            setMobileAdsReady(mobileAdsReady)
            _initialized.value = true
            onComplete?.invoke()
        }
    }

    /**
     * Home / main chrome: wait until MobileAds is ready, then preload interstitial + start cooldown gate.
     * Safe to call before [initialize] finishes — will not crash.
     */
    fun prepareBottomNavWhenReady() {
        scope.launch {
            initialized.first { it }
            if (!_mobileAdsReady.value) {
                adsLogW("bottom_nav_prepare_skip", detail = "MobileAds not ready")
                return@launch
            }
            interstitial.prepareBottomNav()
        }
    }

    /**
     * Suspends until GMA Next-Gen reports adapters ready (or throws / resumes false).
     */
    private suspend fun awaitMobileAdsInitialized(): Boolean =
        suspendCancellableCoroutine { cont ->
            try {
                MobileAds.initialize(
                    application,
                    InitializationConfig.Builder(config.unitIds.applicationId).build()
                ) { status ->
                    adsLog("mobile_ads_ready", detail = "$status")
                    if (cont.isActive) cont.resume(true)
                }
            } catch (t: Throwable) {
                adsLogE("mobile_ads_init_failed", detail = t.message, error = t)
                if (cont.isActive) cont.resume(false)
            }
        }

    /** Re-check after a later RC refresh from the host app. */
    fun canShow(format: AdFormat): Boolean = eligibility.canShow(format)

    /** Sync persisted ads-free expiry into the in-memory gate (call from app storage). */
    fun syncAdFreeUntil(untilEpochMs: Long) {
        adFreeSession.sync(untilEpochMs)
        if (adFreeSession.isActive()) {
            clearMonetizationCaches("ad_free_sync")
        } else {
            appOpen.setSuppressed(false, AppOpenAdManager.REASON_ADS_FREE)
        }
    }

    /**
     * Drop already-loaded monetized ads so UI slots disappear immediately after reward.
     * Does not touch rewarded formats used to earn ads-free time.
     */
    fun clearMonetizationCaches(reason: String = "manual") {
        adsLog("clear_monetization", detail = reason)
        interstitial.destroy()
        retainedBanners.destroyAll()
        retainedNatives.destroyAll()
        appOpen.setSuppressed(true, AppOpenAdManager.REASON_ADS_FREE)
    }

    /** Tear down every cached/preloaded fullscreen format (call on app exit). */
    fun destroyAll() {
        adsLog("sdk_destroy_all", detail = "mobileAdsReady=${_mobileAdsReady.value}")
        interstitial.destroy()
        rewarded.destroy()
        rewardedInterstitial.destroy()
        appOpen.destroy()
        retainedBanners.destroyAll()
        retainedNatives.destroyAll()
        // Stop any in-flight init/preload work; process kill follows on Exit.
        job.cancel()
        job = SupervisorJob()
        scope = CoroutineScope(job + Dispatchers.Default)
        setMobileAdsReady(false)
        _initialized.value = false
    }

    private fun setMobileAdsReady(ready: Boolean) {
        _mobileAdsReady.value = ready
        GmaRuntime.setReady(ready)
    }

    /** Remote Config `ads_master` — 0 hides every placement. */
    fun isAdsMasterOn(): Boolean = remoteConfig.isMasterEnabled()

    /**
     * User tapped Exit: wipe every ad cache, remove the task, and kill the process so the
     * next launch cannot reuse in-memory AdMob / Koin state.
     */
    fun shutdownForExit(activity: android.app.Activity) {
        adsLog("sdk_shutdown_for_exit")
        destroyAll()
        activity.finishAndRemoveTask()
        android.os.Process.killProcess(android.os.Process.myPid())
        kotlin.system.exitProcess(0)
    }

    companion object {
        // Sized so a cold start on a dead connection still clears bootstrap inside the splash
        // budget (SPLASH_MAX_MILLIS). GMA init runs in parallel with the Remote Config fetch
        // (AdsRemoteConfig.FETCH_TIMEOUT_MS), so the two do not add up.
        private const val MOBILE_ADS_INIT_TIMEOUT_MS = 10_000L
    }
}
