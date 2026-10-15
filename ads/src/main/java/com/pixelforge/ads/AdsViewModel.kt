package com.pixelforge.ads

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelforge.ads.common.AdShowCallback
import com.pixelforge.ads.common.AdShowResult
import com.pixelforge.ads.remoteconfig.AdFormat
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

/**
 * MVVM-friendly facade over [AdsSdk] full-screen formats.
 */
class AdsViewModel(
    private val adsSdk: AdsSdk,
) : ViewModel() {

    val initialized: StateFlow<Boolean> = adsSdk.initialized
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun canShow(format: AdFormat): Boolean = adsSdk.canShow(format)

    fun setAppOpenSuppressed(
        suppressed: Boolean,
        reason: String = com.pixelforge.ads.appopen.AppOpenAdManager.REASON_SPLASH,
    ) {
        adsSdk.appOpen.setSuppressed(suppressed, reason)
    }

    fun showInterstitial(
        activity: Activity,
        screen: String = "Unknown",
        onResult: (AdShowResult) -> Unit = {},
    ) {
        adsSdk.interstitial.show(activity, screen, AdShowCallback(onResult))
    }

    fun showRewarded(activity: Activity, onResult: (AdShowResult) -> Unit = {}) {
        adsSdk.rewarded.show(activity, AdShowCallback(onResult))
    }

    fun showRewardedInterstitial(activity: Activity, onResult: (AdShowResult) -> Unit = {}) {
        adsSdk.rewardedInterstitial.show(activity, AdShowCallback(onResult))
    }

    fun showAppOpen(activity: Activity, onResult: (AdShowResult) -> Unit = {}) {
        adsSdk.appOpen.show(activity, AdShowCallback(onResult))
    }

    fun isInterstitialReady() = adsSdk.interstitial.isReady()
    fun isRewardedReady() = adsSdk.rewarded.isReady()
}
