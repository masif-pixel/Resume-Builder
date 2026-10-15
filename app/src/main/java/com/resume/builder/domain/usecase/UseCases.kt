package com.resume.builder.domain.usecase

import android.graphics.Bitmap
import android.net.Uri
import com.resume.builder.domain.model.AiResumeRequest
import com.resume.builder.domain.model.CoverLetter
import com.resume.builder.domain.model.CoverLetterRequest
import com.resume.builder.domain.model.Resume
import com.resume.builder.domain.model.ResumeContent
import com.resume.builder.domain.model.SavedFile
import com.resume.builder.domain.model.TemplateCatalog
import com.resume.builder.domain.model.newId
import com.resume.builder.domain.repository.AiRepository
import com.resume.builder.domain.repository.CoverLetterRepository
import com.resume.builder.domain.repository.ResumeRepository

/** Renders documents to PDF files / preview bitmaps. Implemented in the data layer. */
interface DocumentExporter {
    suspend fun exportResume(resume: Resume): SavedFile
    suspend fun exportCoverLetter(letter: CoverLetter, resume: Resume?): SavedFile
    suspend fun previewResume(resume: Resume, widthPx: Int, maxPages: Int = 10): List<Bitmap>
    suspend fun previewCoverLetter(letter: CoverLetter, resume: Resume?, widthPx: Int): List<Bitmap>
    suspend fun templateThumbnail(templateId: String, widthPx: Int): Bitmap
}

class CreateResumeUseCase(private val resumes: ResumeRepository) {
    suspend operator fun invoke(
        title: String,
        templateId: String = TemplateCatalog.DEFAULT_ID,
        content: ResumeContent = ResumeContent(),
    ): Resume {
        val resume = Resume(title = title.trim().ifBlank { "My Resume" }, templateId = templateId, content = content)
        resumes.upsert(resume)
        return resume
    }
}

class DuplicateResumeUseCase(private val resumes: ResumeRepository) {
    suspend operator fun invoke(resume: Resume): Resume {
        val now = System.currentTimeMillis()
        val copy = resume.copy(id = newId(), title = "${resume.title} (copy)", createdAt = now, updatedAt = now)
        resumes.upsert(copy)
        return copy
    }
}

class DeleteResumeUseCase(private val resumes: ResumeRepository) {
    suspend operator fun invoke(id: String) = resumes.delete(id)
}

class DeleteCoverLetterUseCase(private val letters: CoverLetterRepository) {
    suspend operator fun invoke(id: String) = letters.delete(id)
}

class GenerateAiResumeUseCase(
    private val ai: AiRepository,
    private val createResume: CreateResumeUseCase,
) {
    suspend operator fun invoke(request: AiResumeRequest): Result<Resume> =
        ai.generateResume(request).map { (title, content) -> createResume(title, content = content) }
}

class ImportResumeUseCase(
    private val ai: AiRepository,
    private val createResume: CreateResumeUseCase,
) {
    suspend operator fun invoke(pdf: Uri): Result<Resume> =
        ai.parseResumePdf(pdf).map { (title, content) -> createResume(title, content = content) }
}

class GenerateCoverLetterUseCase(
    private val ai: AiRepository,
    private val letters: CoverLetterRepository,
) {
    suspend operator fun invoke(request: CoverLetterRequest): Result<CoverLetter> = ai.generateCoverLetter(request).mapCatching { body ->
        val title = listOf(request.jobTitle, request.companyName).filter { it.isNotBlank() }.joinToString(" · ").ifBlank { "Cover Letter" }
        CoverLetter(
            title = title,
            body = body,
            resumeId = request.resume.id,
            companyName = request.companyName,
            jobTitle = request.jobTitle,
        ).also { letters.upsert(it) }
    }
}
