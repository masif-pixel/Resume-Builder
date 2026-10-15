package com.pixelforge.ads.rewarded

import android.app.Activity
import com.google.android.libraries.ads.mobile.sdk.common.AdRequest
import com.google.android.libraries.ads.mobile.sdk.common.FullScreenContentError
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.common.PreloadCallback
import com.google.android.libraries.ads.mobile.sdk.common.PreloadConfiguration
import com.google.android.libraries.ads.mobile.sdk.common.ResponseInfo
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAd
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdEventCallback
import com.google.android.libraries.ads.mobile.sdk.rewarded.RewardedAdPreloader
import com.pixelforge.ads.common.AdShowCallback
import com.pixelforge.ads.common.AdShowResult
import com.pixelforge.ads.common.GmaRuntime
import com.pixelforge.ads.common.adsLog
import com.pixelforge.ads.common.adsLogW
import com.pixelforge.ads.common.gmaCall
import com.pixelforge.ads.config.AdsConfig
import com.pixelforge.ads.firebase.FirebaseBootstrap
import com.pixelforge.ads.remoteconfig.AdFormat
import com.pixelforge.ads.remoteconfig.AdsEligibility

/** Opt-in rewarded video (user taps “Watch ad”). */
class RewardedAdManager(
    private val config: AdsConfig,
    private val firebase: FirebaseBootstrap,
    private val eligibility: AdsEligibility,
) {
    private val unitId get() = config.unitIds.rewarded

    /** RC key `ads_rewarded` is commented out in AdsRemoteConfigKeys (unused here), so this stays off. */
    private val enabled = false

    fun startPreload() {
        if (unitId.isBlank() || !enabled /* eligibility.canShow(AdFormat.REWARDED) */ || !GmaRuntime.isReady()) return
        gmaCall(false) {
            val request = AdRequest.Builder(unitId).build()
            val preload = PreloadConfiguration(request, bufferSize = config.fullscreenBufferSize)
            RewardedAdPreloader.start(
                unitId,
                preload,
                object : PreloadCallback {
                    override fun onAdPreloaded(preloadId: String, responseInfo: ResponseInfo) {
                        adsLog("preload_success", screen = "Rewarded")
                    }

                    override fun onAdFailedToPreload(preloadId: String, adError: LoadAdError) {
                        adsLogW("preload_failed", screen = "Rewarded", detail = adError.message)
                    }

                    override fun onAdsExhausted(preloadId: String) {
                        adsLogW("preload_exhausted", screen = "Rewarded")
                    }
                }
            )
        }
    }

    fun isReady(): Boolean =
        unitId.isNotBlank() &&
            enabled && // eligibility.canShow(AdFormat.REWARDED)
            gmaCall(false) { RewardedAdPreloader.isAdAvailable(unitId) }

    fun show(activity: Activity, callback: AdShowCallback? = null) {
        if (unitId.isBlank() || !enabled /* eligibility.canShow(AdFormat.REWARDED) */) {
            callback?.onResult(AdShowResult.Disabled)
            return
        }
        val ad = gmaCall(null) { RewardedAdPreloader.pollAd(unitId) } ?: run {
            callback?.onResult(AdShowResult.NotReady)
            return
        }
        present(ad, activity, callback)
    }

    private fun present(ad: RewardedAd, activity: Activity, callback: AdShowCallback?) {
        ad.adEventCallback = object : RewardedAdEventCallback {
            override fun onAdShowedFullScreenContent() {
                callback?.onResult(AdShowResult.Shown)
            }

            override fun onAdDismissedFullScreenContent() {
                callback?.onResult(AdShowResult.Dismissed)
            }

            override fun onAdFailedToShowFullScreenContent(error: FullScreenContentError) {
                callback?.onResult(AdShowResult.Failed(error.message.orEmpty()))
            }

            override fun onAdImpression() {
                firebase.logAdImpression("rewarded", unitId)
            }

            override fun onAdClicked() {
                firebase.logAdClick("rewarded", unitId)
            }
        }
        gmaCall(Unit) {
            ad.show(activity) { reward ->
                callback?.onResult(AdShowResult.Rewarded(reward.type.orEmpty(), reward.amount))
            }
        }
    }

    fun destroy() {
        if (unitId.isBlank()) return
        gmaCall(false) { RewardedAdPreloader.destroy(unitId) }
    }
}
