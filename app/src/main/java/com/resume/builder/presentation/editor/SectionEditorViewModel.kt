package com.resume.builder.presentation.editor

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resume.builder.data.local.ImageStore
import com.resume.builder.domain.model.ContactInfo
import com.resume.builder.domain.model.ImproveKind
import com.resume.builder.domain.model.Resume
import com.resume.builder.domain.model.ResumeLanguage
import com.resume.builder.domain.model.ResumeSection
import com.resume.builder.domain.model.SectionItem
import com.resume.builder.domain.model.SignatureInfo
import com.resume.builder.domain.repository.AiRepository
import com.resume.builder.domain.repository.ResumeRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class SectionEditorViewModel(
    private val resumeId: String,
    val sectionId: String,
    private val resumes: ResumeRepository,
    private val ai: AiRepository,
    private val images: ImageStore,
    private val appScope: CoroutineScope,
) : ViewModel() {

    private val _resume = MutableStateFlow<Resume?>(null)
    val resume: StateFlow<Resume?> = _resume.asStateFlow()

    private val _aiBusy = MutableStateFlow(false)
    val aiBusy: StateFlow<Boolean> = _aiBusy.asStateFlow()

    private val _messages = Channel<String>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private var saveJob: Job? = null
    private var dirty = false

    init {
        viewModelScope.launch { _resume.value = resumes.getResume(resumeId) }
    }

    val section: ResumeSection? get() = _resume.value?.content?.sectionById(sectionId)

    private fun update(transform: (Resume) -> Resume) {
        val current = _resume.value ?: return
        _resume.value = transform(current)
        dirty = true
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(450)
            persist()
        }
    }

    private suspend fun persist() {
        val value = _resume.value ?: return
        if (!dirty) return
        dirty = false
        resumes.upsert(value)
    }

    private fun updateSection(transform: (ResumeSection) -> ResumeSection) = update { resume ->
        resume.copy(content = resume.content.copy(sections = resume.content.sections.map { if (it.id == sectionId) transform(it) else it }))
    }

    fun updateContact(transform: (ContactInfo) -> ContactInfo) = update { it.copy(content = it.content.copy(contact = transform(it.content.contact))) }

    fun updateSummary(text: String) = update { it.copy(content = it.content.copy(summary = text)) }

    fun updateSignature(transform: (SignatureInfo) -> SignatureInfo) = update { it.copy(content = it.content.copy(signature = transform(it.content.signature))) }

    fun renameSection(title: String) = updateSection { it.copy(title = ResumeLanguage.normalizeTitle(it.type, title)) }

    fun upsertItem(item: SectionItem) = updateSection { section ->
        val exists = section.items.any { it.id == item.id }
        section.copy(items = if (exists) section.items.map { if (it.id == item.id) item else it } else section.items + item)
    }

    fun quickAdd(raw: String) {
        val titles = raw.split(',', '\n', ';').map { it.trim() }.filter { it.isNotEmpty() }
        if (titles.isEmpty()) return
        updateSection { section ->
            val existing = section.items.map { it.title.lowercase() }.toSet()
            section.copy(items = section.items + titles.filter { it.lowercase() !in existing }.distinct().map { SectionItem(title = it) })
        }
    }

    fun deleteItem(id: String) = updateSection { section -> section.copy(items = section.items.filterNot { it.id == id }) }

    fun moveItem(id: String, delta: Int) = updateSection { section ->
        val index = section.items.indexOfFirst { it.id == id }
        val target = index + delta
        if (index < 0 || target !in section.items.indices) section else section.copy(items = section.items.toMutableList().apply { add(target, removeAt(index)) })
    }

    fun setPhoto(uri: Uri) = viewModelScope.launch {
        runCatching { images.savePhoto(resumeId, uri) }
            .onSuccess { path ->
                val old = _resume.value?.content?.contact?.photoPath
                updateContact { it.copy(photoPath = path) }
                images.delete(old)
            }
            .onFailure { _messages.send("Couldn't use that image") }
    }

    fun removePhoto() {
        val old = _resume.value?.content?.contact?.photoPath
        updateContact { it.copy(photoPath = null) }
        viewModelScope.launch { images.delete(old) }
    }

    fun saveSignature(bitmap: Bitmap) = viewModelScope.launch {
        val path = images.saveSignature(resumeId, bitmap)
        val old = _resume.value?.content?.signature?.imagePath
        updateSignature { it.copy(imagePath = path) }
        images.delete(old)
    }

    fun clearSignature() {
        val old = _resume.value?.content?.signature?.imagePath
        updateSignature { it.copy(imagePath = null) }
        viewModelScope.launch { images.delete(old) }
    }

    /** Runs the AI writing assistant and hands the result to [onResult]. */
    fun improve(kind: ImproveKind, text: String, onResult: (String) -> Unit) {
        val contact = _resume.value?.content?.contact
        val context = listOfNotNull(
            contact?.jobTitle?.takeIf { it.isNotBlank() }?.let { "Target role: $it" },
            section?.title?.let { "Section: $it" },
        ).joinToString(". ")
        viewModelScope.launch {
            _aiBusy.value = true
            ai.improveText(text, kind, context)
                .onSuccess(onResult)
                .onFailure { _messages.send(it.message ?: "AI request failed") }
            _aiBusy.value = false
        }
    }

    override fun onCleared() {
        saveJob?.cancel()
        if (dirty) {
            val value = _resume.value ?: return
            appScope.launch { resumes.upsert(value) }
        }
    }
}
