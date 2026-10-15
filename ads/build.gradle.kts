plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.pixelforge.ads"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        debug {
            // Google sample / test units — safe while developing.
            applyAdMobIds(
                appId = "ca-app-pub-3940256099942544~3347511713",
                interSplash = "ca-app-pub-3940256099942544/1033173712",
                interHome = "ca-app-pub-3940256099942544/1033173712",
                appOpen = "ca-app-pub-3940256099942544/9257395921",
                nativeReferral = "ca-app-pub-3940256099942544/2247696110",
                nativeHome = "ca-app-pub-3940256099942544/2247696110",
                nativeOnboarding = "ca-app-pub-3940256099942544/2247696110",
                nativePdfTools = "ca-app-pub-3940256099942544/2247696110",
                nativeTemplates = "ca-app-pub-3940256099942544/2247696110",
                bannerExit = "ca-app-pub-3940256099942544/9214589741",
                rewardedInterstitial = "ca-app-pub-3940256099942544/5354046379",
            )
        }
        release {
            // Units containing "xxxx" are treated as unconfigured and never load.
            applyAdMobIds(
                appId = "ca-app-pub-7852273515717628~5739275704",
                interSplash = "ca-app-pub-7852273515717628/8414374653",
                interHome = "ca-app-pub-7852273515717628/3058106199",
                appOpen = "ca-app-pub-7852273515717628/9951562464",
                nativeReferral = "ca-app-pub-7852273515717628/5596639621",
                nativeHome = "ca-app-pub-7852273515717628/7349449857",
                nativeOnboarding = "ca-app-pub-7852273515717628/7892375331",
                nativePdfTools = "ca-app-pub-7852273515717628/3308378474",
                nativeTemplates = "ca-app-pub-7852273515717628/9240371163",
                bannerExit = "ca-app-pub-xxxxxxxxxxxxxxxx/banner_exit",
                rewardedInterstitial = "ca-app-pub-7852273515717628/8369133463",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

/** Debug vs release AdMob App ID + per-screen unit IDs → BuildConfig + manifest placeholder. */
fun com.android.build.api.dsl.LibraryBuildType.applyAdMobIds(
    appId: String,
    interSplash: String,
    interHome: String,
    appOpen: String,
    nativeReferral: String,
    nativeHome: String,
    nativeOnboarding: String,
    nativePdfTools: String,
    nativeTemplates: String,
    bannerExit: String,
    banner: String = "",
    rewarded: String = "",
    rewardedInterstitial: String,
) {
    manifestPlaceholders["admobAppId"] = appId
    buildConfigField("String", "ADMOB_APP_ID", "\"$appId\"")
    buildConfigField("String", "ADMOB_INTER_SPLASH", "\"$interSplash\"")
    buildConfigField("String", "ADMOB_INTER_HOME", "\"$interHome\"")
    buildConfigField("String", "ADMOB_APP_OPEN", "\"$appOpen\"")
    buildConfigField("String", "ADMOB_NATIVE_REFERRAL", "\"$nativeReferral\"")
    buildConfigField("String", "ADMOB_NATIVE_HOME", "\"$nativeHome\"")
    buildConfigField("String", "ADMOB_NATIVE_ONBOARDING", "\"$nativeOnboarding\"")
    buildConfigField("String", "ADMOB_NATIVE_PDF_TOOLS", "\"$nativePdfTools\"")
    buildConfigField("String", "ADMOB_NATIVE_TEMPLATES", "\"$nativeTemplates\"")
    buildConfigField("String", "ADMOB_BANNER_EXIT", "\"$bannerExit\"")
    buildConfigField("String", "ADMOB_BANNER", "\"$banner\"")
    buildConfigField("String", "ADMOB_REWARDED", "\"$rewarded\"")
    buildConfigField("String", "ADMOB_REWARDED_INTERSTITIAL", "\"$rewardedInterstitial\"")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.kotlinx.coroutines.android)
    implementation(platform(libs.koin.bom))
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)

    // GMA Next-Gen SDK (do not also depend on play-services-ads).
    api(libs.ads.mobile.sdk)
    // Meta Audience Network bidding adapter. Brings Audience Network SDK 6.22.0.
    api(libs.ads.mediation.facebook)
    // Google consent form (UMP). Separate from GMA Next-Gen.
    api(libs.user.messaging.platform)

    // Firebase suite used by the reusable ads module.
    api(platform(libs.firebase.bom))
    api(libs.firebase.analytics)
    api(libs.firebase.crashlytics)
    api(libs.firebase.messaging)
    api(libs.firebase.perf)
    api(libs.firebase.config)
    implementation(libs.kotlinx.coroutines.play.services)
}

// Meta's adapter still declares the legacy GMA SDK. Next-Gen must be the only ads SDK.
configurations.configureEach {
    exclude(group = "com.google.android.gms", module = "play-services-ads")
    exclude(group = "com.google.android.gms", module = "play-services-ads-lite")
}
