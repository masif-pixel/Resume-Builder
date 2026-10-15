package com.pixelforge.ads.nativead

import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.pixelforge.ads.common.gmaCall
import java.util.concurrent.ConcurrentHashMap

/**
 * Keeps loaded [NativeAd] instances across Compose disposal (e.g. Home left for another screen
 * and re-entered) so the same creative is shown without a second network load.
 * Cleared from [com.pixelforge.ads.AdsSdk.destroyAll] / exit.
 */
class RetainedNativeStore {
    private val ads = ConcurrentHashMap<String, NativeAd>()

    fun get(key: String): NativeAd? = ads[key]

    fun put(key: String, ad: NativeAd) {
        val previous = ads.put(key, ad)
        if (previous != null && previous !== ad) gmaCall(Unit) { previous.destroy() }
    }

    fun destroyAll() {
        ads.keys.toList().forEach { key ->
            ads.remove(key)?.let { gmaCall(Unit) { it.destroy() } }
        }
    }
}
