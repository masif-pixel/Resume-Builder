package com.resume.builder.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.media.ExifInterface
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import kotlin.math.min

data class PickedFile(val uri: Uri, val name: String, val sizeBytes: Long, val isPdf: Boolean)

data class LocalPdf(val file: File, val name: String, val sizeBytes: Long, val pageCount: Int)

enum class ToolPageSize(val label: String) { A4("A4"), LETTER("Letter"), FIT("Fit to image") }
enum class ToolOrientation(val label: String) { AUTO("Auto"), PORTRAIT("Portrait"), LANDSCAPE("Landscape") }
enum class CompressionLevel(val label: String, val description: String, val scale: Float, val jpegQuality: Int) {
    LOW("Light", "Best quality, smaller savings", 1.6f, 82),
    MEDIUM("Balanced", "Great quality, good savings", 1.25f, 65),
    HIGH("Strong", "Smallest file, lower quality", 0.95f, 45),
}
enum class ImageFormat(val label: String, val extension: String, val mime: String) {
    JPG("JPG", "jpg", "image/jpeg"), PNG("PNG", "png", "image/png"),
}
enum class ImageQuality(val label: String, val scale: Float) { LOW("Low", 1f), MEDIUM("Medium", 2f), HIGH("High", 3f) }

/**
 * Local PDF utilities built on PdfRenderer + PdfDocument. Pages are rasterised, which keeps
 * everything on-device and dependency free (text in the output is not selectable).
 */
class PdfToolsEngine(private val context: Context) {

    private val rendererLock = Mutex()
    private val workDir: File get() = File(context.cacheDir, "pdf_tools").apply { mkdirs() }

    fun newOutputFile(extension: String = "pdf"): File = File(workDir, "${UUID.randomUUID()}.$extension")

    suspend fun describe(uri: Uri): PickedFile = withContext(Dispatchers.IO) {
        var name = "file"
        var size = 0L
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                name = cursor.getString(0) ?: name
                size = if (cursor.isNull(1)) 0L else cursor.getLong(1)
            }
        }
        val mime = context.contentResolver.getType(uri).orEmpty()
        PickedFile(uri, name, size, mime == "application/pdf" || name.endsWith(".pdf", ignoreCase = true))
    }

    suspend fun importPdf(uri: Uri): LocalPdf = withContext(Dispatchers.IO) {
        val picked = describe(uri)
        val target = newOutputFile()
        context.contentResolver.openInputStream(uri)?.use { input -> target.outputStream().use { input.copyTo(it) } }
            ?: error("Unable to open ${picked.name}")
        val pages = rendererLock.withLock { openRenderer(target).use { it.pageCount } }
        LocalPdf(target, picked.name, target.length(), pages)
    }

    suspend fun renderPage(file: File, index: Int, widthPx: Int): Bitmap = withContext(Dispatchers.IO) {
        rendererLock.withLock {
            openRenderer(file).use { renderer ->
                renderer.openPage(index).use { page ->
                    val height = (widthPx.toFloat() * page.height / page.width).toInt()
                    Bitmap.createBitmap(widthPx, height, Bitmap.Config.ARGB_8888).apply {
                        eraseColor(Color.WHITE)
                        page.render(this, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    }
                }
            }
        }
    }

    suspend fun merge(
        sources: List<PickedFile>,
        pageSize: ToolPageSize,
        orientation: ToolOrientation,
        marginMm: Float,
        output: File,
    ): Int = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        var pageNumber = 0
        try {
            sources.forEach { source ->
                if (source.isPdf) {
                    val local = importPdf(source.uri)
                    rasterizeInto(document, local.file, (0 until local.pageCount).toList(), 2f, null) { pageNumber++; pageNumber }
                    local.file.delete()
                } else {
                    val bitmap = decodeImage(source.uri) ?: return@forEach
                    addImagePage(document, bitmap, pageSize, orientation, marginMm, ++pageNumber)
                    bitmap.recycle()
                }
            }
            check(pageNumber > 0) { "Nothing to merge" }
            output.outputStream().use { document.writeTo(it) }
            pageNumber
        } finally {
            document.close()
        }
    }

    suspend fun compress(file: File, level: CompressionLevel, pageCount: Int, output: File) = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        try {
            var number = 0
            rasterizeInto(document, file, (0 until pageCount).toList(), level.scale, level.jpegQuality) { ++number }
            output.outputStream().use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }

    suspend fun extractPages(file: File, pages: List<Int>, output: File) = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        try {
            var number = 0
            rasterizeInto(document, file, pages.sorted(), 2f, null) { ++number }
            output.outputStream().use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }

    /** [relX], [relY] and [relWidth] are fractions of the page the signature is stamped at. */
    suspend fun sign(
        file: File,
        pageCount: Int,
        pageIndex: Int,
        signature: Bitmap,
        relX: Float,
        relY: Float,
        relWidth: Float,
        output: File,
    ) = withContext(Dispatchers.IO) {
        val document = PdfDocument()
        try {
            var number = 0
            rasterizeInto(document, file, (0 until pageCount).toList(), 2f, null, { index, canvas, w, h ->
                if (index == pageIndex) {
                    val sigW = w * relWidth
                    val sigH = sigW * signature.height / signature.width
                    val left = (w * relX - sigW / 2).coerceIn(0f, w - sigW)
                    val top = (h * relY - sigH / 2).coerceIn(0f, h - sigH)
                    canvas.drawBitmap(signature, null, RectF(left, top, left + sigW, top + sigH), Paint(Paint.FILTER_BITMAP_FLAG))
                }
            }) { ++number }
            output.outputStream().use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }

    suspend fun exportImages(file: File, pages: List<Int>, format: ImageFormat, quality: ImageQuality): List<File> =
        withContext(Dispatchers.IO) {
            rendererLock.withLock {
                openRenderer(file).use { renderer ->
                    pages.sorted().map { index ->
                        renderer.openPage(index).use { page ->
                            val bitmap = Bitmap.createBitmap((page.width * quality.scale).toInt(), (page.height * quality.scale).toInt(), Bitmap.Config.ARGB_8888)
                            bitmap.eraseColor(Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                            val out = newOutputFile(format.extension)
                            out.outputStream().use { stream ->
                                val compressFormat = if (format == ImageFormat.PNG) Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
                                bitmap.compress(compressFormat, 92, stream)
                            }
                            bitmap.recycle()
                            out
                        }
                    }
                }
            }
        }

    private suspend fun rasterizeInto(
        document: PdfDocument,
        file: File,
        pages: List<Int>,
        scale: Float,
        jpegQuality: Int?,
        overlay: ((Int, Canvas, Float, Float) -> Unit)? = null,
        nextNumber: () -> Int,
    ) {
        rendererLock.withLock {
            openRenderer(file).use { renderer ->
                pages.filter { it in 0 until renderer.pageCount }.forEach { index ->
                    renderer.openPage(index).use { page ->
                        val w = page.width
                        val h = page.height
                        var bitmap = Bitmap.createBitmap((w * scale).toInt().coerceAtLeast(1), (h * scale).toInt().coerceAtLeast(1), Bitmap.Config.ARGB_8888)
                        bitmap.eraseColor(Color.WHITE)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                        if (jpegQuality != null) {
                            val bytes = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, it) }.toByteArray()
                            bitmap.recycle()
                            bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        }
                        val outPage = document.startPage(PdfDocument.PageInfo.Builder(w, h, nextNumber()).create())
                        outPage.canvas.drawBitmap(bitmap, null, RectF(0f, 0f, w.toFloat(), h.toFloat()), Paint(Paint.FILTER_BITMAP_FLAG))
                        overlay?.invoke(index, outPage.canvas, w.toFloat(), h.toFloat())
                        document.finishPage(outPage)
                        bitmap.recycle()
                    }
                }
            }
        }
    }

    private fun addImagePage(
        document: PdfDocument,
        bitmap: Bitmap,
        pageSize: ToolPageSize,
        orientation: ToolOrientation,
        marginMm: Float,
        number: Int,
    ) {
        val landscape = when (orientation) {
            ToolOrientation.AUTO -> bitmap.width > bitmap.height
            ToolOrientation.PORTRAIT -> false
            ToolOrientation.LANDSCAPE -> true
        }
        val margin = marginMm * 72f / 25.4f
        val (baseW, baseH) = when (pageSize) {
            ToolPageSize.A4 -> 595 to 842
            ToolPageSize.LETTER -> 612 to 792
            ToolPageSize.FIT -> {
                val fit = 842f / maxOf(bitmap.width, bitmap.height)
                val w = (bitmap.width * fit + margin * 2).toInt()
                val h = (bitmap.height * fit + margin * 2).toInt()
                if (landscape == (w > h) || orientation == ToolOrientation.AUTO) w to h else h to w
            }
        }
        val (pageW, pageH) = if (pageSize != ToolPageSize.FIT && landscape) baseH to baseW else baseW to baseH
        val page = document.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, number).create())
        val availableW = pageW - margin * 2
        val availableH = pageH - margin * 2
        val ratio = min(availableW / bitmap.width, availableH / bitmap.height)
        val drawW = bitmap.width * ratio
        val drawH = bitmap.height * ratio
        val left = (pageW - drawW) / 2
        val top = (pageH - drawH) / 2
        page.canvas.drawColor(Color.WHITE)
        page.canvas.drawBitmap(bitmap, null, RectF(left, top, left + drawW, top + drawH), Paint(Paint.FILTER_BITMAP_FLAG))
        document.finishPage(page)
    }

    private fun decodeImage(uri: Uri): Bitmap? {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= 2000 || bounds.outHeight / (sample * 2) >= 2000) sample *= 2
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                when (ExifInterface(stream).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            } ?: 0f
        }.getOrDefault(0f)
        if (rotation == 0f) return bitmap
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(rotation) }, true)
        if (rotated != bitmap) bitmap.recycle()
        return rotated
    }

    private fun openRenderer(file: File): PdfRenderer =
        PdfRenderer(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY))
}

private inline fun <T> PdfRenderer.use(block: (PdfRenderer) -> T): T = try { block(this) } finally { close() }
private inline fun <T> PdfRenderer.Page.use(block: (PdfRenderer.Page) -> T): T = try { block(this) } finally { close() }
