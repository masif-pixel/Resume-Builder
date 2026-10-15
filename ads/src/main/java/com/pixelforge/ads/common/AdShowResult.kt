package com.pixelforge.ads.common

/** Filter Logcat with this tag for every ads call / load / show / fail. */
const val TAG_ADS = "TAG_ADS"

/** Structured ads logging: event + screen name + details. */
fun adsLog(event: String, screen: String? = null, detail: String? = null) {
    val screenPart = screen?.let { " screen=$it" }.orEmpty()
    val detailPart = detail?.let { " | $it" }.orEmpty()
    android.util.Log.i(TAG_ADS, "$event$screenPart$detailPart")
}

fun adsLogW(event: String, screen: String? = null, detail: String? = null) {
    val screenPart = screen?.let { " screen=$it" }.orEmpty()
    val detailPart = detail?.let { " | $it" }.orEmpty()
    android.util.Log.w(TAG_ADS, "$event$screenPart$detailPart")
}

fun adsLogE(
    event: String,
    screen: String? = null,
    detail: String? = null,
    error: Throwable? = null,
) {
    val screenPart = screen?.let { " screen=$it" }.orEmpty()
    val detailPart = detail?.let { " | $it" }.orEmpty()
    android.util.Log.e(TAG_ADS, "$event$screenPart$detailPart", error)
}

/**
 * True only after [com.google.android.libraries.ads.mobile.sdk.MobileAds.initialize]
 * succeeds. RC skip never sets this.
 */
object GmaRuntime {
    @Volatile
    private var ready: Boolean = false

    fun isReady(): Boolean = ready

    fun setReady(value: Boolean) {
        ready = value
        adsLog("gma_runtime", detail = "ready=$value")
    }
}

/**
 * GMA Next-Gen throws [IllegalStateException] if preloaders/destroy/load run before
 * [com.google.android.libraries.ads.mobile.sdk.MobileAds.initialize].
 */
inline fun <T> gmaCall(default: T, block: () -> T): T {
    if (!GmaRuntime.isReady()) {
        adsLogW("gma_call_skip", detail = "MobileAds not initialized")
        return default
    }
    return try {
        block()
    } catch (t: Throwable) {
        adsLogW("gma_call_skip", detail = t.message)
        default
    }
}

/** Shared callbacks for full-screen formats. */
fun interface AdShowCallback {
    fun onResult(result: AdShowResult)
}

sealed class AdShowResult {
    data object Shown : AdShowResult()
    data object Impression : AdShowResult()
    data object Dismissed : AdShowResult()
    data object NotReady : AdShowResult()
    data object Disabled : AdShowResult()
    data class Failed(val message: String) : AdShowResult()
    data class Rewarded(val type: String, val amount: Int) : AdShowResult()
}
