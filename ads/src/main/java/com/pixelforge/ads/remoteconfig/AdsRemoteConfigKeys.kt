package com.pixelforge.ads.remoteconfig

/**
 * Firebase Remote Config keys — **per screen / placement**, not one key per ad format.
 *
 * Format switches are **Int**: `0` = OFF, `1` = ON.
 * Timing keys are **Int** seconds.
 * Create the same parameter names in Firebase Console → Remote Config.
 *
 * When you add a new ad, add a matching key here + unit ID in [com.pixelforge.ads.config.AdsUnitIds].
 */
object AdsRemoteConfigKeys {
    /** Master kill-switch for every placement. */
    const val MASTER = "ads_master"

    // --- Screen placements (0/1) ---
    const val INTER_SPLASH = "ads_inter_splash"
    const val INTER_HOME = "ads_inter_home"
    // Unused in this app (no tools-hub interstitial): Tools hub item click — same home inter unit, separate RC gate.
    // const val INTER_TOOL = "ads_inter_tool"
    const val APP_OPEN = "ads_app_open"
    const val NATIVE_REFERRAL = "ads_native_referral"
    const val NATIVE_HOME = "ads_native_home"
    const val NATIVE_ONBOARDING = "ads_native_onboarding"
    const val NATIVE_PDF_TOOLS = "ads_native_pdf_tools"
    const val NATIVE_TEMPLATES = "ads_native_templates"
    // Unused in this app (no banner on the exit sheet).
    // const val BANNER_EXIT = "ads_banner_exit"

    // --- Optional / future (0/1) — BANNER and REWARDED are unused in this app (no unit IDs) ---
    // const val BANNER = "ads_banner"
    // const val REWARDED = "ads_rewarded"
    const val REWARDED_INTERSTITIAL = "ads_rewarded_interstitial"

    /**
     * Seconds after Home / after home-inter dismiss before the next bottom-tab interstitial.
     * Default [DEFAULT_INTER_HOME_SECONDS].
     */
    const val INTER_HOME_SECONDS = "ads_inter_home_seconds"
    const val DEFAULT_INTER_HOME_SECONDS = 10

    /**
     * Native ad call-to-action background, hex string such as `#4F46E5`.
     * Empty or invalid values keep the layout color [DEFAULT_NATIVE_CTA_COLOR].
     */
    const val NATIVE_CTA_COLOR = "ads_native_cta_color"
    const val DEFAULT_NATIVE_CTA_COLOR = "#4F46E5"

    /** 0/1 placement switches. */
    val allAdKeys: List<String> = listOf(
        MASTER,
        INTER_SPLASH,
        INTER_HOME,
        // INTER_TOOL,
        APP_OPEN,
        NATIVE_REFERRAL,
        NATIVE_HOME,
        NATIVE_ONBOARDING,
        NATIVE_PDF_TOOLS,
        NATIVE_TEMPLATES,
        // BANNER_EXIT,
        // BANNER,
        // REWARDED,
        REWARDED_INTERSTITIAL,
    )

    /** Every key that needs an in-app default (flags + timings + colors). */
    val allKeys: List<String> = allAdKeys + INTER_HOME_SECONDS + NATIVE_CTA_COLOR
}

/**
 * Ad placements gated by Remote Config (screen-specific where applicable).
 */
enum class AdFormat(val remoteConfigKey: String) {
    INTER_SPLASH(AdsRemoteConfigKeys.INTER_SPLASH),
    INTER_HOME(AdsRemoteConfigKeys.INTER_HOME),
    // INTER_TOOL(AdsRemoteConfigKeys.INTER_TOOL),
    APP_OPEN(AdsRemoteConfigKeys.APP_OPEN),
    NATIVE_REFERRAL(AdsRemoteConfigKeys.NATIVE_REFERRAL),
    NATIVE_HOME(AdsRemoteConfigKeys.NATIVE_HOME),
    NATIVE_ONBOARDING(AdsRemoteConfigKeys.NATIVE_ONBOARDING),
    NATIVE_PDF_TOOLS(AdsRemoteConfigKeys.NATIVE_PDF_TOOLS),
    NATIVE_TEMPLATES(AdsRemoteConfigKeys.NATIVE_TEMPLATES),
    // BANNER_EXIT(AdsRemoteConfigKeys.BANNER_EXIT),
    // BANNER(AdsRemoteConfigKeys.BANNER),
    // REWARDED(AdsRemoteConfigKeys.REWARDED),
    REWARDED_INTERSTITIAL(AdsRemoteConfigKeys.REWARDED_INTERSTITIAL),
}

/** Formats rendered by [com.pixelforge.ads.nativead.NativeAdSlot]. */
val AdFormat.isNative: Boolean
    get() = when (this) {
        AdFormat.NATIVE_REFERRAL,
        AdFormat.NATIVE_HOME,
        AdFormat.NATIVE_ONBOARDING,
        AdFormat.NATIVE_PDF_TOOLS,
        AdFormat.NATIVE_TEMPLATES -> true
        else -> false
    }
