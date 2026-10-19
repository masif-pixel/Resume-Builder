package com.resume.builder.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resume.builder.domain.model.CustomLayout
import com.resume.builder.domain.model.Resume
import com.resume.builder.domain.model.ResumeLanguage
import com.resume.builder.domain.model.ResumeSection
import com.resume.builder.domain.model.SectionType
import com.resume.builder.domain.repository.ResumeRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

class ResumeEditorViewModel(
    private val resumeId: String,
    private val resumes: ResumeRepository,
) : ViewModel() {

    private val _resume = MutableStateFlow<Resume?>(null)
    val resume: StateFlow<Resume?> = _resume.asStateFlow()

    private val _openSection = Channel<String>(Channel.BUFFERED)
    val openSection = _openSection.receiveAsFlow()

    init {
        viewModelScope.launch {
            resumes.observeResume(resumeId).collect { _resume.value = it }
        }
    }

    private fun update(transform: (Resume) -> Resume) {
        val current = _resume.value ?: return
        val next = transform(current)
        if (next == current) return
        _resume.value = next
        viewModelScope.launch { resumes.upsert(next) }
    }

    private fun updateSections(transform: (List<ResumeSection>) -> List<ResumeSection>) =
        update { it.copy(content = it.content.copy(sections = transform(it.content.sections))) }

    fun move(fromId: String, toId: String) = updateSections { sections ->
        val from = sections.indexOfFirst { it.id == fromId }
        val to = sections.indexOfFirst { it.id == toId }
        if (from < 0 || to < 0) sections else sections.toMutableList().apply { add(to, removeAt(from)) }
    }

    fun setVisible(sectionId: String, visible: Boolean) = updateSections { sections ->
        sections.map { if (it.id == sectionId) it.copy(visible = visible) else it }
    }

    fun addCustomSection(name: String, layout: CustomLayout) {
        val section = ResumeSection(type = SectionType.CUSTOM, title = name, customLayout = layout)
        updateSections { sections ->
            val signatureIndex = sections.indexOfFirst { it.type == SectionType.SIGNATURE }.takeIf { it >= 0 } ?: sections.size
            sections.toMutableList().apply { add(signatureIndex, section) }
        }
        viewModelScope.launch { _openSection.send(section.id) }
    }

    fun deleteSection(sectionId: String) = updateSections { sections -> sections.filterNot { it.id == sectionId && it.type == SectionType.CUSTOM } }

    fun rename(title: String) = update { it.copy(title = title) }

    fun setLanguage(language: ResumeLanguage) = update { it.copy(style = it.style.copy(language = language)) }
}
