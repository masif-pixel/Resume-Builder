package com.pixelforge.ads.remoteconfig

import com.pixelforge.ads.adfree.AdFreeSession
import com.pixelforge.ads.config.AdsConfig
import com.pixelforge.ads.config.isConfiguredAdUnit
import com.pixelforge.ads.config.unitIdFor

/**
 * Single place to decide whether an ad format may load/show.
 * Combines local [AdsConfig.adsEnabled], Firebase Remote Config, and temporary ads-free.
 */
class AdsEligibility(
    private val config: AdsConfig,
    private val remoteConfig: AdsRemoteConfig,
    private val adFreeSession: AdFreeSession,
) {
    fun canShow(format: AdFormat): Boolean {
        if (!config.adsEnabled) return false
        if (!config.unitIds.unitIdFor(format).isConfiguredAdUnit()) return false
        if (!remoteConfig.isAdEnabled(format)) return false
        // Rewarded formats stay available so the user can earn another ads-free window.
        if (adFreeSession.isActive() && format.isBlockedDuringAdFree) return false
        return true
    }
}

private val AdFormat.isBlockedDuringAdFree: Boolean
    get() = when (this) {
        // AdFormat.REWARDED,
        AdFormat.REWARDED_INTERSTITIAL -> false
        else -> true
    }
