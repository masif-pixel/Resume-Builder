package com.pixelforge.ads.consent

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

private const val TAG_CONSENT = "TAG_CONSENT"

/**
 * Asks UMP if a privacy message is required, then shows it.
 * Does not gate ad loading. Call this from onboarding, not before the splash ad.
 */
fun Activity.showConsentFormIfRequired() {
    val consentInformation = UserMessagingPlatform.getConsentInformation(this)
    val params = ConsentRequestParameters.Builder().build()
    consentInformation.requestConsentInfoUpdate(
        this,
        params,
        {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(this) { error ->
                if (error != null) {
                    Log.w(TAG_CONSENT, "consent form error ${error.errorCode} ${error.message}")
                } else {
                    Log.i(TAG_CONSENT, "consent complete canRequestAds=${consentInformation.canRequestAds()}")
                }
            }
        },
        { error ->
            Log.w(TAG_CONSENT, "consent info update failed ${error.errorCode} ${error.message}")
        },
    )
}
