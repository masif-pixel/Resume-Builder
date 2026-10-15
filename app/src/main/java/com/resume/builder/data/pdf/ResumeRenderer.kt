package com.resume.builder.data.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.resume.builder.domain.model.CoverLetter
import com.resume.builder.domain.model.Resume
import java.io.File

typealias DrawOp = (Canvas) -> Unit

class RenderAssets(val photo: Bitmap?, val signature: Bitmap?)

/**
 * Draws resumes and cover letters with the Android Canvas API. The same draw operations feed both
 * the on-screen preview (bitmaps) and the exported PDF, so what users see is exactly what they save.
 */
class ResumeRenderer {

    fun renderPdf(resume: Resume, output: File, assets: RenderAssets = loadAssets(resume)) {
        val engine = LayoutEngine(resume, assets)
        writePdf(engine.resumePages(), engine.pageW, engine.pageH, output)
    }

    fun renderBitmaps(
        resume: Resume,
        widthPx: Int,
        maxPages: Int = MAX_PAGES,
        assets: RenderAssets = loadAssets(resume),
    ): List<Bitmap> {
        val engine = LayoutEngine(resume, assets)
        return engine.resumePages().take(maxPages).map { toBitmap(it, widthPx, engine.pageW, engine.pageH) }
    }

    fun renderCoverLetterPdf(letter: CoverLetter, resume: Resume?, output: File) {
        val engine = LayoutEngine(resume ?: Resume(title = letter.title), RenderAssets(null, null))
        writePdf(engine.coverLetterPages(letter), engine.pageW, engine.pageH, output)
    }

    fun renderCoverLetterBitmaps(letter: CoverLetter, resume: Resume?, widthPx: Int): List<Bitmap> {
        val engine = LayoutEngine(resume ?: Resume(title = letter.title), RenderAssets(null, null))
        return engine.coverLetterPages(letter).take(MAX_PAGES).map { toBitmap(it, widthPx, engine.pageW, engine.pageH) }
    }

    private fun writePdf(pages: List<List<DrawOp>>, width: Int, height: Int, output: File) {
        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, ops ->
                val page = document.startPage(PdfDocument.PageInfo.Builder(width, height, index + 1).create())
                ops.forEach { it(page.canvas) }
                document.finishPage(page)
            }
            output.parentFile?.mkdirs()
            output.outputStream().use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }

    private fun toBitmap(ops: List<DrawOp>, widthPx: Int, pageW: Int, pageH: Int): Bitmap {
        val heightPx = (widthPx.toFloat() * pageH / pageW).toInt()
        val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        canvas.scale(widthPx.toFloat() / pageW, widthPx.toFloat() / pageW)
        ops.forEach { it(canvas) }
        return bitmap
    }

    companion object {
        const val MAX_PAGES = 10

        fun loadAssets(resume: Resume): RenderAssets = RenderAssets(
            photo = resume.content.contact.photoPath?.let { decodeSampled(it, 640) },
            signature = resume.content.signature.imagePath?.let { decodeSampled(it, 800) },
        )

        fun decodeSampled(path: String, maxDim: Int): Bitmap? {
            val file = File(path)
            if (!file.exists()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(path, bounds)
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= maxDim && bounds.outHeight / (sample * 2) >= maxDim) sample *= 2
            return BitmapFactory.decodeFile(path, BitmapFactory.Options().apply { inSampleSize = sample })
        }

        /** Friendly placeholder portrait used by template thumbnails. */
        fun samplePortrait(seed: Int): Bitmap {
            val size = 320
            val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val palette = listOf(0xFFB9C6E8, 0xFFE8D3B9, 0xFFC7E3D4, 0xFFE3C7DE).map { it.toInt() }
            val skin = listOf(0xFFF1C7A5, 0xFFD9A47F, 0xFFF3D2B8, 0xFFB98463).map { it.toInt() }
            val hair = listOf(0xFF3A2A20, 0xFF6B4A2E, 0xFF1E1E24, 0xFF8A5A3B).map { it.toInt() }
            val i = seed.mod(palette.size)
            canvas.drawColor(palette[i])
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            paint.color = 0xFF2F3A4F.toInt()
            canvas.drawOval(RectF(40f, 215f, 280f, 420f), paint)
            paint.color = Color.WHITE
            val collar = Path().apply { moveTo(130f, 222f); lineTo(160f, 280f); lineTo(190f, 222f); close() }
            canvas.drawPath(collar, paint)
            paint.color = skin[i]
            canvas.drawRect(138f, 180f, 182f, 232f, paint)
            canvas.drawOval(RectF(100f, 70f, 220f, 205f), paint)
            paint.color = hair[i]
            canvas.drawArc(RectF(96f, 58f, 224f, 170f), 180f, 180f, true, paint)
            return bitmap
        }
    }
}
