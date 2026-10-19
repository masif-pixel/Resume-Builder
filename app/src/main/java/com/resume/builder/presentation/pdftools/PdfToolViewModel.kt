package com.resume.builder.presentation.pdftools

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Compress
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.Draw
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.MergeType
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.resume.builder.R
import com.resume.builder.presentation.components.UiText
import com.resume.builder.data.pdf.CompressionLevel
import com.resume.builder.data.pdf.ImageFormat
import com.resume.builder.data.pdf.ImageQuality
import com.resume.builder.data.pdf.LocalPdf
import com.resume.builder.data.pdf.PdfToolsEngine
import com.resume.builder.data.pdf.PickedFile
import com.resume.builder.data.pdf.ToolOrientation
import com.resume.builder.data.pdf.ToolPageSize
import com.resume.builder.domain.model.FileCategory
import com.resume.builder.domain.model.SavedFile
import com.resume.builder.domain.repository.SavedFileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class PdfTool(@StringRes val title: Int, @StringRes val subtitle: Int, val icon: ImageVector, val color: Color) {
    MERGE(R.string.tool_merge, R.string.tool_merge_desc, Icons.Rounded.MergeType, Color(0xFF4F46E5)),
    COMPRESS(R.string.tool_compress, R.string.tool_compress_desc, Icons.Rounded.Compress, Color(0xFF0D9488)),
    SPLIT(R.string.tool_split, R.string.tool_split_desc, Icons.Rounded.ContentCut, Color(0xFFEA580C)),
    SIGN(R.string.tool_sign, R.string.tool_sign_desc, Icons.Rounded.Draw, Color(0xFF7C3AED)),
    TO_IMAGES(R.string.tool_to_images, R.string.tool_to_images_desc, Icons.Rounded.Image, Color(0xFFDB2777)),
}

data class ToolResult(val files: List<SavedFile>, val headline: UiText, val detail: UiText? = null)

data class PdfToolState(
    val mergeItems: List<PickedFile> = emptyList(),
    val pageSize: ToolPageSize = ToolPageSize.A4,
    val orientation: ToolOrientation = ToolOrientation.AUTO,
    val marginMm: Float = 6f,
    val pdf: LocalPdf? = null,
    val thumbnails: Map<Int, Bitmap> = emptyMap(),
    val selectedPages: Set<Int> = emptySet(),
    val compression: CompressionLevel = CompressionLevel.MEDIUM,
    val imageFormat: ImageFormat = ImageFormat.JPG,
    val imageQuality: ImageQuality = ImageQuality.MEDIUM,
    val signPage: Int = 0,
    val signature: Bitmap? = null,
    val signX: Float = 0.7f,
    val signY: Float = 0.85f,
    val signWidth: Float = 0.3f,
    val working: Boolean = false,
    val result: ToolResult? = null,
)

class PdfToolViewModel(
    val tool: PdfTool,
    private val engine: PdfToolsEngine,
    private val savedFiles: SavedFileRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(PdfToolState())
    val state: StateFlow<PdfToolState> = _state.asStateFlow()

    private val _messages = Channel<UiText>(Channel.BUFFERED)
    val messages = _messages.receiveAsFlow()

    private var thumbnailJob: Job? = null

    private fun stamp(): String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

    private inline fun work(crossinline block: suspend () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(working = true) }
            runCatching { block() }.onFailure { _messages.send(UiText.Res(R.string.error_generic)) }
            _state.update { it.copy(working = false) }
        }
    }

    // region Merge
    fun addMergeFiles(uris: List<Uri>) = viewModelScope.launch {
        val picked = uris.mapNotNull { runCatching { engine.describe(it) }.getOrNull() }
        _state.update { it.copy(mergeItems = it.mergeItems + picked) }
    }

    fun removeMergeItem(index: Int) = _state.update { s -> s.copy(mergeItems = s.mergeItems.filterIndexed { i, _ -> i != index }) }

    fun moveMergeItem(index: Int, delta: Int) = _state.update { s ->
        val target = index + delta
        if (target !in s.mergeItems.indices) s else s.copy(mergeItems = s.mergeItems.toMutableList().apply { add(target, removeAt(index)) })
    }

    fun setPageSize(size: ToolPageSize) = _state.update { it.copy(pageSize = size) }
    fun setOrientation(orientation: ToolOrientation) = _state.update { it.copy(orientation = orientation) }
    fun setMargin(mm: Float) = _state.update { it.copy(marginMm = mm) }

    fun merge() = work {
        val s = _state.value
        val output = engine.newOutputFile()
        val pages = engine.merge(s.mergeItems, s.pageSize, s.orientation, s.marginMm, output)
        val saved = savedFiles.register(output, "merged_${stamp()}.pdf", PDF, FileCategory.PDF_TOOLS)
        _state.update { it.copy(result = ToolResult(listOf(saved), UiText.Res(R.string.result_merged), UiText.Res(R.string.result_merged_detail, s.mergeItems.size, pages))) }
    }
    // endregion

    // region Single PDF tools
    fun loadPdf(uri: Uri) = work {
        val pdf = engine.importPdf(uri)
        _state.update { it.copy(pdf = pdf, thumbnails = emptyMap(), selectedPages = emptySet(), signPage = 0) }
        thumbnailJob?.cancel()
        thumbnailJob = viewModelScope.launch {
            for (index in 0 until minOf(pdf.pageCount, MAX_THUMBNAILS)) {
                val bitmap = runCatching { engine.renderPage(pdf.file, index, 360) }.getOrNull() ?: continue
                _state.update { it.copy(thumbnails = it.thumbnails + (index to bitmap)) }
            }
        }
    }

    fun clearPdf() {
        thumbnailJob?.cancel()
        _state.value.pdf?.file?.delete()
        _state.update { it.copy(pdf = null, thumbnails = emptyMap(), selectedPages = emptySet(), signature = null) }
    }

    fun togglePage(index: Int) = _state.update { s ->
        s.copy(selectedPages = if (index in s.selectedPages) s.selectedPages - index else s.selectedPages + index)
    }

    fun selectAll(select: Boolean) = _state.update { s ->
        s.copy(selectedPages = if (select) (0 until (s.pdf?.pageCount ?: 0)).toSet() else emptySet())
    }

    fun setCompression(level: CompressionLevel) = _state.update { it.copy(compression = level) }

    fun compress() = work {
        val s = _state.value
        val pdf = s.pdf ?: return@work
        val output = engine.newOutputFile()
        engine.compress(pdf.file, s.compression, pdf.pageCount, output)
        val before = pdf.sizeBytes
        val after = output.length()
        val base = pdf.name.substringBeforeLast('.')
        val saved = savedFiles.register(output, "${base}_compressed.pdf", PDF, FileCategory.PDF_TOOLS)
        val change = if (before > 0) ((before - after) * 100 / before) else 0
        _state.update {
            it.copy(
                result = ToolResult(
                    listOf(saved),
                    if (change > 0) UiText.Res(R.string.result_reduced, change.toInt()) else UiText.Res(R.string.result_compressed),
                    UiText.Res(if (change <= 0) R.string.result_sizes_optimised else R.string.result_sizes, formatSize(before), formatSize(after)),
                ),
            )
        }
    }

    fun extract() = work {
        val s = _state.value
        val pdf = s.pdf ?: return@work
        val output = engine.newOutputFile()
        engine.extractPages(pdf.file, s.selectedPages.toList(), output)
        val base = pdf.name.substringBeforeLast('.')
        val saved = savedFiles.register(output, "${base}_pages.pdf", PDF, FileCategory.PDF_TOOLS)
        _state.update { it.copy(result = ToolResult(listOf(saved), UiText.Res(R.string.result_extracted), UiText.Res(R.string.result_extracted_detail, s.selectedPages.size, pdf.pageCount))) }
    }

    fun setImageFormat(format: ImageFormat) = _state.update { it.copy(imageFormat = format) }
    fun setImageQuality(quality: ImageQuality) = _state.update { it.copy(imageQuality = quality) }

    fun exportImages() = work {
        val s = _state.value
        val pdf = s.pdf ?: return@work
        val files = engine.exportImages(pdf.file, s.selectedPages.toList(), s.imageFormat, s.imageQuality)
        val base = pdf.name.substringBeforeLast('.')
        val sortedPages = s.selectedPages.sorted()
        val saved = files.mapIndexed { i, file ->
            savedFiles.register(file, "${base}_page${sortedPages[i] + 1}.${s.imageFormat.extension}", s.imageFormat.mime, FileCategory.IMAGES)
        }
        _state.update { it.copy(result = ToolResult(saved, UiText.Plural(R.plurals.result_images_saved, saved.size, saved.size), UiText.Res(R.string.result_images_detail))) }
    }

    fun setSignPage(index: Int) = _state.update { it.copy(signPage = index) }
    fun setSignature(bitmap: Bitmap?) = _state.update { it.copy(signature = bitmap) }
    fun moveSignature(x: Float, y: Float) = _state.update { it.copy(signX = x.coerceIn(0f, 1f), signY = y.coerceIn(0f, 1f)) }
    fun setSignatureWidth(width: Float) = _state.update { it.copy(signWidth = width) }

    fun sign() = work {
        val s = _state.value
        val pdf = s.pdf ?: return@work
        val signature = s.signature ?: return@work
        val output = engine.newOutputFile()
        engine.sign(pdf.file, pdf.pageCount, s.signPage, signature, s.signX, s.signY, s.signWidth, output)
        val base = pdf.name.substringBeforeLast('.')
        val saved = savedFiles.register(output, "${base}_signed.pdf", PDF, FileCategory.PDF_TOOLS)
        _state.update { it.copy(result = ToolResult(listOf(saved), UiText.Res(R.string.result_signed), UiText.Res(R.string.result_signed_detail, s.signPage + 1))) }
    }
    // endregion

    suspend fun renderLargePage(index: Int): Bitmap? = _state.value.pdf?.let { runCatching { engine.renderPage(it.file, index, 1080) }.getOrNull() }

    fun startOver() {
        clearPdf()
        _state.value = PdfToolState()
    }

    override fun onCleared() {
        _state.value.pdf?.file?.delete()
    }

    private fun formatSize(bytes: Long) = when {
        bytes >= 1_048_576 -> "%.2f MB".format(bytes / 1_048_576.0)
        else -> "%.1f KB".format(bytes / 1024.0)
    }

    private companion object {
        const val PDF = "application/pdf"
        const val MAX_THUMBNAILS = 80
    }
}
