package com.pixelforge.ads.adfree

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory mirror of the app's persisted ads-free expiry.
 * Call [sync] whenever DataStore updates so [AdsEligibility] and Compose slots react.
 */
class AdFreeSession {
    private val _untilEpochMs = MutableStateFlow(0L)
    val untilEpochMs: StateFlow<Long> = _untilEpochMs.asStateFlow()

    fun sync(untilEpochMs: Long) {
        _untilEpochMs.value = untilEpochMs.coerceAtLeast(0L)
    }

    fun isActive(now: Long = System.currentTimeMillis()): Boolean =
        _untilEpochMs.value > now

    companion object {
        const val DEFAULT_DURATION_MS: Long = 2L * 60L * 60L * 1000L
    }
}
