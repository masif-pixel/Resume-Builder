package com.resume.builder.presentation.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.resume.builder.domain.model.AiResumeRequest
import com.resume.builder.domain.model.ResumeLanguage
import com.resume.builder.domain.usecase.GenerateAiResumeUseCase
import com.resume.builder.presentation.components.AiOrb
import com.resume.builder.presentation.components.AppTopBar
import com.resume.builder.presentation.components.DropdownField
import com.resume.builder.presentation.components.FormCard
import com.resume.builder.presentation.components.GradientButton
import com.resume.builder.presentation.components.LoadingDialog
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

sealed interface AiResumeEvent {
    data class Created(val resumeId: String) : AiResumeEvent
    data class Error(val message: String) : AiResumeEvent
}

class AiResumeViewModel(
    private val generate: GenerateAiResumeUseCase,
) : ViewModel() {

    private val _generating = MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating.asStateFlow()

    private val _events = Channel<AiResumeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var job: Job? = null

    fun generate(background: String, targetJob: String, language: String) {
        job = viewModelScope.launch {
            _generating.value = true
            generate(AiResumeRequest(background = background, targetJob = targetJob, language = language))
                .onSuccess { _events.send(AiResumeEvent.Created(it.id)) }
                .onFailure { _events.send(AiResumeEvent.Error(it.message ?: "Couldn't generate the resume")) }
            _generating.value = false
        }
    }

    fun cancel() {
        job?.cancel()
        _generating.value = false
    }
}

private const val MIN_BACKGROUND = 15
private const val MAX_TEXT = 5000

private const val EXAMPLE_BACKGROUND =
    "I'm Sara Khan from Lahore. I studied BS Computer Science at FAST (2016-2020). Worked at Arbisoft as a " +
        "frontend developer 2020-2022 building React dashboards, then at Careem since 2022 as a senior engineer on the " +
        "rider app where I led the payments revamp and cut checkout drop-offs. Skills: React, TypeScript, Kotlin, GraphQL, " +
        "testing, mentoring. I speak English, Urdu and some Arabic."

@Composable
fun AiResumeScreen(
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: AiResumeViewModel = koinViewModel(),
) {
    val generating by viewModel.generating.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var language by rememberSaveable { mutableStateOf(ResumeLanguage.ENGLISH.label) }
    var background by rememberSaveable { mutableStateOf("") }
    var target by rememberSaveable { mutableStateOf("") }
    var showTips by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is AiResumeEvent.Created -> onCreated(event.resumeId)
                is AiResumeEvent.Error -> snackbar.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = { AppTopBar(title = "AI resume", subtitle = "Describe yourself — we'll structure the rest", onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                GradientButton(
                    text = "Generate resume",
                    icon = Icons.Rounded.AutoAwesome,
                    enabled = background.trim().length >= MIN_BACKGROUND,
                    onClick = { viewModel.generate(background, target, language) },
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FormCard(title = "Resume language") {
                DropdownField(label = "Language", options = ResumeLanguage.entries.map { it.label }, selected = language, optionLabel = { it }, onSelect = { language = it })
            }
            FormCard(title = "Your background", subtitle = "Experience, education, skills and achievements — plain sentences are fine") {
                OutlinedTextField(
                    value = background,
                    onValueChange = { if (it.length <= MAX_TEXT) background = it },
                    placeholder = { Text("Write about your experience, education, skills and work history…") },
                    minLines = 9,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    supportingText = {
                        Text(if (background.trim().length < MIN_BACKGROUND) "At least $MIN_BACKGROUND characters · ${background.length}/$MAX_TEXT" else "${background.length}/$MAX_TEXT")
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(onClick = { showTips = !showTips }) {
                        Text("Tips")
                        Icon(if (showTips) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null)
                    }
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = { background = EXAMPLE_BACKGROUND }) { Text("Try an example") }
                }
                AnimatedVisibility(showTips) {
                    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(top = 10.dp)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Lightbulb, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("When writing about yourself, include:", style = MaterialTheme.typography.titleSmall)
                            }
                            listOf(
                                "Your name and where you live",
                                "Jobs you've had, dates and what you achieved",
                                "Education — school, college, certificates",
                                "Skills and tools you're good at",
                                "Languages, awards or projects (optional)",
                            ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                        }
                    }
                }
            }
            FormCard(title = "Target job", subtitle = "Optional — paste a job posting to tailor keywords") {
                OutlinedTextField(
                    value = target,
                    onValueChange = { if (it.length <= MAX_TEXT) target = it },
                    placeholder = { Text("Paste the job posting or describe the role you want") },
                    minLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    supportingText = { Text("${target.length}/$MAX_TEXT") },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }

    if (generating) {
        LoadingDialog(title = "Building your resume", message = "Structuring sections and polishing wording…", onCancel = viewModel::cancel, visual = { AiOrb(size = 130.dp) })
    }
}
