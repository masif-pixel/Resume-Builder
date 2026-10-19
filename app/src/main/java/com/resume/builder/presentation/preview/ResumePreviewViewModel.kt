package com.resume.builder.presentation.preview

import android.graphics.Bitmap
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resume.builder.R
import com.resume.builder.presentation.components.UiText
import com.resume.builder.domain.model.Resume
import com.resume.builder.domain.model.ResumeStyle
import com.resume.builder.domain.model.SavedFile
import com.resume.builder.domain.model.TemplateCatalog
import com.resume.builder.domain.repository.ResumeRepository
import com.resume.builder.domain.usecase.DocumentExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class CustomizeCategory(@StringRes val label: Int) {
    TEMPLATE(R.string.customize_template), COLORS(R.string.customize_colors), FONT(R.string.customize_font), SIZE(R.string.customize_size), SPACING(R.string.customize_spacing),
    LAYOUT(R.string.customize_layout), PHOTO(R.string.customize_photo), CONTACT(R.string.customize_contact), LANGUAGE(R.string.customize_language),
}

sealed interface PreviewEvent {
    data class Exported(val file: SavedFile) : PreviewEvent
    data class Error(val message: UiText) : PreviewEvent
}

@OptIn(FlowPreview::class)
class ResumePreviewViewModel(
    private val resumeId: String,
    private val resumes: ResumeRepository,
    private val exporter: DocumentExporter,
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val _resume = MutableStateFlow<Resume?>(null)
    val resume: StateFlow<Resume?> = _resume.asStateFlow()

    private val _pages = MutableStateFlow<List<Bitmap>>(emptyList())
    val pages: StateFlow<List<Bitmap>> = _pages.asStateFlow()

    private val _rendering = MutableStateFlow(true)
    val rendering: StateFlow<Boolean> = _rendering.asStateFlow()

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    private val _thumbnails = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val thumbnails: StateFlow<Map<String, Bitmap>> = _thumbnails.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _events = Channel<PreviewEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private val undoStack = ArrayDeque<Pair<String, ResumeStyle>>()
    private var lastUndoRecord = 0L
    private var saveJob: Job? = null
    private var dirty = false

    init {
        viewModelScope.launch {
            resumes.observeResume(resumeId).collect { stored ->
                // Keep local, not-yet-persisted edits; otherwise follow the database (e.g. content edits).
                if (!dirty) _resume.value = stored
            }
        }
        viewModelScope.launch {
            _resume.filterNotNull().distinctUntilChanged().debounce(180).collectLatest { value ->
                _rendering.value = true
                runCatching { exporter.previewResume(value, PAGE_WIDTH_PX) }
                    .onSuccess { _pages.value = it }
                    .onFailure { _events.send(PreviewEvent.Error(UiText.Res(R.string.error_preview_failed, it.message.orEmpty()))) }
                _rendering.value = false
            }
        }
        viewModelScope.launch {
            TemplateCatalog.all.forEach { spec ->
                runCatching { exporter.templateThumbnail(spec.id, 240) }.onSuccess { bmp -> _thumbnails.update { it + (spec.id to bmp) } }
            }
        }
    }

    private fun mutate(transform: (Resume) -> Resume) {
        val current = _resume.value ?: return
        val next = transform(current)
        if (next == current) return
        val now = System.currentTimeMillis()
        if (now - lastUndoRecord > UNDO_COALESCE_MS) {
            undoStack.addLast(current.templateId to current.style)
            if (undoStack.size > MAX_UNDO) undoStack.removeFirst()
            _canUndo.value = true
        }
        lastUndoRecord = now
        _resume.value = next
        dirty = true
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            persist()
        }
    }

    private suspend fun persist() {
        val value = _resume.value ?: return
        resumes.upsert(value)
        dirty = false
    }

    fun updateStyle(transform: (ResumeStyle) -> ResumeStyle) = mutate { it.copy(style = transform(it.style)) }

    fun setTemplate(templateId: String) = mutate { it.copy(templateId = templateId) }

    fun undo() {
        val (template, style) = undoStack.removeLastOrNull() ?: return
        _canUndo.value = undoStack.isNotEmpty()
        lastUndoRecord = 0L
        val current = _resume.value ?: return
        _resume.value = current.copy(templateId = template, style = style)
        dirty = true
        saveJob?.cancel()
        saveJob = viewModelScope.launch { delay(300); persist() }
    }

    fun reset(category: CustomizeCategory) = updateStyle { style ->
        val d = ResumeStyle()
        when (category) {
            CustomizeCategory.TEMPLATE -> style
            CustomizeCategory.COLORS -> style.copy(
                accentColor = null, sidebarColor = null, backgroundColor = null, nameColor = null,
                jobTitleColor = null, headingColor = null, textColor = null, photoBorderColor = null,
            )
            CustomizeCategory.FONT -> style.copy(font = null)
            CustomizeCategory.SIZE -> style.copy(nameSize = d.nameSize, jobTitleSize = d.jobTitleSize, headingSize = d.headingSize, bodySize = d.bodySize)
            CustomizeCategory.SPACING -> style.copy(
                marginHorizontal = d.marginHorizontal, marginVertical = d.marginVertical, sectionSpacing = d.sectionSpacing,
                headingSpacing = d.headingSpacing, lineSpacing = d.lineSpacing, letterSpacing = d.letterSpacing,
            )
            CustomizeCategory.LAYOUT -> style.copy(columnWidth = d.columnWidth, swapColumns = d.swapColumns, pageSize = d.pageSize)
            CustomizeCategory.PHOTO -> style.copy(showPhoto = true, photoSize = d.photoSize, photoCornerRadius = null, photoBorderWidth = null)
            CustomizeCategory.CONTACT -> style.copy(showContactIcons = true, contactOrder = d.contactOrder, hiddenContactFields = emptySet())
            CustomizeCategory.LANGUAGE -> style.copy(language = d.language)
        }
    }

    fun export() {
        val value = _resume.value ?: return
        viewModelScope.launch {
            _exporting.value = true
            if (dirty) persist()
            runCatching { exporter.exportResume(value) }
                .onSuccess { _events.send(PreviewEvent.Exported(it)) }
                .onFailure { _events.send(PreviewEvent.Error(UiText.Res(R.string.error_save_pdf, it.message.orEmpty()))) }
            _exporting.value = false
        }
    }

    override fun onCleared() {
        saveJob?.cancel()
        val value = _resume.value
        if (dirty && value != null) appScope.launch { resumes.upsert(value) }
    }

    private companion object {
        const val PAGE_WIDTH_PX = 1080
        const val UNDO_COALESCE_MS = 700L
        const val MAX_UNDO = 40
    }
}
