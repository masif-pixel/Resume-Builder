package com.pixelforge.ads.banner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.google.android.libraries.ads.mobile.sdk.banner.BannerAdRequest
import com.google.android.libraries.ads.mobile.sdk.common.AdLoadResult
import com.pixelforge.ads.AdsSdk
import com.pixelforge.ads.common.adsLog
import com.pixelforge.ads.common.adsLogE
import com.pixelforge.ads.common.adsLogW
import com.pixelforge.ads.common.findActivity
import com.pixelforge.ads.common.gmaCall
import com.pixelforge.ads.common.hasValidatedNetwork
import com.pixelforge.ads.config.AdsConfig
import com.pixelforge.ads.config.isConfiguredAdUnit
import com.pixelforge.ads.config.unitIdFor
import com.pixelforge.ads.remoteconfig.AdFormat
import com.pixelforge.ads.remoteconfig.AdsEligibility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject

private enum class BannerLoadState {
    Loading,
    Loaded,
    Failed,
}

/**
 * Compose banner for any [BannerAdStyle].
 *
 * @param placement RC gate + default unit (e.g. [AdFormat.BANNER_EXIT]).
 * @param retain When true, keeps the loaded ad in [RetainedBannerStore] across Compose disposal
 *   (exit sheet close/reopen shows the same ad without reload).
 */
@Composable
fun BannerAdSlot(
    placement: AdFormat,
    style: BannerAdStyle = BannerAdStyle.BANNER,
    screen: String = placement.name,
    adUnitId: String? = null,
    retain: Boolean = false,
    showShimmer: Boolean = true,
    /**
     * Breathing room above and below, applied by the slot itself so it disappears with the slot.
     * Hosts must not wrap this composable in their own `Spacer`s: every gate here renders nothing
     * at all — ads-free, Remote Config off, unconfigured unit, offline, load failed —
     * and outer spacers would survive all of them as a blank gap.
     */
    spacing: Dp = 0.dp,
    modifier: Modifier = Modifier,
    config: AdsConfig = koinInject(),
    eligibility: AdsEligibility = koinInject(),
    adsSdk: AdsSdk = koinInject(),
    retainedStore: RetainedBannerStore = koinInject(),
) {
    val adFreeUntil by adsSdk.adFreeSession.untilEpochMs.collectAsStateWithLifecycle()
    // Read (`.value`, not a delegate) before the gate so a slot composed on cached RC values is
    // recomposed when this launch's fetch lands; `canShow` itself is not state.
    adsSdk.remoteConfigReady.collectAsStateWithLifecycle().value
    if (System.currentTimeMillis() < adFreeUntil) return
    if (!eligibility.canShow(placement)) return

    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val inspection = LocalInspectionMode.current
    val unitId = adUnitId ?: unitIdFor(placement, config)
    if (!unitId.isConfiguredAdUnit()) return
    val retainKey = "${placement.name}:$unitId:${style.name}"

    // Offline: draw nothing and request nothing. An ad already in hand still shows — losing the
    // network should not blank a banner the user is looking at — but an empty slot must not put
    // up a shimmer for a request that cannot succeed. That shimmer is what made the exit sheet
    // open tall, fail, and then collapse out from under the user's thumb.
    val onlineEnough = remember(retainKey) {
        (retain && retainedStore.get(retainKey) != null) || context.hasValidatedNetwork()
    }
    if (!onlineEnough) {
        adsLog("banner_skip", screen, "offline unit=$unitId")
        return
    }

    var bannerAd by remember(retainKey) {
        mutableStateOf(if (retain) retainedStore.get(retainKey) else null)
    }
    var loadState by remember(retainKey) {
        mutableStateOf(
            if (bannerAd != null) BannerLoadState.Loaded else BannerLoadState.Loading
        )
    }

    LaunchedEffect(retainKey, style, unitId, activity) {
        if (inspection) return@LaunchedEffect
        if (activity == null) {
            adsLogW("banner_skip", screen, "no Activity from LocalContext")
            loadState = BannerLoadState.Failed
            return@LaunchedEffect
        }
        val cached = if (retain) retainedStore.get(retainKey) else null
        if (cached != null) {
            bannerAd = cached
            loadState = BannerLoadState.Loaded
            adsLog("banner_retain_hit", screen, "unit=$unitId")
            return@LaunchedEffect
        }

        loadState = BannerLoadState.Loading
        adsSdk.initialized.first { it }
        if (!adsSdk.mobileAdsReady.value) {
            adsLogW("banner_skip", screen, "MobileAds not ready after init")
            loadState = BannerLoadState.Failed
            return@LaunchedEffect
        }
        adsLog("banner_load_start", screen, "style=$style unit=$unitId retain=$retain")

        val result = runCatching {
            // GMA Next-Gen banner load should stay on Main.
            withContext(Dispatchers.Main) {
                val adSize = style.toAdSize(activity)
                val builder = BannerAdRequest.Builder(unitId, adSize)
                style.collapsibleExtras()?.let { builder.setGoogleExtrasBundle(it) }
                BannerAd.load(builder.build())
            }
        }.getOrElse { error ->
            adsLogE("banner_load_exception", screen, error.message, error)
            loadState = BannerLoadState.Failed
            return@LaunchedEffect
        }

        when (result) {
            is AdLoadResult.Success -> {
                if (retain) {
                    retainedStore.put(retainKey, result.ad)
                } else {
                    bannerAd?.let { gmaCall(Unit) { it.destroy() } }
                }
                bannerAd = result.ad
                loadState = BannerLoadState.Loaded
                adsLog("banner_load_success", screen, "unit=$unitId")
            }
            is AdLoadResult.Failure -> {
                loadState = BannerLoadState.Failed
                adsLogW("banner_load_failed", screen, "${result.error}")
            }
        }
    }

    DisposableEffect(retainKey) {
        onDispose {
            if (!retain) {
                bannerAd?.let { gmaCall(Unit) { it.destroy() } }
                bannerAd = null
            }
        }
    }

    when {
        loadState == BannerLoadState.Failed -> Unit
        loadState == BannerLoadState.Loading && showShimmer -> {
            BannerAdShimmer(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing),
                style = style
            )
        }
        loadState == BannerLoadState.Loaded -> {
            val ad = bannerAd
            val host = activity
            if (ad == null || host == null) return
            val heightDp = when (style) {
                BannerAdStyle.BANNER -> 50.dp
                BannerAdStyle.LARGE_BANNER -> 100.dp
                BannerAdStyle.MEDIUM_RECTANGLE -> 250.dp
                else -> 100.dp
            }
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = spacing)
                    .height(heightDp),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { _ ->
                        gmaCall(null) {
                            ad.getView(host).also { view ->
                                (view.parent as? android.view.ViewGroup)?.removeView(view)
                            }
                        } ?: android.widget.FrameLayout(host)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(heightDp)
                )
            }
        }
    }
}

private fun unitIdFor(placement: AdFormat, config: AdsConfig): String =
    config.unitIds.unitIdFor(placement)
