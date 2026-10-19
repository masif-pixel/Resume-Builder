package com.resume.builder.presentation.coverletter

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resume.builder.R
import com.resume.builder.presentation.components.UiText
import com.resume.builder.domain.model.CoverLetter
import com.resume.builder.domain.model.CoverLetterLength
import com.resume.builder.domain.model.CoverLetterRequest
import com.resume.builder.domain.model.CoverLetterTone
import com.resume.builder.domain.model.ImproveKind
import com.resume.builder.domain.model.Resume
import com.resume.builder.domain.model.SavedFile
import com.resume.builder.domain.repository.AiRepository
import com.resume.builder.domain.repository.CoverLetterRepository
import com.resume.builder.domain.repository.ResumeRepository
import com.resume.builder.domain.usecase.DocumentExporter
import com.resume.builder.domain.usecase.GenerateCoverLetterUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class GenerateForm(
    val language: String = "English",
    val jobTitle: String = "",
    val companyName: String = "",
    val hiringManager: String = "",
    val jobDescription: String = "",
    val tone: CoverLetterTone = CoverLetterTone.PROFESSIONAL,
    val length: CoverLetterLength = CoverLetterLength.MEDIUM,
    val instructions: String = "",
) {
    val canGenerate: Boolean get() = jobDescription.trim().length >= MIN_DESCRIPTION

    companion object {
        const val MIN_DESCRIPTION = 80
        const val MAX_DESCRIPTION = 5000
        const val MAX_INSTRUCTIONS = 500
    }
}

sealed interface CoverLetterEvent {
    data class Generated(val letterId: String) : CoverLetterEvent
    data class Saved(val letterId: String, val thenPreview: Boolean) : CoverLetterEvent
    data class Exported(val file: SavedFile) : CoverLetterEvent
    data class Message(val text: UiText) : CoverLetterEvent
}

class CoverLetterGenerateViewModel(
    private val resumeId: String,
    private val resumes: ResumeRepository,
    private val generate: GenerateCoverLetterUseCase,
) : ViewModel() {

    private val _form = MutableStateFlow(GenerateForm())
    val form: StateFlow<GenerateForm> = _form.asStateFlow()

    private val _resume = MutableStateFlow<Resume?>(null)
    val resume: StateFlow<Resume?> = _resume.asStateFlow()

    private val _generating = MutableStateFlow(false)
    val generating: StateFlow<Boolean> = _generating.asStateFlow()

    private val _events = Channel<CoverLetterEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var job: Job? = null

    init {
        viewModelScope.launch {
            val resume = resumes.getResume(resumeId)
            _resume.value = resume
            resume?.content?.contact?.jobTitle?.takeIf { it.isNotBlank() }?.let { title -> _form.update { it.copy(jobTitle = title) } }
        }
    }

    fun update(transform: (GenerateForm) -> GenerateForm) = _form.update(transform)

    fun generate() {
        val resume = _resume.value ?: return
        val form = _form.value
        job = viewModelScope.launch {
            _generating.value = true
            generate(
                CoverLetterRequest(
                    resume = resume, jobTitle = form.jobTitle, companyName = form.companyName, hiringManager = form.hiringManager,
                    jobDescription = form.jobDescription, tone = form.tone, length = form.length, language = form.language,
                    instructions = form.instructions,
                ),
            ).onSuccess { _events.send(CoverLetterEvent.Generated(it.id)) }
                .onFailure { _events.send(CoverLetterEvent.Message(UiText.Res(R.string.error_generation_failed))) }
            _generating.value = false
        }
    }

    fun cancel() {
        job?.cancel()
        _generating.value = false
    }
}

class CoverLetterEditorViewModel(
    letterId: String?,
    resumeId: String?,
    private val letters: CoverLetterRepository,
    private val resumes: ResumeRepository,
    private val ai: AiRepository,
) : ViewModel() {

    private val _letter = MutableStateFlow<CoverLetter?>(null)
    val letter: StateFlow<CoverLetter?> = _letter.asStateFlow()

    private val _aiBusy = MutableStateFlow(false)
    val aiBusy: StateFlow<Boolean> = _aiBusy.asStateFlow()

    private val _dirty = MutableStateFlow(false)
    val dirty: StateFlow<Boolean> = _dirty.asStateFlow()

    private val _events = Channel<CoverLetterEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            _letter.value = letterId?.let { letters.getLetter(it) } ?: run {
                val resume = resumeId?.let { resumes.getResume(it) }
                val name = resume?.content?.contact?.fullName.orEmpty()
                _dirty.value = true
                CoverLetter(
                    title = resume?.content?.contact?.jobTitle?.takeIf { it.isNotBlank() }?.let { "$it cover letter" } ?: "Cover letter",
                    resumeId = resume?.id,
                    jobTitle = resume?.content?.contact?.jobTitle.orEmpty(),
                    body = buildString {
                        appendLine("Dear Hiring Manager,")
                        appendLine()
                        appendLine("I'm excited to apply for the [Position] role at [Company]. ")
                        appendLine()
                        appendLine("In my recent role, I … (share one achievement that matches the job).")
                        appendLine()
                        appendLine("I'd welcome the chance to discuss how I can contribute to your team. Thank you for your time and consideration.")
                        appendLine()
                        appendLine("Sincerely,")
                        append(name.ifBlank { "[Your name]" })
                    },
                )
            }
        }
    }

    fun update(transform: (CoverLetter) -> CoverLetter) {
        _letter.update { it?.let(transform) }
        _dirty.value = true
    }

    fun save(thenPreview: Boolean = false) {
        val value = _letter.value ?: return
        viewModelScope.launch {
            letters.upsert(value)
            _dirty.value = false
            _events.send(CoverLetterEvent.Saved(value.id, thenPreview))
        }
    }

    fun improve() {
        val value = _letter.value ?: return
        viewModelScope.launch {
            _aiBusy.value = true
            ai.improveText(value.body, ImproveKind.COVER_LETTER, listOf(value.jobTitle, value.companyName).filter { it.isNotBlank() }.joinToString(" at "))
                .onSuccess { body -> update { it.copy(body = body) } }
                .onFailure { _events.send(CoverLetterEvent.Message(UiText.Res(R.string.error_ai_failed))) }
            _aiBusy.value = false
        }
    }
}

class CoverLetterPreviewViewModel(
    private val letterId: String,
    private val letters: CoverLetterRepository,
    private val resumes: ResumeRepository,
    private val exporter: DocumentExporter,
) : ViewModel() {

    private val _pages = MutableStateFlow<List<Bitmap>>(emptyList())
    val pages: StateFlow<List<Bitmap>> = _pages.asStateFlow()

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _events = Channel<CoverLetterEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var letter: CoverLetter? = null
    private var resume: Resume? = null

    init {
        viewModelScope.launch {
            val loaded = letters.getLetter(letterId) ?: return@launch
            letter = loaded
            _title.value = loaded.title
            resume = loaded.resumeId?.let { resumes.getResume(it) }
            _pages.value = exporter.previewCoverLetter(loaded, resume, 1080)
        }
    }

    fun export() {
        val value = letter ?: return
        viewModelScope.launch {
            _exporting.value = true
            runCatching { exporter.exportCoverLetter(value, resume) }
                .onSuccess { _events.send(CoverLetterEvent.Exported(it)) }
                .onFailure { _events.send(CoverLetterEvent.Message(UiText.Res(R.string.error_save_pdf, it.message.orEmpty()))) }
            _exporting.value = false
        }
    }
}
