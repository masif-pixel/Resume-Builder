package com.resume.builder.data.local.prefs

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.core.content.edit
import com.resume.builder.domain.model.ThemeMode
import com.resume.builder.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Lightweight key-value settings backed by SharedPreferences. */
class AppPreferences(context: Context) : PreferencesRepository {

    private val appContext = context.applicationContext
    private val prefs = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(
        runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.SYSTEM.name)!!) }.getOrDefault(ThemeMode.SYSTEM),
    )
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    init {
        applySystemNightMode(_themeMode.value)
    }

    override fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString(KEY_THEME, mode.name) }
        _themeMode.value = mode
        applySystemNightMode(mode)
    }

    /**
     * Tells Android 12+ which theme the app uses, so the system splash screen (shown before any app code runs)
     * picks the matching values/values-night resources. Older versions follow the system setting.
     */
    private fun applySystemNightMode(mode: ThemeMode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val uiModeManager = appContext.getSystemService(UiModeManager::class.java) ?: return
        val nightMode = when (mode) {
            ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
            ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
            ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
        }
        if (uiModeManager.nightMode != nightMode) uiModeManager.setApplicationNightMode(nightMode)
    }

    override fun isOnboardingDone(): Boolean = prefs.getBoolean(KEY_ONBOARDING, false)

    override fun setOnboardingDone(referralSource: String?) {
        prefs.edit {
            putBoolean(KEY_ONBOARDING, true)
            referralSource?.let { putString(KEY_REFERRAL, it) }
        }
    }

    private val _adFreeUntil = MutableStateFlow(prefs.getLong(KEY_AD_FREE_UNTIL, 0L))
    override val adFreeUntilEpochMs: StateFlow<Long> = _adFreeUntil.asStateFlow()

    @Synchronized
    override fun grantAdFree(durationMs: Long): Long {
        val now = System.currentTimeMillis()
        val current = prefs.getLong(KEY_AD_FREE_UNTIL, 0L)
        val until = if (current > now) current else now + durationMs
        prefs.edit { putLong(KEY_AD_FREE_UNTIL, until) }
        _adFreeUntil.value = until
        return until
    }

    private companion object {
        const val KEY_AD_FREE_UNTIL = "ad_free_until_epoch_ms"
        const val KEY_THEME = "theme_mode"
        const val KEY_ONBOARDING = "onboarding_done"
        const val KEY_REFERRAL = "referral_source"
    }
}
