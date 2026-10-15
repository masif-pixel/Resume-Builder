package com.pixelforge.ads.banner

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.WindowManager
import com.google.android.libraries.ads.mobile.sdk.banner.AdSize

enum class BannerAdStyle {
    /** Classic 320×50. */
    BANNER,
    /** 320×100. */
    LARGE_BANNER,
    /** 300×250 MREC. */
    MEDIUM_RECTANGLE,
    /** Anchored adaptive (standard height). */
    ANCHORED_ADAPTIVE,
    /** Larger anchored adaptive. */
    LARGE_ANCHORED_ADAPTIVE,
    /** Anchored adaptive + collapsible overlay (bottom). */
    COLLAPSIBLE_BOTTOM,
    /** Anchored adaptive + collapsible overlay (top). */
    COLLAPSIBLE_TOP,
}

fun BannerAdStyle.toAdSize(context: Context): AdSize {
    val widthDp = context.currentWindowWidthDp()
    return when (this) {
        BannerAdStyle.BANNER -> AdSize.BANNER
        BannerAdStyle.LARGE_BANNER -> AdSize.LARGE_BANNER
        BannerAdStyle.MEDIUM_RECTANGLE -> AdSize.MEDIUM_RECTANGLE
        BannerAdStyle.ANCHORED_ADAPTIVE,
        BannerAdStyle.COLLAPSIBLE_BOTTOM,
        BannerAdStyle.COLLAPSIBLE_TOP ->
            AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
        BannerAdStyle.LARGE_ANCHORED_ADAPTIVE ->
            AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp)
    }
}

fun BannerAdStyle.collapsibleExtras(): Bundle? = when (this) {
    BannerAdStyle.COLLAPSIBLE_BOTTOM -> Bundle().apply { putString("collapsible", "bottom") }
    BannerAdStyle.COLLAPSIBLE_TOP -> Bundle().apply { putString("collapsible", "top") }
    else -> null
}

private fun Context.currentWindowWidthDp(): Int {
    val wm = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val metrics = DisplayMetrics()
    @Suppress("DEPRECATION")
    wm.defaultDisplay.getMetrics(metrics)
    val widthPx = if (this is Activity) {
        resources.displayMetrics.widthPixels
    } else {
        metrics.widthPixels
    }
    return (widthPx / resources.displayMetrics.density).toInt().coerceAtLeast(320)
}
