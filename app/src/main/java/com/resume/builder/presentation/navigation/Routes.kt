package com.resume.builder.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable data object Splash : Route
    @Serializable data object Onboarding : Route
    @Serializable data class Main(val tab: Int = 0) : Route
    @Serializable data class Editor(val resumeId: String) : Route
    @Serializable data class SectionEditor(val resumeId: String, val sectionId: String) : Route
    @Serializable data class Preview(val resumeId: String) : Route
    @Serializable data class TemplatePicker(val resumeId: String? = null) : Route
    @Serializable data object AiResume : Route
    @Serializable data class CoverLetterGenerate(val resumeId: String) : Route
    @Serializable data class CoverLetterEditor(val letterId: String? = null, val resumeId: String? = null) : Route
    @Serializable data class CoverLetterPreview(val letterId: String) : Route
    @Serializable data object PdfTools : Route
    @Serializable data class PdfTool(val tool: String) : Route
    @Serializable data class PdfViewer(val path: String, val title: String) : Route
    @Serializable data object Settings : Route
    @Serializable data class Legal(val type: String) : Route
}

object MainTab {
    const val RESUMES = 0
    const val COVER_LETTERS = 1
    const val TEMPLATES = 2
    const val SAVED = 3
}
