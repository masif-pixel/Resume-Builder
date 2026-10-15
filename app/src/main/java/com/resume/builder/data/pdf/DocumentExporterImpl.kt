package com.resume.builder.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.util.LruCache
import com.resume.builder.domain.model.CoverLetter
import com.resume.builder.domain.model.FileCategory
import com.resume.builder.domain.model.Resume
import com.resume.builder.domain.model.SavedFile
import com.resume.builder.domain.model.TemplateCatalog
import com.resume.builder.domain.repository.SavedFileRepository
import com.resume.builder.domain.usecase.DocumentExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class DocumentExporterImpl(
    private val context: Context,
    private val renderer: ResumeRenderer,
    private val savedFiles: SavedFileRepository,
) : DocumentExporter {

    private val thumbnails = LruCache<String, Bitmap>(24)
    private val thumbnailLock = Mutex()

    override suspend fun exportResume(resume: Resume): SavedFile = withContext(Dispatchers.Default) {
        val temp = tempFile()
        renderer.renderPdf(resume, temp)
        savedFiles.register(temp, "${fileBaseName(resume.content.contact.fullName, resume.title)}.pdf", PDF_MIME, FileCategory.RESUME)
    }

    override suspend fun exportCoverLetter(letter: CoverLetter, resume: Resume?): SavedFile = withContext(Dispatchers.Default) {
        val temp = tempFile()
        renderer.renderCoverLetterPdf(letter, resume, temp)
        val base = fileBaseName(resume?.content?.contact?.fullName.orEmpty(), letter.title)
        savedFiles.register(temp, "$base - Cover Letter.pdf", PDF_MIME, FileCategory.COVER_LETTER)
    }

    override suspend fun previewResume(resume: Resume, widthPx: Int, maxPages: Int): List<Bitmap> = withContext(Dispatchers.Default) {
        renderer.renderBitmaps(resume, widthPx, maxPages)
    }

    override suspend fun previewCoverLetter(letter: CoverLetter, resume: Resume?, widthPx: Int): List<Bitmap> =
        withContext(Dispatchers.Default) { renderer.renderCoverLetterBitmaps(letter, resume, widthPx) }

    override suspend fun templateThumbnail(templateId: String, widthPx: Int): Bitmap = withContext(Dispatchers.Default) {
        val key = "$templateId@$widthPx"
        thumbnails.get(key) ?: thumbnailLock.withLock {
            thumbnails.get(key) ?: run {
                val index = TemplateCatalog.all.indexOfFirst { it.id == templateId }
                val assets = RenderAssets(ResumeRenderer.samplePortrait(index), null)
                renderer.renderBitmaps(SampleResumes.preview(templateId), widthPx, maxPages = 1, assets = assets)
                    .first()
                    .also { thumbnails.put(key, it) }
            }
        }
    }

    private fun tempFile(): File = File(context.cacheDir, "render_${UUID.randomUUID()}.pdf")

    private fun fileBaseName(name: String, title: String): String =
        listOf(name, title).filter { it.isNotBlank() }.distinct().joinToString(" - ").ifBlank { "Resume" }

    private companion object {
        const val PDF_MIME = "application/pdf"
    }
}
