package com.resume.builder.presentation.settings

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
import com.resume.builder.presentation.components.AppTopBar
import com.resume.builder.presentation.components.FileActions
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

    Scaffold(topBar = { AppTopBar(title = "Settings", onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Group("Appearance") {
                SettingRow(Icons.Rounded.Palette, "Theme", value = theme.label, onClick = { themeDialog = true })
            }
            Group("AI assistant") {
                SettingRow(
                    Icons.Rounded.AutoAwesome,
                    "Status",
                    value = "On-device",
                    subtitle = "Drafts resumes & cover letters offline, no internet needed",
                )
            }
            Group("Legal") {
                SettingRow(Icons.Rounded.Policy, "Privacy policy", onClick = { onLegal("privacy") })
                SettingRow(Icons.Rounded.Description, "Terms of service", onClick = { onLegal("terms") })
            }
            Group("Support") {
                SettingRow(Icons.Rounded.QuestionAnswer, "FAQ", onClick = { onLegal("faq") })
                SettingRow(Icons.Rounded.Feedback, "Send feedback", onClick = { FileActions.sendFeedback(context, BuildConfig.VERSION_NAME) })
                SettingRow(Icons.Rounded.Star, "Rate the app", onClick = { FileActions.rateApp(context) })
            }
            Group("About") {
                SettingRow(Icons.Rounded.Info, "Version", value = BuildConfig.VERSION_NAME)
            }
        }
    }

    if (themeDialog) {
        AlertDialog(
            onDismissRequest = { themeDialog = false },
            title = { Text("Choose theme") },
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
                            Text(mode.label, modifier = Modifier.weight(1f))
                            RadioButton(selected = theme == mode, onClick = null)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { themeDialog = false }) { Text("Cancel") } },
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

private val faq = listOf(
    "Is the app free?" to "Yes. Every template, customization option and PDF tool is free to use.",
    "Where are my resumes stored?" to "On your device in a private database. If you sign in, they're also backed up to your cloud account and synced across devices.",
    "Are the templates ATS-friendly?" to "Templates tagged ATS (Clarity, Compact Pro, Ember) use a simple single-column structure that applicant tracking systems read reliably.",
    "Can I use my own language?" to "Yes. Write your content in any language and pick it under Preview › Language to translate section titles and enable right-to-left layout for Arabic or Urdu.",
    "How does AI writing work?" to "Your text is sent securely to the AI assistant to draft or polish content. Always review generated text — it never invents employers or dates on purpose, but double-check facts.",
    "Why can't I select text in PDFs from PDF tools?" to "Merge, compress, split and sign re-render pages on your device for privacy, which turns pages into images. Resumes you export from the editor keep selectable text.",
)

@Composable
fun LegalScreen(type: String, onBack: () -> Unit) {
    val title = when (type) {
        "privacy" -> "Privacy policy"
        "terms" -> "Terms of service"
        else -> "FAQ"
    }
    Scaffold(topBar = { AppTopBar(title = title, onBack = onBack) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp)) {
            when (type) {
                "privacy" -> LegalBody(
                    listOf(
                        "What we store" to "Resumes, cover letters, photos and signatures you create are stored locally on your device. Files you export are saved to your device storage.",
                        "AI features" to "AI drafting, resume import and cover letter generation run entirely on your device. Nothing you write is sent to a server.",
                        "Onboarding" to "During onboarding you may tell us how you found the app. This answer is stored only on your device.",
                        "Your control" to "Delete any document at any time. Uninstalling the app removes all of your data.",
                    ),
                )
                "terms" -> LegalBody(
                    listOf(
                        "Using the app" to "You may use Resume Builder to create documents for personal and professional use. You're responsible for the accuracy of the information you include.",
                        "AI-generated content" to "AI suggestions are provided as-is. Review and edit generated content before sharing it with employers.",
                        "Your content" to "You own everything you create. We don't claim rights to your resumes or cover letters.",
                        "Availability" to "Features may change over time. We aim to keep your locally stored documents accessible at all times.",
                    ),
                )
                else -> faq.forEach { (q, a) -> FaqItem(q, a) }
            }
        }
    }
}

@Composable
private fun LegalBody(sections: List<Pair<String, String>>) {
    sections.forEach { (heading, body) ->
        Text(heading, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
