package com.resume.builder.presentation.settings

import androidx.compose.ui.res.stringResource
import com.resume.builder.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Feedback
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Policy
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.resume.builder.BuildConfig
import com.resume.builder.domain.model.ThemeMode
import com.resume.builder.domain.repository.PreferencesRepository
import com.resume.builder.presentation.components.AppLanguageDialog
import com.resume.builder.presentation.components.AppTopBar
import com.resume.builder.presentation.components.FileActions
import com.resume.builder.presentation.components.currentAppLanguageName
import com.resume.builder.presentation.components.displayName
import com.resume.builder.presentation.components.formatModified
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val prefs: PreferencesRepository,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> = prefs.themeMode

    fun setTheme(mode: ThemeMode) = prefs.setThemeMode(mode)
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onLegal: (String) -> Unit,
    viewModel: SettingsViewModel = org.koin.androidx.compose.koinViewModel(),
) {
    val context = LocalContext.current
    val theme by viewModel.themeMode.collectAsStateWithLifecycle()
    var themeDialog by remember { mutableStateOf(false) }
    var languageDialog by remember { mutableStateOf(false) }

    Scaffold(topBar = { AppTopBar(title = stringResource(R.string.drawer_settings), onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Group(stringResource(R.string.settings_appearance)) {
                SettingRow(Icons.Rounded.Palette, stringResource(R.string.settings_theme), value = theme.displayName(), onClick = { themeDialog = true })
                SettingRow(Icons.Rounded.Translate, stringResource(R.string.app_language), value = currentAppLanguageName(), onClick = { languageDialog = true })
            }
            Group(stringResource(R.string.settings_ai)) {
                SettingRow(
                    Icons.Rounded.AutoAwesome,
                    stringResource(R.string.settings_status),
                    value = stringResource(R.string.settings_on_device),
                    subtitle = stringResource(R.string.settings_ai_desc),
                )
            }
            Group(stringResource(R.string.settings_legal)) {
                SettingRow(Icons.Rounded.Policy, stringResource(R.string.legal_privacy_policy), onClick = { onLegal("privacy") })
                SettingRow(Icons.Rounded.Description, stringResource(R.string.legal_terms_of_service), onClick = { onLegal("terms") })
            }
            Group(stringResource(R.string.settings_support)) {
                SettingRow(Icons.Rounded.QuestionAnswer, stringResource(R.string.settings_faq), onClick = { onLegal("faq") })
                SettingRow(Icons.Rounded.Feedback, stringResource(R.string.drawer_feedback), onClick = { FileActions.sendFeedback(context, BuildConfig.VERSION_NAME) })
                SettingRow(Icons.Rounded.Star, stringResource(R.string.settings_rate), onClick = { FileActions.rateApp(context) })
            }
            Group(stringResource(R.string.settings_about)) {
                SettingRow(Icons.Rounded.Info, stringResource(R.string.settings_version), value = BuildConfig.VERSION_NAME)
            }
        }
    }

    if (languageDialog) AppLanguageDialog(onDismiss = { languageDialog = false })

    if (themeDialog) {
        AlertDialog(
            onDismissRequest = { themeDialog = false },
            title = { Text(stringResource(R.string.settings_choose_theme)) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        val icon = when (mode) {
                            ThemeMode.LIGHT -> Icons.Rounded.LightMode
                            ThemeMode.DARK -> Icons.Rounded.DarkMode
                            ThemeMode.SYSTEM -> Icons.Rounded.BrightnessAuto
                        }
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp))
                                .selectable(selected = theme == mode, role = Role.RadioButton) { viewModel.setTheme(mode); themeDialog = false }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(icon, null)
                            Spacer(Modifier.width(14.dp))
                            Text(mode.displayName(), modifier = Modifier.weight(1f))
                            RadioButton(selected = theme == mode, onClick = null)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { themeDialog = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun Group(title: String, content: @Composable () -> Unit) {
    Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 8.dp, top = 12.dp, bottom = 8.dp))
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    value: String? = null,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        value?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        trailing?.invoke()
        if (onClick != null && trailing == null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private val faq: List<Pair<Int, Int>> = listOf(
    R.string.faq_free_q to R.string.faq_free_a,
    R.string.faq_storage_q to R.string.faq_storage_a,
    R.string.faq_ats_q to R.string.faq_ats_a,
    R.string.faq_language_q to R.string.faq_language_a,
    R.string.faq_ai_q to R.string.faq_ai_a,
    R.string.faq_pdf_q to R.string.faq_pdf_a,
)

@Composable
fun LegalScreen(type: String, onBack: () -> Unit) {
    val title = when (type) {
        "privacy" -> stringResource(R.string.legal_privacy_policy)
        "terms" -> stringResource(R.string.legal_terms_of_service)
        else -> stringResource(R.string.settings_faq)
    }
    Scaffold(topBar = { AppTopBar(title = title, onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
            when (type) {
                "privacy" -> LegalBody(
                    listOf(
                        R.string.privacy_store_h to R.string.privacy_store_b,
                        R.string.privacy_ai_h to R.string.privacy_ai_b,
                        R.string.privacy_onboarding_h to R.string.privacy_onboarding_b,
                        R.string.privacy_control_h to R.string.privacy_control_b,
                    ),
                )
                "terms" -> LegalBody(
                    listOf(
                        R.string.terms_use_h to R.string.terms_use_b,
                        R.string.terms_ai_h to R.string.terms_ai_b,
                        R.string.terms_content_h to R.string.terms_content_b,
                        R.string.terms_availability_h to R.string.terms_availability_b,
                    ),
                )
                else -> faq.forEach { (q, a) -> FaqItem(stringResource(q), stringResource(a)) }
            }
        }
    }
}

@Composable
private fun LegalBody(sections: List<Pair<Int, Int>>) {
    sections.forEach { (heading, body) ->
        Text(stringResource(heading), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun FaqItem(question: String, answer: String) {
    var open by rememberSaveable(question) { mutableStateOf(false) }
    Surface(
        onClick = { open = !open },
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(question, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Icon(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
            }
            AnimatedVisibility(open) {
                Text(answer, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}
