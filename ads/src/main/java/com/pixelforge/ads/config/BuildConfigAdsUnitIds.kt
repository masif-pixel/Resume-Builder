package com.pixelforge.ads.config

import com.pixelforge.ads.BuildConfig

/**
 * AdMob IDs from `:ads` BuildConfig (`ads/build.gradle.kts` debug vs release).
 */
object BuildConfigAdsUnitIds : AdsUnitIds {
    override val applicationId: String = BuildConfig.ADMOB_APP_ID
    override val interstitialSplash: String = BuildConfig.ADMOB_INTER_SPLASH
    override val interstitialHome: String = BuildConfig.ADMOB_INTER_HOME
    override val appOpen: String = BuildConfig.ADMOB_APP_OPEN
    override val nativeReferral: String = BuildConfig.ADMOB_NATIVE_REFERRAL
    override val nativeHome: String = BuildConfig.ADMOB_NATIVE_HOME
    override val nativeOnboarding: String = BuildConfig.ADMOB_NATIVE_ONBOARDING
    override val nativePdfTools: String = BuildConfig.ADMOB_NATIVE_PDF_TOOLS
    override val nativeTemplates: String = BuildConfig.ADMOB_NATIVE_TEMPLATES
    override val bannerExit: String = BuildConfig.ADMOB_BANNER_EXIT
    override val banner: String = BuildConfig.ADMOB_BANNER
    override val rewarded: String = BuildConfig.ADMOB_REWARDED
    override val rewardedInterstitial: String = BuildConfig.ADMOB_REWARDED_INTERSTITIAL
}
