package com.resume.builder.presentation.home

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resume.builder.data.pdf.PdfToolsEngine
import com.resume.builder.domain.model.Resume
import com.resume.builder.domain.model.SavedFile
import com.resume.builder.domain.repository.ResumeRepository
import com.resume.builder.domain.usecase.CreateResumeUseCase
import com.resume.builder.domain.usecase.DeleteResumeUseCase
import com.resume.builder.domain.usecase.DocumentExporter
import com.resume.builder.domain.usecase.DuplicateResumeUseCase
import com.resume.builder.domain.usecase.ImportResumeUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface HomeEvent {
    data class OpenEditor(val resumeId: String) : HomeEvent
    data class Message(val text: String) : HomeEvent
    data class Deleted(val resume: Resume) : HomeEvent
    data class Exported(val file: SavedFile) : HomeEvent
}

class HomeViewModel(
    private val resumes: ResumeRepository,
    private val createResume: CreateResumeUseCase,
    private val duplicateResume: DuplicateResumeUseCase,
    private val deleteResume: DeleteResumeUseCase,
    private val importResume: ImportResumeUseCase,
    private val exporter: DocumentExporter,
    private val tools: PdfToolsEngine,
) : ViewModel() {

    private val _thumbnails = MutableStateFlow<Map<String, Pair<Long, Bitmap>>>(emptyMap())
    val thumbnails: StateFlow<Map<String, Pair<Long, Bitmap>>> = _thumbnails.asStateFlow()

    val resumeList: StateFlow<List<Resume>?> = resumes.observeResumes()
        .onEach { list -> refreshThumbnails(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _busyResumeId = MutableStateFlow<String?>(null)
    val busyResumeId: StateFlow<String?> = _busyResumeId.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var importJob: Job? = null

    private fun refreshThumbnails(list: List<Resume>) {
        viewModelScope.launch {
            val ids = list.map { it.id }.toSet()
            _thumbnails.update { current -> current.filterKeys { it in ids } }
            list.forEach { resume ->
                val cached = _thumbnails.value[resume.id]
                if (cached == null || cached.first != resume.updatedAt) {
                    val bitmap = runCatching { exporter.previewResume(resume, widthPx = 360, maxPages = 1).firstOrNull() }.getOrNull() ?: return@forEach
                    _thumbnails.update { it + (resume.id to (resume.updatedAt to bitmap)) }
                }
            }
        }
    }

    fun create(title: String) = viewModelScope.launch {
        val resume = createResume(title)
        _events.send(HomeEvent.OpenEditor(resume.id))
    }

    fun rename(resume: Resume, title: String) = viewModelScope.launch {
        resumes.upsert(resume.copy(title = title))
    }

    fun duplicate(resume: Resume) = viewModelScope.launch {
        duplicateResume(resume)
        _events.send(HomeEvent.Message("Duplicated “${resume.title}”"))
    }

    fun delete(resume: Resume) = viewModelScope.launch {
        deleteResume(resume.id)
        _events.send(HomeEvent.Deleted(resume))
    }

    fun undoDelete(resume: Resume) = viewModelScope.launch {
        resumes.upsert(resume, touch = true)
    }

    fun export(resume: Resume) = viewModelScope.launch {
        _busyResumeId.value = resume.id
        runCatching { exporter.exportResume(resume) }
            .onSuccess { _events.send(HomeEvent.Exported(it)) }
            .onFailure { _events.send(HomeEvent.Message("Couldn't create PDF: ${it.message}")) }
        _busyResumeId.value = null
    }

    fun import(uri: Uri) {
        importJob = viewModelScope.launch {
            val picked = runCatching { tools.describe(uri) }.getOrNull()
            if (picked != null && picked.sizeBytes > MAX_IMPORT_BYTES) {
                _events.send(HomeEvent.Message("Please pick a PDF smaller than 5 MB"))
                return@launch
            }
            _importing.value = true
            importResume(uri)
                .onSuccess { _events.send(HomeEvent.OpenEditor(it.id)) }
                .onFailure { _events.send(HomeEvent.Message(it.message ?: "Couldn't read that resume")) }
            _importing.value = false
        }
    }

    fun cancelImport() {
        importJob?.cancel()
        _importing.value = false
    }

    private companion object {
        const val MAX_IMPORT_BYTES = 5L * 1024 * 1024
    }
}
