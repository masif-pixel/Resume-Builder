package com.resume.builder.data.pdf

import android.content.Context
import android.net.Uri
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

class PdfTextExtractor(private val context: Context) {

    @Volatile private var initialized = false

    suspend fun extractText(uri: Uri): String = withContext(Dispatchers.IO) {
        if (!initialized) {
            PDFBoxResourceLoader.init(context.applicationContext)
            initialized = true
        }
        context.contentResolver.openInputStream(uri)?.use { input ->
            PDDocument.load(input).use { document ->
                val collector = GlyphCollector().apply { getText(document) }
                val text = LayoutReader.read(collector.pages).ifBlank { PDFTextStripper().getText(document) }
                // Unfold ligatures such as "ﬁ"/"ﬃ" so words like "certiﬁcation" match and edit normally.
                java.text.Normalizer.normalize(text, java.text.Normalizer.Form.NFKC)
            }
        }.orEmpty()
    }
}

/** Glyphs of one page plus its width, in PDFBox's top-left "direction adjusted" coordinates. */
internal class PageGlyphs(val width: Float, val glyphs: MutableList<TextPosition> = mutableListOf())

/** Runs PDFBox's text pass (which also drops duplicate overprinted glyphs) only to capture positioned glyphs. */
private class GlyphCollector : PDFTextStripper() {
    val pages = mutableListOf<PageGlyphs>()

    init {
        sortByPosition = true
    }

    override fun startPage(page: PDPage) {
        pages += PageGlyphs(page.cropBox?.width ?: page.mediaBox.width)
    }

    override fun writeString(text: String, textPositions: List<TextPosition>) {
        pages.lastOrNull()?.glyphs?.addAll(textPositions)
    }
}

/**
 * Rebuilds reading order from glyph positions. Resume PDFs often store text in drawing order, and sidebar layouts put
 * two unrelated columns side by side, so a plain text dump interleaves sections. Here each page is split into rows and
 * horizontal chunks; when a page has a clear vertical gutter, the left column (sidebar) and right column are read
 * separately and continued across pages, after the full-width header at the top.
 */
internal object LayoutReader {

    private class Chunk(val text: String, val x0: Float, val x1: Float)

    private class Row(val y: Float, val chunks: List<Chunk>) {
        val text: String get() = chunks.joinToString(" | ") { it.text }
        fun crosses(x: Float) = chunks.any { it.x0 < x - 1f && it.x1 > x + 1f }
        // Split by chunk centre so a chunk ending within the crossing slack of the gutter is never dropped.
        fun leftText(x: Float) = chunks.filter { (it.x0 + it.x1) / 2 < x }.joinToString(" | ") { it.text }
        fun rightText(x: Float) = chunks.filter { (it.x0 + it.x1) / 2 >= x }.joinToString(" | ") { it.text }
    }

    fun read(pages: List<PageGlyphs>): String {
        val header = mutableListOf<String>()
        val left = mutableListOf<String>()
        val right = mutableListOf<String>()
        var columnsStarted = false

        pages.forEach { page ->
            val rows = mergeOrphans(rows(page.glyphs))
            val gutter = gutter(rows, page.width)
            // The column block starts where several consecutive rows stay clear of the gutter; a short line such as a
            // job title between full-width header lines does not count.
            val blockStart = if (gutter == null) rows.size else rows.indices.firstOrNull { i ->
                (i until minOf(i + 4, rows.size)).none { rows[it].crosses(gutter) }
            } ?: rows.size
            rows.forEachIndexed { index, row ->
                when {
                    gutter == null || index < blockStart && !columnsStarted || row.crosses(gutter) ->
                        // Full-width rows above the first column block form the header; later ones read with the main column.
                        (if (columnsStarted) right else header) += row.text
                    else -> {
                        columnsStarted = true
                        row.leftText(gutter).takeIf { it.isNotBlank() }?.let { left += it }
                        row.rightText(gutter).takeIf { it.isNotBlank() }?.let { right += it }
                    }
                }
            }
        }
        return (header + left + right).joinToString("\n")
    }

    private fun rows(glyphs: List<TextPosition>): List<Row> {
        val lines = mutableListOf<MutableList<TextPosition>>()
        glyphs.filter { it.unicode.isNotEmpty() }.sortedBy { it.yDirAdj }.forEach { glyph ->
            val line = lines.lastOrNull()
            val tolerance = maxOf(glyph.heightDir, 2f) * 0.5f
            if (line != null && abs(line.first().yDirAdj - glyph.yDirAdj) <= tolerance) line += glyph else lines += mutableListOf(glyph)
        }
        return lines.filter { line -> line.any { it.unicode.isNotBlank() } }
            .map { line -> Row(line.first().yDirAdj, chunks(line.sortedBy { it.xDirAdj })) }
    }

    /** Splits a line at wide gaps (column gutters, icon gaps) and inserts spaces at word gaps. */
    private fun chunks(line: List<TextPosition>): List<Chunk> {
        val result = mutableListOf<Chunk>()
        val text = StringBuilder()
        var start = line.first().xDirAdj
        var end = start
        var pendingSpace = false
        line.forEach { glyph ->
            // Real space glyphs only mark a word break; gaps decide between a space and a new chunk.
            if (glyph.unicode.isBlank()) {
                pendingSpace = true
                return@forEach
            }
            // Font size in points is unreliable (often 1 scaled by the text matrix), so measure with rendered sizes.
            val height = glyph.heightDir.takeIf { it > 0f } ?: 6f
            val space = glyph.widthOfSpace.takeIf { it > 0f && it < height * 2 } ?: (height * 0.3f)
            val gap = glyph.xDirAdj - end
            if (text.isNotEmpty() && gap > maxOf(height * 1.5f, space * 3f)) {
                result += Chunk(text.toString().trim(), start, end)
                text.clear()
                start = glyph.xDirAdj
            } else if (text.isNotEmpty() && (pendingSpace || gap > space * 0.5f) && !text.endsWith(" ")) {
                text.append(' ')
            }
            pendingSpace = false
            text.append(glyph.unicode)
            end = maxOf(end, glyph.xDirAdj + glyph.widthDirAdj)
        }
        if (text.isNotBlank()) result += Chunk(text.toString().trim(), start, end)
        return result
    }

    /** Text wrapped onto its own short row (e.g. ".com" of a long email) is joined back onto the row above. */
    private fun mergeOrphans(rows: List<Row>): List<Row> {
        val out = mutableListOf<Row>()
        rows.forEach { row ->
            val previous = out.lastOrNull()
            val orphan = row.chunks.singleOrNull()?.text?.takeIf { it.matches(Regex("\\.[A-Za-z]{2,6}")) }
            val target = previous?.chunks?.lastOrNull { it.text.contains('@') && !it.text.substringAfter('@').contains('.') }
            if (orphan != null && previous != null && target != null) {
                out[out.lastIndex] = Row(previous.y, previous.chunks.map { if (it === target) Chunk(it.text + orphan, it.x0, it.x1) else it })
            } else {
                out += row
            }
        }
        return out
    }

    /**
     * Finds an x position that splits the page into two real columns: few rows run across it, and many rows have text
     * on both sides of it. Returns null for single-column pages (a right-aligned date alone does not make a column).
     */
    private fun gutter(rows: List<Row>, pageWidth: Float): Float? {
        if (rows.size < 8 || pageWidth <= 0f) return null
        var best: Float? = null
        var bestScore = Int.MIN_VALUE
        var x = pageWidth * 0.15f
        while (x <= pageWidth * 0.7f) {
            val crossing = rows.count { it.crosses(x) }
            val open = rows.filterNot { it.crosses(x) }
            val leftRows = open.count { r -> r.chunks.any { it.x1 <= x } }
            val rightRows = open.count { r -> r.chunks.any { it.x0 >= x } }
            val bothRows = open.count { r -> r.chunks.any { it.x1 <= x } && r.chunks.any { it.x0 >= x } }
            val isColumnSplit = crossing <= rows.size / 4 && leftRows >= 5 && rightRows >= 5 && bothRows >= 3
            val score = bothRows * 2 + minOf(leftRows, rightRows) - crossing * 3
            if (isColumnSplit && score > bestScore) {
                bestScore = score
                best = x
            }
            x += 2f
        }
        return best
    }
}
