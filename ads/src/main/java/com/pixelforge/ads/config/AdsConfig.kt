package com.pixelforge.ads.config

import com.pixelforge.ads.remoteconfig.AdFormat

/**
 * Host-app provided AdMob IDs — **per screen / placement**.
 *
 * Debug / CI should return [GoogleTestAdsUnitIds] values until real units exist.
 * When you add a new ad, add a property here + BuildConfig field in `ads/build.gradle.kts`.
 */
interface AdsUnitIds {
    /** AdMob App ID (`ca-app-pub-xxxx~yyyy`). Also put this in the app manifest meta-data. */
    val applicationId: String

    val interstitialSplash: String
    val interstitialHome: String
    val appOpen: String
    val nativeReferral: String
    val nativeHome: String
    val nativeOnboarding: String
    val nativePdfTools: String
    val nativeTemplates: String
    val bannerExit: String

    /**
     * Optional extra banner / rewarded video. Empty = disabled (no load or preload).
     * Exit MREC uses [bannerExit]; 2h ads-free uses [rewardedInterstitial].
     */
    val banner: String
    val rewarded: String
    val rewardedInterstitial: String
}

/** Official Google sample / test units — safe for development (same test ID reused per format). */
object GoogleTestAdsUnitIds : AdsUnitIds {
    override val applicationId = "ca-app-pub-3940256099942544~3347511713"
    override val interstitialSplash = "ca-app-pub-3940256099942544/1033173712"
    override val interstitialHome = "ca-app-pub-3940256099942544/1033173712"
    override val appOpen = "ca-app-pub-3940256099942544/9257395921"
    override val nativeReferral = "ca-app-pub-3940256099942544/2247696110"
    override val nativeHome = "ca-app-pub-3940256099942544/2247696110"
    override val nativeOnboarding = "ca-app-pub-3940256099942544/2247696110"
    override val nativePdfTools = "ca-app-pub-3940256099942544/2247696110"
    override val nativeTemplates = "ca-app-pub-3940256099942544/2247696110"
    override val bannerExit = "ca-app-pub-3940256099942544/9214589741"
    override val banner = ""
    override val rewarded = ""
    override val rewardedInterstitial = "ca-app-pub-3940256099942544/5354046379"
}

data class AdsConfig(
    val unitIds: AdsUnitIds,
    /** When false, all load/show calls no-op (e.g. ads removed). */
    val adsEnabled: Boolean = true,
    /** Start Next-Gen preloaders after MobileAds.initialize. */
    val preloadFullscreenFormats: Boolean = true,
    /** App-open: skip the very first cold start (Google recommendation). */
    val showAppOpenOnFirstLaunch: Boolean = false,
    val fullscreenBufferSize: Int = 2,
)

/** False for blank, placeholder (`xxxx`), or non-AdMob IDs — never load/preload those. */
fun String.isConfiguredAdUnit(): Boolean {
    val id = trim()
    if (id.isEmpty() || "xxxx" in id) return false
    return id.startsWith("ca-app-pub-") && '/' in id
}

fun AdsUnitIds.unitIdFor(format: AdFormat): String =
    when (format) {
        AdFormat.INTER_SPLASH -> interstitialSplash
        AdFormat.INTER_HOME -> interstitialHome
        // AdFormat.INTER_TOOL -> interstitialHome
        AdFormat.APP_OPEN -> appOpen
        AdFormat.NATIVE_REFERRAL -> nativeReferral
        AdFormat.NATIVE_HOME -> nativeHome
        AdFormat.NATIVE_ONBOARDING -> nativeOnboarding
        AdFormat.NATIVE_PDF_TOOLS -> nativePdfTools
        AdFormat.NATIVE_TEMPLATES -> nativeTemplates
        // AdFormat.BANNER_EXIT -> bannerExit
        // AdFormat.BANNER -> banner
        // AdFormat.REWARDED -> rewarded
        AdFormat.REWARDED_INTERSTITIAL -> rewardedInterstitial
    }
