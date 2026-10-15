package com.resume.builder.presentation.coverletter

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resume.builder.domain.model.CoverLetterLength
import com.resume.builder.domain.model.CoverLetterTone
import com.resume.builder.domain.model.ResumeLanguage
import com.resume.builder.domain.model.SavedFile
import com.resume.builder.presentation.components.AiOrb
import com.resume.builder.presentation.components.AppTopBar
import com.resume.builder.presentation.components.ChoiceChips
import com.resume.builder.presentation.components.ConfirmDialog
import com.resume.builder.presentation.components.DropdownField
import com.resume.builder.presentation.components.ExportedDialog
import com.resume.builder.presentation.components.FormCard
import com.resume.builder.presentation.components.GradientButton
import com.resume.builder.presentation.components.LoadingDialog
import com.resume.builder.presentation.components.PagesPreview
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun CoverLetterGenerateScreen(
    resumeId: String,
    onBack: () -> Unit,
    onGenerated: (String) -> Unit,
    viewModel: CoverLetterGenerateViewModel = koinViewModel { parametersOf(resumeId) },
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val resume by viewModel.resume.collectAsStateWithLifecycle()
    val generating by viewModel.generating.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CoverLetterEvent.Generated -> onGenerated(event.letterId)
                is CoverLetterEvent.Message -> snackbar.showSnackbar(event.text)
                else -> Unit
            }
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "AI cover letter", subtitle = resume?.let { "Based on “${it.title}”" }, onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                GradientButton(
                    text = "Generate cover letter",
                    icon = Icons.Rounded.AutoAwesome,
                    onClick = viewModel::generate,
                    enabled = form.canGenerate && resume != null,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FormCard(title = "The role") {
                OutlinedTextField(
                    value = form.jobTitle, onValueChange = { v -> viewModel.update { it.copy(jobTitle = v) } }, label = { Text("Job title") },
                    singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = form.companyName, onValueChange = { v -> viewModel.update { it.copy(companyName = v) } }, label = { Text("Company") },
                    singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = form.hiringManager, onValueChange = { v -> viewModel.update { it.copy(hiringManager = v) } }, label = { Text("Hiring manager (optional)") },
                    singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                )
            }
            FormCard(title = "Job description", subtitle = "Paste the posting — the more detail, the better the match") {
                OutlinedTextField(
                    value = form.jobDescription,
                    onValueChange = { v -> if (v.length <= GenerateForm.MAX_DESCRIPTION) viewModel.update { it.copy(jobDescription = v) } },
                    placeholder = { Text("Responsibilities, requirements, about the company…") },
                    minLines = 7,
                    shape = RoundedCornerShape(16.dp),
                    supportingText = {
                        val n = form.jobDescription.trim().length
                        Text(
                            if (n < GenerateForm.MIN_DESCRIPTION) "At least ${GenerateForm.MIN_DESCRIPTION} characters · $n/${GenerateForm.MAX_DESCRIPTION}" else "$n/${GenerateForm.MAX_DESCRIPTION}",
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            FormCard(title = "Style") {
                DropdownField(
                    label = "Language",
                    options = ResumeLanguage.entries.map { it.label },
                    selected = form.language,
                    optionLabel = { it },
                    onSelect = { v -> viewModel.update { it.copy(language = v) } },
                )
                Spacer(Modifier.height(14.dp))
                Text("Tone", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                ChoiceChips(options = CoverLetterTone.entries, selected = form.tone, label = { it.label }, onSelect = { v -> viewModel.update { it.copy(tone = v) } })
                Spacer(Modifier.height(12.dp))
                Text("Length", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                ChoiceChips(options = CoverLetterLength.entries, selected = form.length, label = { "${it.label} · ${it.words} words" }, onSelect = { v -> viewModel.update { it.copy(length = v) } })
            }
            FormCard(title = "Additional instructions", subtitle = "Optional") {
                OutlinedTextField(
                    value = form.instructions,
                    onValueChange = { v -> if (v.length <= GenerateForm.MAX_INSTRUCTIONS) viewModel.update { it.copy(instructions = v) } },
                    placeholder = { Text("e.g. Emphasise my leadership and the fintech project") },
                    minLines = 3,
                    shape = RoundedCornerShape(16.dp),
                    supportingText = { Text("${form.instructions.length}/${GenerateForm.MAX_INSTRUCTIONS}") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (generating) {
        LoadingDialog(title = "Writing your letter", message = "Matching your experience to the role…", onCancel = viewModel::cancel, visual = { AiOrb(size = 120.dp) })
    }
}

@Composable
fun CoverLetterEditorScreen(
    letterId: String?,
    resumeId: String?,
    onBack: () -> Unit,
    onPreview: (String) -> Unit,
    viewModel: CoverLetterEditorViewModel = koinViewModel(key = "letter-${letterId ?: "new-$resumeId"}") { parametersOf(letterId, resumeId) },
) {
    val letter by viewModel.letter.collectAsStateWithLifecycle()
    val aiBusy by viewModel.aiBusy.collectAsStateWithLifecycle()
    val dirty by viewModel.dirty.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmLeave by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CoverLetterEvent.Saved -> if (event.thenPreview) onPreview(event.letterId) else snackbar.showSnackbar("Cover letter saved")
                is CoverLetterEvent.Message -> snackbar.showSnackbar(event.text)
                else -> Unit
            }
        }
    }
    BackHandler(enabled = dirty) { confirmLeave = true }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Edit cover letter",
                onBack = { if (dirty) confirmLeave = true else onBack() },
                actions = {
                    IconButton(onClick = viewModel::improve, enabled = !aiBusy) {
                        if (aiBusy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp) else Icon(Icons.Rounded.AutoAwesome, "Polish with AI")
                    }
                    TextButton(onClick = { viewModel.save() }, enabled = dirty) {
                        Icon(Icons.Rounded.Save, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Save")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                GradientButton(
                    text = "Save & preview PDF",
                    icon = Icons.Rounded.Visibility,
                    onClick = { viewModel.save(thenPreview = true) },
                    enabled = letter != null,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        },
    ) { padding ->
        val current = letter
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            OutlinedTextField(
                value = current.title, onValueChange = { v -> viewModel.update { it.copy(title = v) } }, label = { Text("Letter name") },
                singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = current.companyName, onValueChange = { v -> viewModel.update { it.copy(companyName = v) } }, label = { Text("Company (shown above the letter)") },
                singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = current.body,
                onValueChange = { v -> viewModel.update { it.copy(body = v) } },
                label = { Text("Letter") },
                minLines = 16,
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                supportingText = { Text("${current.body.split(Regex("\\s+")).count { it.isNotBlank() }} words · replace any [placeholders] before sending") },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (confirmLeave) {
        ConfirmDialog(
            title = "Save changes?",
            message = "You have unsaved edits to this cover letter.",
            confirmText = "Save",
            onConfirm = { confirmLeave = false; viewModel.save(); onBack() },
            onDismiss = { confirmLeave = false; onBack() },
        )
    }
}

@Composable
fun CoverLetterPreviewScreen(
    letterId: String,
    onBack: () -> Unit,
    onOpenSaved: () -> Unit,
    viewModel: CoverLetterPreviewViewModel = koinViewModel(key = "letter-preview-$letterId") { parametersOf(letterId) },
) {
    val pages by viewModel.pages.collectAsStateWithLifecycle()
    val exporting by viewModel.exporting.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var exported by remember { mutableStateOf<SavedFile?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CoverLetterEvent.Exported -> exported = event.file
                is CoverLetterEvent.Message -> snackbar.showSnackbar(event.text)
                else -> Unit
            }
        }
    }
    Scaffold(
        topBar = {
            AppTopBar(
                title = title.ifBlank { "Cover letter" },
                onBack = onBack,
                actions = {
                    Button(onClick = viewModel::export, enabled = !exporting && pages.isNotEmpty(), contentPadding = PaddingValues(horizontal = 14.dp), modifier = Modifier.padding(end = 8.dp)) {
                        if (exporting) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White) else Icon(Icons.Rounded.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save PDF")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            if (pages.isEmpty()) CircularProgressIndicator(Modifier.align(Alignment.Center)) else PagesPreview(pages, Modifier.fillMaxSize())
        }
    }
    exported?.let { file ->
        ExportedDialog(file = file, onDismiss = { exported = null }, onViewSaved = { exported = null; onOpenSaved() }, message = "Your cover letter is ready.")
    }
}
