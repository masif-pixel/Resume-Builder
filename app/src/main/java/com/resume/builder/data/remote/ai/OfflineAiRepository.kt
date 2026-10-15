package com.resume.builder.data.remote.ai

import android.net.Uri
import com.resume.builder.data.pdf.PdfTextExtractor
import com.resume.builder.domain.model.AiResumeRequest
import com.resume.builder.domain.model.CoverLetterRequest
import com.resume.builder.domain.model.ImproveKind
import com.resume.builder.domain.model.ResumeContent
import com.resume.builder.domain.repository.AiRepository

/** Writing assistant that runs entirely on-device, with no AI model or network calls. */
class OfflineAiRepository(private val textExtractor: PdfTextExtractor) : AiRepository {

    override suspend fun generateResume(request: AiResumeRequest): Result<Pair<String, ResumeContent>> =
        runCatching { OfflineResumeWriter.generateResume(request) }

    override suspend fun parseResumePdf(pdf: Uri): Result<Pair<String, ResumeContent>> = runCatching {
        val text = textExtractor.extractText(pdf)
        require(text.isNotBlank()) { "No selectable text found in this PDF" }
        ResumeTextParser.parse(text)
    }

    override suspend fun generateCoverLetter(request: CoverLetterRequest): Result<String> =
        runCatching { OfflineResumeWriter.generateCoverLetter(request) }

    override suspend fun improveText(text: String, kind: ImproveKind, context: String): Result<String> =
        runCatching { OfflineResumeWriter.improveText(text, kind, context) }
}
