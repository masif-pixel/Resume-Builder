package com.pixelforge.ads.remoteconfig

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.pixelforge.ads.common.adsLog
import com.pixelforge.ads.common.adsLogE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await

class AdsRemoteConfig(
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
) {
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val defaultsApplied = remoteConfig.setDefaultsAsync(defaultValues())
    private val settingsApplied = remoteConfig.setConfigSettingsAsync(
        FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(FETCH_INTERVAL_SECONDS)
            // The SDK default is 60s. Splash waits on this fetch, so it must give up inside
            // the window splash is prepared to wait.
            .setFetchTimeoutInSeconds(FETCH_TIMEOUT_MS / 1_000L)
            .build()
    )

    /** Local defaults only. Does not hit the network. */
    suspend fun awaitDefaults() {
        runCatching { defaultsApplied.await() }
    }

    /**
     * Fetches latest values from Firebase and activates them.
     * Called on every cold start before splash continues. [fetch] with a 0-second
     * cache window so a console change is visible on the next launch.
     * Settings are awaited first; otherwise the SDK keeps its 12-hour cache.
     * @return true when activate succeeded (or defaults already usable).
     */
    suspend fun fetchAndActivate(): Boolean {
        return runCatching {
            settingsApplied.await()
            remoteConfig.fetch(FETCH_INTERVAL_SECONDS).await()
            val updated = remoteConfig.activate().await()
            _ready.value = true
            adsLog("rc_activated", detail = "updated=$updated")
            logSnapshot()
            true
        }.getOrElse { error ->
            adsLogE("rc_fetch_failed", detail = "using last activated values", error = error)
            _ready.value = true
            false
        }
    }

    /** String parameter (empty if the key was never set). */
    fun getString(key: String): String = remoteConfig.getString(key)

    /** Raw Int for any RC key (defaults to 0 if missing). */
    fun getInt(key: String): Int = remoteConfig.getLong(key).toInt()

    /**
     * Native CTA fill. Remote Config hex wins when it parses; otherwise null so the
     * layout keeps `@color/ads_accent` (`#4F46E5`).
     */
    fun nativeCtaColor(): Int? {
        val raw = getString(AdsRemoteConfigKeys.NATIVE_CTA_COLOR).trim()
        if (raw.isEmpty()) return null
        val hex = if (raw.startsWith("#")) raw else "#$raw"
        return runCatching { android.graphics.Color.parseColor(hex) }.getOrNull()
    }

    /** `true` when key value is exactly 1. */
    fun isOn(key: String): Boolean = getInt(key) == ON

    /**
     * Placement is allowed when master == 1 **and** the placement key == 1.
     */
    fun isAdEnabled(format: AdFormat): Boolean =
        isOn(AdsRemoteConfigKeys.MASTER) && isOn(format.remoteConfigKey)

    fun isMasterEnabled(): Boolean = isOn(AdsRemoteConfigKeys.MASTER)

    private fun logSnapshot() {
        AdsRemoteConfigKeys.allAdKeys.forEach { key ->
            adsLog("rc_flag", detail = "$key=${getInt(key)}")
        }
        adsLog(
            "rc_flag",
            detail = "${AdsRemoteConfigKeys.INTER_HOME_SECONDS}=${interHomeCooldownSeconds()}"
        )
        adsLog(
            "rc_flag",
            detail = "${AdsRemoteConfigKeys.NATIVE_CTA_COLOR}=${getString(AdsRemoteConfigKeys.NATIVE_CTA_COLOR)}"
        )
    }

    /**
     * Home interstitial gate in seconds (Remote Config Int).
     * Falls back to [AdsRemoteConfigKeys.DEFAULT_INTER_HOME_SECONDS]; never negative.
     */
    fun interHomeCooldownSeconds(): Int =
        getInt(AdsRemoteConfigKeys.INTER_HOME_SECONDS)
            .takeIf { it >= 0 }
            ?: AdsRemoteConfigKeys.DEFAULT_INTER_HOME_SECONDS

    fun interHomeCooldownMs(): Long = interHomeCooldownSeconds() * 1_000L

    /** @deprecated Use [interHomeCooldownMs]. */
    fun interstitialBottomNavCooldownMs(): Long = interHomeCooldownMs()

    companion object {
        const val OFF = 0
        const val ON = 1

        /**
         * Cache window for each splash fetch. 0 means the next launch always
         * asks Firebase instead of reusing the previous 12-hour cache.
         */
        private const val FETCH_INTERVAL_SECONDS = 0L

        /** Longest a single fetch may take; splash waits on it for at most this long. */
        const val FETCH_TIMEOUT_MS = 15_000L

        fun defaultValues(): Map<String, Any> = buildMap {
            AdsRemoteConfigKeys.allAdKeys.forEach { key -> put(key, ON) }
            put(
                AdsRemoteConfigKeys.INTER_HOME_SECONDS,
                AdsRemoteConfigKeys.DEFAULT_INTER_HOME_SECONDS
            )
            put(
                AdsRemoteConfigKeys.NATIVE_CTA_COLOR,
                AdsRemoteConfigKeys.DEFAULT_NATIVE_CTA_COLOR
            )
        }
    }
}
