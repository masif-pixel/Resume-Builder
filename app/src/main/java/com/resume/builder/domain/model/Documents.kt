package com.resume.builder.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CoverLetter(
    val id: String = newId(),
    val title: String,
    val body: String,
    val resumeId: String? = null,
    val companyName: String = "",
    val jobTitle: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

enum class FileCategory(val label: String) {
    RESUME("Resume"), COVER_LETTER("Cover Letter"), PDF_TOOLS("PDF Tools"), IMAGES("Images"),
}

data class SavedFile(
    val id: String = newId(),
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val mimeType: String,
    val category: FileCategory,
    val createdAt: Long = System.currentTimeMillis(),
)

enum class ThemeMode(val label: String) { LIGHT("Light"), DARK("Dark"), SYSTEM("Follow system") }

enum class CoverLetterTone(val label: String) {
    PROFESSIONAL("Professional"), CONFIDENT("Confident"), FRIENDLY("Friendly"), ENTHUSIASTIC("Enthusiastic"), FORMAL("Formal"),
}

enum class CoverLetterLength(val label: String, val words: String) {
    SHORT("Short", "180-230"), MEDIUM("Medium", "260-340"), LONG("Long", "380-450"),
}

data class CoverLetterRequest(
    val resume: Resume,
    val jobTitle: String,
    val companyName: String,
    val hiringManager: String,
    val jobDescription: String,
    val tone: CoverLetterTone,
    val length: CoverLetterLength,
    val language: String,
    val instructions: String,
)

data class AiResumeRequest(
    val background: String,
    val targetJob: String,
    val language: String,
)

/** Kind of text the AI writing assistant should polish. */
enum class ImproveKind { SUMMARY, BULLETS, COVER_LETTER }
