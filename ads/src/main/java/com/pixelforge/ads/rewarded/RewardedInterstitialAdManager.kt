package com.pixelforge.ads.rewarded

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAd
import com.google.android.libraries.ads.mobile.sdk.rewardedinterstitial.RewardedInterstitialAdEventCallback
import com.pixelforge.ads.appopen.AppOpenAdManager
import com.pixelforge.ads.common.AdShowCallback
import com.pixelforge.ads.common.AdShowResult
import com.pixelforge.ads.common.GmaRuntime
import com.pixelforge.ads.common.adsLog
import com.pixelforge.ads.common.adsLogE
import com.pixelforge.ads.common.adsLogW
import com.pixelforge.ads.common.gmaCall
import com.pixelforge.ads.config.AdsConfig
import com.pixelforge.ads.firebase.FirebaseBootstrap
import com.pixelforge.ads.remoteconfig.AdFormat
import com.pixelforge.ads.remoteconfig.AdsEligibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Rewarded interstitial — **on-demand only** (Watch click). Never preloads.
 */
class RewardedInterstitialAdManager(
    private val config: AdsConfig,
    private val firebase: FirebaseBootstrap,
    private val eligibility: AdsEligibility,
    private val appOpen: AppOpenAdManager,
) {
    private val unitId get() = config.unitIds.rewardedInterstitial
    private val loading = AtomicBoolean(false)

    /** No-op — rewarded interstitial must not be preloaded. */
    fun startPreload() {
        adsLog("preload_skip", SCREEN, "on-demand only")
    }

    fun isReady(): Boolean = false

    fun show(activity: Activity, callback: AdShowCallback? = null) {
        adsLogW("show_skip", SCREEN, "use loadAndShow")
        callback?.onResult(AdShowResult.NotReady)
    }

    /**
     * Load on the calling coroutine, then show. Call from Watch click while UI shows progress.
     * Emits [AdShowResult.Rewarded] when the user earns the reward.
     */
    suspend fun loadAndShow(activity: Activity, callback: AdShowCallback? = null) {
        if (!eligibility.canShow(AdFormat.REWARDED_INTERSTITIAL)) {
            adsLogW("load_show_skip", SCREEN, "disabled")
            callback?.onResult(AdShowResult.Disabled)
            return
        }
        if (!GmaRuntime.isReady()) {
            adsLogW("load_show_skip", SCREEN, "MobileAds not initialized")
            callback?.onResult(AdShowResult.Disabled)
            return
        }
        if (!loading.compareAndSet(false, true)) {
            adsLogW("load_show_busy", SCREEN)
            callback?.onResult(AdShowResult.NotReady)
            return
        }
        adsLog("load_start", SCREEN, "unit=$unitId")
        val loaded = try {
            withContext(Dispatchers.Main) {
                RewardedInterstitialAd.load(AdRequest.Builder(unitId).build())
            }
        } catch (t: Throwable) {
            adsLogE("load_exception", SCREEN, t.message, t)
            null
        } finally {
            loading.set(false)
        }
        when (loaded) {
            is AdLoadResult.Success -> {
                adsLog("load_success", SCREEN)
                withContext(Dispatchers.Main) { present(loaded.ad, activity, callback) }
            }
            is AdLoadResult.Failure -> {
                adsLogW("load_failed", SCREEN, "${loaded.error}")
                callback?.onResult(AdShowResult.Failed(loaded.error.toString()))
            }
            null -> callback?.onResult(AdShowResult.Failed("load exception"))
        }
    }

    private fun present(
        ad: RewardedInterstitialAd,
        activity: Activity,
        callback: AdShowCallback?,
    ) {
        adsLog("show_start", SCREEN)
        ad.adEventCallback = object : RewardedInterstitialAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                adsLog("show_success", SCREEN)
                callback?.onResult(AdShowResult.Shown)
            }

            override fun onAdDismissedFullScreenContent() {
                adsLog("show_dismissed", SCREEN)
                appOpen.notifyFullscreenClosed()
                callback?.onResult(AdShowResult.Dismissed)
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                adsLogW("show_failed", SCREEN, error.message)
                appOpen.notifyFullscreenClosed()
                callback?.onResult(AdShowResult.Failed(error.message.orEmpty()))
            }

            override fun onAdImpression() {
                adsLog("impression", SCREEN)
                firebase.logAdImpression("rewarded_interstitial", unitId)
                callback?.onResult(AdShowResult.Impression)
            }

            override fun onAdClicked() {
                firebase.logAdClick("rewarded_interstitial", unitId)
            }
        }
        appOpen.notifyFullscreenShowing()
        gmaCall(Unit) {
            ad.show(activity) { reward ->
                adsLog("reward_earned", SCREEN, "type=${reward.type} amount=${reward.amount}")
                callback?.onResult(AdShowResult.Rewarded(reward.type.orEmpty(), reward.amount))
            }
        }
    }

    fun destroy() {
        loading.set(false)
    }

    companion object {
        private const val SCREEN = "RewardedInterstitial"
    }
}
