package com.resume.builder.presentation.components

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.resume.builder.R
import com.resume.builder.core.locale.AppLanguage
import com.resume.builder.core.locale.AppLocale

/** Name of the current app language in its own script, or "System default". */
@Composable
fun currentAppLanguageName(): String {
    val context = LocalContext.current
    val tag = remember(context) { AppLocale.current(context) }
    return AppLocale.languages.firstOrNull { it.tag == tag }?.nativeName ?: stringResource(R.string.app_language_system)
}

/**
 * Picks the language of the app UI. The applied language is listed first; a choice only takes
 * effect on Apply, which recreates the activity in the new language.
 */
@Composable
fun AppLanguageDialog(onDismiss: () -> Unit) {
    val activity = LocalActivity.current
    val context = LocalContext.current
    val applied = remember(context) { AppLocale.current(context) }
    var selected by rememberSaveable { mutableStateOf(applied) }
    val system = AppLanguage("", stringResource(R.string.app_language_system), "")
    val options = remember(applied, system.nativeName) {
        val all = listOf(system) + AppLocale.languages
        all.filter { it.tag == applied } + all.filter { it.tag != applied }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.app_language)) },
        text = {
            // Capped so the dialog stays compact; the rest of the list scrolls.
            LazyColumn(Modifier.heightIn(max = 420.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(options, key = { it.tag }) { language ->
                    LanguageOption(
                        language = language,
                        selected = language.tag == selected,
                        applied = language.tag == applied,
                        onClick = { selected = language.tag },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDismiss()
                    activity?.let { AppLocale.set(it, selected) }
                },
                enabled = selected != applied,
            ) { Text(stringResource(R.string.common_apply)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
    )
}

@Composable
private fun LanguageOption(language: AppLanguage, selected: Boolean, applied: Boolean, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) colors.primaryContainer else colors.surface,
        border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) colors.primary else colors.outlineVariant),
        shadowElevation = if (selected) 0.dp else 1.dp,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    language.nativeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (selected) colors.onPrimaryContainer else colors.onSurface,
                )
                val secondary = listOfNotNull(
                    language.englishName.takeIf { it.isNotEmpty() && it != language.nativeName },
                    stringResource(R.string.app_language_current).takeIf { applied },
                ).joinToString(" · ")
                if (secondary.isNotEmpty()) {
                    Text(secondary, style = MaterialTheme.typography.bodySmall, color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant)
                }
            }
            if (selected) {
                Spacer(Modifier.width(12.dp))
                Surface(shape = CircleShape, color = colors.primary, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Rounded.Check, null, tint = colors.onPrimary, modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}
