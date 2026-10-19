package com.resume.builder.core.locale

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.LocaleList
import androidx.core.content.edit
import java.util.Locale

/** A language the app UI is translated into. An empty [tag] means "follow the system". */
data class AppLanguage(val tag: String, val nativeName: String, val englishName: String)

/**
 * Per-app UI language. Android 13+ stores the choice in the system (also editable from the system
 * "App languages" settings); older versions keep it in preferences and wrap the base context.
 */
object AppLocale {

    val languages = listOf(
        AppLanguage("en", "English", "English"),
        AppLanguage("es", "Español", "Spanish"),
        AppLanguage("fr", "Français", "French"),
        AppLanguage("pt", "Português", "Portuguese"),
        AppLanguage("de", "Deutsch", "German"),
        AppLanguage("it", "Italiano", "Italian"),
        AppLanguage("ru", "Русский", "Russian"),
        AppLanguage("ar", "العربية", "Arabic"),
        AppLanguage("hi", "हिन्दी", "Hindi"),
        AppLanguage("id", "Bahasa Indonesia", "Indonesian"),
        AppLanguage("tr", "Türkçe", "Turkish"),
        AppLanguage("vi", "Tiếng Việt", "Vietnamese"),
        AppLanguage("zh-CN", "简体中文", "Chinese (Simplified)"),
        AppLanguage("ja", "日本語", "Japanese"),
        AppLanguage("ko", "한국어", "Korean"),
    )

    /** The chosen language tag, or "" when the app follows the system language. */
    fun current(context: Context): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val locale = context.getSystemService(LocaleManager::class.java)?.applicationLocales?.get(0) ?: return ""
            return match(locale)?.tag ?: locale.toLanguageTag()
        }
        return prefs(context).getString(KEY, "").orEmpty()
    }

    /** Applies [tag] ("" = system) and recreates the UI in the new language. */
    fun set(activity: Activity, tag: String) {
        if (tag == current(activity)) return
        prefs(activity).edit { putString(KEY, tag) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // The system recreates the activity with the new configuration.
            activity.getSystemService(LocaleManager::class.java)?.applicationLocales =
                if (tag.isEmpty()) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            // The activity re-wraps its context on recreate; the application context is updated here so
            // strings resolved outside the UI (repositories, notifications) follow without a restart.
            val locale = if (tag.isEmpty()) Resources.getSystem().configuration.locales[0] else Locale.forLanguageTag(tag)
            Locale.setDefault(locale)
            val app = activity.applicationContext.resources
            val config = Configuration(app.configuration).apply { setLocale(locale) }
            @Suppress("DEPRECATION")
            app.updateConfiguration(config, app.displayMetrics)
            activity.recreate()
        }
    }

    /** Wraps [base] with the chosen locale on Android 12 and lower; newer versions are handled by the system. */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = prefs(base).getString(KEY, "").orEmpty()
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }

    private fun match(locale: Locale): AppLanguage? =
        languages.firstOrNull { Locale.forLanguageTag(it.tag).let { l -> l.language == locale.language && (l.country.isEmpty() || l.country == locale.country) } }
            ?: languages.firstOrNull { Locale.forLanguageTag(it.tag).language == locale.language }

    // Same file as AppPreferences; read directly because wrapping happens before DI starts.
    private fun prefs(context: Context) = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    private const val KEY = "app_language"
}
