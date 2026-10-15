package com.pixelforge.ads.nativead

import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.ads.mobile.sdk.common.LoadAdError
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoader
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdLoaderCallback
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdRequest
import com.pixelforge.ads.AdsSdk
import com.pixelforge.ads.common.adsLog
import com.pixelforge.ads.common.adsLogE
import com.pixelforge.ads.common.adsLogW
import com.pixelforge.ads.common.gmaCall
import com.pixelforge.ads.config.AdsConfig
import com.pixelforge.ads.config.unitIdFor
import com.pixelforge.ads.remoteconfig.AdFormat
import com.pixelforge.ads.remoteconfig.AdsEligibility
import com.pixelforge.ads.remoteconfig.AdsRemoteConfig
import com.pixelforge.ads.remoteconfig.isNative
import org.koin.compose.koinInject

private enum class NativeLoadState {
    WaitingSdk,
    Loading,
    Loaded,
    Failed,
}

/**
 * Loads and renders a native ad with [NativeAdTemplate] layouts.
 * Waits for [AdsSdk.initialized] before calling GMA (avoids first-launch crash on Language).
 * [placement] must be a native format ([AdFormat.isNative]); it picks the RC key + unit ID.
 *
 * @param retain When true, the loaded ad is kept in [RetainedNativeStore] across Compose
 *   disposal, so leaving the screen and coming back shows the same ad without a second load.
 *   Cleared from [AdsSdk.destroyAll] / exit.
 */
@Composable
fun NativeAdSlot(
    placement: AdFormat,
    template: NativeAdTemplate = NativeAdTemplate.MEDIUM,
    screen: String = placement.name,
    adUnitId: String? = null,
    retain: Boolean = false,
    modifier: Modifier = Modifier,
    showShimmer: Boolean = true,
    config: AdsConfig = koinInject(),
    eligibility: AdsEligibility = koinInject(),
    adsSdk: AdsSdk = koinInject(),
    remoteConfig: AdsRemoteConfig = koinInject(),
    retainedStore: RetainedNativeStore = koinInject(),
) {
    require(placement.isNative) { "NativeAdSlot placement must be a native format, was $placement" }
    val adFreeUntil by adsSdk.adFreeSession.untilEpochMs.collectAsStateWithLifecycle()
    // Read before the eligibility gate below, not after it. `canShow` is a plain call, not
    // state: a slot composed on cached RC values (e.g. Home, composed behind the splash) must
    // still be recomposed when this launch's fetch lands, or a placement switched back on in
    // the console only appears next session.
    // `.value` here, not `by`: a delegate is only read where it is used, which is past the return.
    val remoteConfigReady = remoteConfig.ready.collectAsStateWithLifecycle().value
    val sdkReady = adsSdk.initialized.collectAsStateWithLifecycle().value
    if (System.currentTimeMillis() < adFreeUntil) return
    if (!eligibility.canShow(placement)) return
    val inspection = LocalInspectionMode.current
    val unitId = adUnitId ?: config.unitIds.unitIdFor(placement)
    val retainKey = "${placement.name}:$unitId:${template.name}"
    val gmaReady by adsSdk.mobileAdsReady.collectAsStateWithLifecycle()
    val ctaColor = remember(remoteConfigReady) { remoteConfig.nativeCtaColor() }
    var nativeAd by remember(retainKey) {
        mutableStateOf(if (retain) retainedStore.get(retainKey) else null)
    }
    var loadState by remember(retainKey) {
        mutableStateOf(if (nativeAd != null) NativeLoadState.Loaded else NativeLoadState.WaitingSdk)
    }

    /** A retained ad outlives this slot; any other ad dies with it. */
    fun release() {
        if (!retain) nativeAd?.let { gmaCall(Unit) { it.destroy() } }
        nativeAd = null
    }

    DisposableEffect(placement, template, unitId, screen, sdkReady, gmaReady) {
        val retained = if (retain) retainedStore.get(retainKey) else null
        if (inspection) {
            loadState = NativeLoadState.Loading
            onDispose { }
        } else if (retained != null) {
            nativeAd = retained
            loadState = NativeLoadState.Loaded
            adsLog("load_retain_hit", screen, "native template=$template")
            onDispose { release() }
        } else if (!sdkReady) {
            loadState = NativeLoadState.WaitingSdk
            adsLog("load_wait_sdk", screen, "native")
            onDispose { release() }
        } else if (!gmaReady) {
            loadState = NativeLoadState.Failed
            adsLogW("load_skip", screen, "MobileAds not initialized")
            onDispose { release() }
        } else {
            var disposed = false
            loadState = NativeLoadState.Loading
            adsLog("load_start", screen, "native template=$template unit=$unitId retain=$retain")
            try {
                val request = NativeAdRequest.Builder(
                    unitId,
                    listOf(NativeAd.NativeAdType.NATIVE)
                ).build()
                NativeAdLoader.load(
                    request,
                    object : NativeAdLoaderCallback {
                        override fun onNativeAdLoaded(ad: NativeAd) {
                            adsLog("load_success", screen, "native template=$template")
                            if (retain) {
                                // Kept even when the slot already left: the next visit shows it.
                                retainedStore.put(retainKey, ad)
                            } else if (disposed) {
                                gmaCall(Unit) { ad.destroy() }
                                return
                            } else {
                                nativeAd?.let { gmaCall(Unit) { it.destroy() } }
                            }
                            if (disposed) return
                            nativeAd = ad
                            loadState = NativeLoadState.Loaded
                        }

                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            if (!disposed) loadState = NativeLoadState.Failed
                            adsLogW("load_failed", screen, "native ${adError.message}")
                        }
                    }
                )
            } catch (t: Throwable) {
                loadState = NativeLoadState.Failed
                adsLogE("load_exception", screen, "native ${t.message}", t)
            }
            onDispose {
                disposed = true
                release()
            }
        }
    }

    when {
        loadState == NativeLoadState.Failed -> Unit
        (loadState == NativeLoadState.WaitingSdk || loadState == NativeLoadState.Loading) &&
            showShimmer -> {
            when (template) {
                NativeAdTemplate.SMALL ->
                    NativeAdSmallShimmer(modifier = modifier.fillMaxWidth())
                NativeAdTemplate.MEDIUM,
                NativeAdTemplate.LARGE ->
                    NativeAdMediumShimmer(modifier = modifier.fillMaxWidth())
            }
        }
        loadState == NativeLoadState.Loaded -> {
            val ad = nativeAd ?: return
            AndroidView(
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                factory = { ctx ->
                    FrameLayout(ctx).apply {
                        val inflater = LayoutInflater.from(ctx)
                        val adView = inflateNativeAdView(inflater, template)
                        bindNativeAd(adView, ad, ctaColor)
                        removeAllViews()
                        addView(adView)
                    }
                },
                update = { container ->
                    container.removeAllViews()
                    val inflater = LayoutInflater.from(container.context)
                    val adView = inflateNativeAdView(inflater, template)
                    bindNativeAd(adView, ad, ctaColor)
                    container.addView(adView)
                }
            )
        }
    }
}
