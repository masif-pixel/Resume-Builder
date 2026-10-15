package com.pixelforge.ads.banner

import com.google.android.libraries.ads.mobile.sdk.banner.BannerAd
import com.pixelforge.ads.common.gmaCall
import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps loaded [BannerAd] instances across Compose disposal (e.g. exit bottom sheet
 * close/reopen) so the same creative is shown without a second network load.
 * Cleared from [com.pixelforge.ads.AdsSdk.destroyAll] / exit.
 */
class RetainedBannerStore {
    private val ads = ConcurrentHashMap<String, BannerAd>()

    fun get(key: String): BannerAd? = ads[key]

    fun put(key: String, ad: BannerAd) {
        gmaCall(Unit) { ads.put(key, ad)?.destroy() }
    }

    fun remove(key: String) {
        gmaCall(Unit) { ads.remove(key)?.destroy() }
    }

    fun destroyAll() {
        ads.keys.toList().forEach { key ->
            gmaCall(Unit) { ads.remove(key)?.destroy() }
        }
    }
}
