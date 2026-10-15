package com.pixelforge.ads.nativead

import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.pixelforge.ads.R

internal fun inflateNativeAdView(
    inflater: LayoutInflater,
    template: NativeAdTemplate,
): NativeAdView {
    val layout = when (template) {
        NativeAdTemplate.SMALL -> R.layout.ads_native_small
        NativeAdTemplate.MEDIUM -> R.layout.ads_native_medium
        NativeAdTemplate.LARGE -> R.layout.ads_native_large
    }
    return inflater.inflate(layout, null, false) as NativeAdView
}

internal fun bindNativeAd(adView: NativeAdView, nativeAd: NativeAd, ctaColor: Int? = null) {
    val headline = adView.findViewById<TextView>(R.id.ad_headline)
    val body = adView.findViewById<TextView?>(R.id.ad_body)
    val cta = adView.findViewById<TextView>(R.id.ad_call_to_action)
    val icon = adView.findViewById<ImageView>(R.id.ad_app_icon)
    val advertiser = adView.findViewById<TextView?>(R.id.ad_advertiser)
    val media = adView.findViewById<MediaView?>(R.id.ad_media)

    adView.headlineView = headline
    adView.bodyView = body
    adView.callToActionView = cta
    adView.iconView = icon
    adView.advertiserView = advertiser

    headline.text = nativeAd.headline
    body?.bindOrHide(nativeAd.body)
    cta.bindOrHide(nativeAd.callToAction)
    ctaColor?.let { cta.applyCtaColor(it) }
    advertiser?.bindOrHide(nativeAd.advertiser)

    val drawable = nativeAd.icon?.drawable
    if (drawable != null) {
        icon.setImageDrawable(drawable)
        icon.visibility = View.VISIBLE
    } else {
        icon.visibility = View.GONE
    }

    // Media is optional (large template only).
    adView.registerNativeAd(nativeAd, media)
}

private fun TextView.applyCtaColor(color: Int) {
    val drawable = background?.mutate()
    if (drawable is GradientDrawable) {
        drawable.setColor(color)
        background = drawable
    } else {
        backgroundTintList = ColorStateList.valueOf(color)
    }
}

private fun TextView.bindOrHide(value: CharSequence?) {
    if (value.isNullOrBlank()) {
        visibility = View.GONE
    } else {
        text = value
        visibility = View.VISIBLE
    }
}
