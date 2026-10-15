package com.pixelforge.ads.common

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Whether an ad request has any chance of reaching the network right now.
 *
 * Requires [NetworkCapabilities.NET_CAPABILITY_VALIDATED], not merely a connected transport: a
 * device associated to a captive portal or a router with no route out reports a network and then
 * stalls every request until it times out. Treats an unavailable ConnectivityManager as online so
 * a missing system service degrades into the old behaviour instead of hiding ads outright.
 */
fun Context.hasValidatedNetwork(): Boolean {
    val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return true
    val capabilities = runCatching {
        manager.getNetworkCapabilities(manager.activeNetwork ?: return false)
    }.getOrNull() ?: return false
    return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

/** Walks [ContextWrapper]s — needed inside ModalBottomSheet / Dialog where LocalContext is not the Activity. */
fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return current as? Activity
}
