package com.resume.builder.presentation.components

import android.content.Context
import androidx.annotation.PluralsRes
import androidx.annotation.StringRes

/** Text produced outside the UI (view models, engines) and resolved against the current locale when shown. */
sealed interface UiText {
    data class Raw(val value: String) : UiText

    class Res(@StringRes val id: Int, vararg val args: Any) : UiText

    class Plural(@PluralsRes val id: Int, val count: Int, vararg val args: Any) : UiText

    fun resolve(context: Context): String = when (this) {
        is Raw -> value
        is Res -> context.getString(id, *args.map { if (it is UiText) it.resolve(context) else it }.toTypedArray())
        is Plural -> context.resources.getQuantityString(id, count, *args.map { if (it is UiText) it.resolve(context) else it }.toTypedArray())
    }
}
