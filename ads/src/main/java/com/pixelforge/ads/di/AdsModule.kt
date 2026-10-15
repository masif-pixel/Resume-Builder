package com.pixelforge.ads.di

import com.pixelforge.ads.AdsSdk
import com.pixelforge.ads.AdsViewModel
import com.pixelforge.ads.adfree.AdFreeSession
import com.pixelforge.ads.appopen.AppOpenAdManager
import com.pixelforge.ads.config.AdsConfig
import com.pixelforge.ads.config.AdsUnitIds
import com.pixelforge.ads.config.BuildConfigAdsUnitIds
import com.pixelforge.ads.firebase.FirebaseBootstrap
import com.pixelforge.ads.interstitial.InterstitialAdManager
import com.pixelforge.ads.remoteconfig.AdsEligibility
import com.pixelforge.ads.remoteconfig.AdsRemoteConfig
import com.pixelforge.ads.rewarded.RewardedAdManager
import com.pixelforge.ads.rewarded.RewardedInterstitialAdManager
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Reusable Koin module. Ad unit IDs come from `:ads` BuildConfig by default.
 * Remote Config Int flags (0/1) gate every format via [AdsEligibility].
 */
fun adsModule(
    unitIds: AdsUnitIds = BuildConfigAdsUnitIds,
    adsEnabled: Boolean = true,
) = module {
    single<AdsUnitIds> { unitIds }
    single {
        AdsConfig(
            unitIds = get(),
            adsEnabled = adsEnabled,
            preloadFullscreenFormats = true,
            showAppOpenOnFirstLaunch = false,
        )
    }
    single { FirebaseBootstrap(androidContext()) }
    single { AdsRemoteConfig() }
    single { AdFreeSession() }
    single { AdsEligibility(get(), get(), get()) }
    single { com.pixelforge.ads.banner.RetainedBannerStore() }
    single { com.pixelforge.ads.nativead.RetainedNativeStore() }
    single { AppOpenAdManager(androidApplication(), get(), get(), get()) }
    single { InterstitialAdManager(get(), get(), get(), get(), get()) }
    single { RewardedAdManager(get(), get(), get()) }
    single { RewardedInterstitialAdManager(get(), get(), get(), get()) }
    single {
        AdsSdk(
            application = androidApplication(),
            config = get(),
            firebase = get(),
            remoteConfig = get(),
            eligibility = get(),
            retainedBanners = get(),
            retainedNatives = get(),
            adFreeSession = get(),
            interstitial = get(),
            rewarded = get(),
            rewardedInterstitial = get(),
            appOpen = get(),
        )
    }
    viewModel { AdsViewModel(get()) }
}
