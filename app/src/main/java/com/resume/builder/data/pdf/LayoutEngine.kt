package com.resume.builder.data.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import androidx.core.graphics.ColorUtils
import com.resume.builder.domain.model.ContactField
import com.resume.builder.domain.model.CoverLetter
import com.resume.builder.domain.model.CustomLayout
import com.resume.builder.domain.model.HeaderStyle
import com.resume.builder.domain.model.HeadingStyle
import com.resume.builder.domain.model.ItemFields
import com.resume.builder.domain.model.LayoutKind
import com.resume.builder.domain.model.Resume
import com.resume.builder.domain.model.ResumeSection
import com.resume.builder.domain.model.SectionItem
import com.resume.builder.domain.model.SectionType
import com.resume.builder.domain.model.TemplateCatalog
import java.text.DateFormat
import java.util.Date
import kotlin.math.max
import kotlin.math.min

/** A vertically stacked, measurable piece of content. [draw] receives the column x, top y and width. */
private class Block(
    val height: Float,
    val keepWithNext: Boolean = false,
    val spacer: Boolean = false,
    val draw: (Canvas, Float, Float, Float) -> Unit = { _, _, _, _ -> },
)

private class Column(val x: Float, val width: Float, val firstTop: Float, val top: Float, val bottom: Float)

internal class LayoutEngine(private val resume: Resume, private val assets: RenderAssets) {

    private val spec = TemplateCatalog.byId(resume.templateId)
    private val style = resume.style
    private val content = resume.content
    private val lang = style.language
    private val rtl = lang.rtl

    val pageW: Int = style.pageSize.widthPt
    val pageH: Int = style.pageSize.heightPt

    // region Resolved theme
    private val white = Color.WHITE
    private val accent = style.accentColor?.toInt() ?: spec.accent.toInt()
    private val sidebarBg = style.sidebarColor?.toInt() ?: if (spec.sidebar == spec.accent) accent else spec.sidebar.toInt()
    private val sidebarDark = ColorUtils.calculateLuminance(sidebarBg) < 0.5
    private val sidebarText = if (sidebarDark) white else 0xFF2B2F3A.toInt()
    private val sidebarMuted = ColorUtils.setAlphaComponent(sidebarText, 205)
    private val bg = style.backgroundColor?.toInt() ?: spec.background.toInt()
    private val bgDark = ColorUtils.calculateLuminance(bg) < 0.5
    private val text = style.textColor?.toInt() ?: if (bgDark) 0xFFF1F1F4.toInt() else spec.text.toInt()
    private val strong = if (bgDark) white else ColorUtils.blendARGB(text, Color.BLACK, 0.35f)
    private val muted = ColorUtils.blendARGB(text, bg, 0.36f)
    private val nameColor = style.nameColor?.toInt() ?: when {
        bgDark -> white
        style.accentColor != null && spec.nameColor == spec.accent -> accent
        else -> spec.nameColor.toInt()
    }
    private val jobTitleColor = style.jobTitleColor?.toInt() ?: if (bgDark) muted else spec.jobTitle.toInt()
    private val headingColor = style.headingColor?.toInt() ?: when {
        style.accentColor != null -> accent
        bgDark -> white
        else -> spec.headingColor.toInt()
    }
    private val subtitleColor = ColorUtils.blendARGB(accent, text, 0.45f)
    private val photoBorder = style.photoBorderColor?.toInt() ?: when {
        spec.skillBars -> accent
        spec.layout == LayoutKind.SIDEBAR && sidebarDark -> ColorUtils.setAlphaComponent(white, 150)
        spec.header == HeaderStyle.SOLID -> ColorUtils.setAlphaComponent(white, 170)
        else -> ColorUtils.setAlphaComponent(accent, 200)
    }
    // endregion

    // region Typography & spacing
    private val family = (style.font ?: spec.font).family
    private val regular: Typeface = Typeface.create(family, Typeface.NORMAL)
    private val bold: Typeface = Typeface.create(family, Typeface.BOLD)
    private val italic: Typeface = Typeface.create(family, Typeface.ITALIC)

    private val dense = spec.id == "compact"
    private val nameSize = ((if (spec.layout == LayoutKind.SIDEBAR) 25f else 24f) + style.nameSize).coerceIn(12f, 48f)
    private val titleSize = (12f + style.jobTitleSize).coerceIn(7f, 24f)
    private val headingSize = ((if (dense) 10.5f else 11.5f) + style.headingSize).coerceIn(7f, 22f)
    private val bodySize = ((if (dense) 8.5f else 9f) + style.bodySize).coerceIn(6f, 16f)
    private val smallSize = bodySize - 0.8f

    private val mh = (34f + style.marginHorizontal).coerceIn(10f, 90f)
    private val mv = ((if (dense) 26f else 32f) + style.marginVertical).coerceIn(10f, 90f)
    private val sectionGap = ((if (dense) 9f else 13f) + style.sectionSpacing).coerceIn(2f, 48f)
    private val headingGap = (6f + style.headingSpacing).coerceIn(0f, 24f)
    private val itemGap = if (dense) 5f else 8f
    private val lineMult = (1.12f + style.lineSpacing).coerceIn(0.9f, 2.2f)
    private val letterSpacing = style.letterSpacing.coerceIn(-0.08f, 0.3f)

    private val photoSize = (92f + style.photoSize).coerceIn(36f, 160f)
    private val photoRadius = ((style.photoCornerRadius ?: if (spec.photoRound) 50f else 10f) / 100f).coerceIn(0f, 0.5f)
    private val photoBorderWidth = (style.photoBorderWidth ?: if (spec.skillBars) 4f else 2f).coerceIn(0f, 12f)
    private val photo: Bitmap? = assets.photo.takeIf { style.showPhoto }
    // endregion

    private val pageOps = mutableListOf<MutableList<DrawOp>>()
    private var background: (Int) -> List<DrawOp> = { listOf { c -> c.drawColor(bg) } }

    private fun ops(page: Int): MutableList<DrawOp> {
        while (pageOps.size <= page) pageOps.add(mutableListOf())
        return pageOps[page]
    }

    fun resumePages(): List<List<DrawOp>> {
        pageOps.clear()
        when (spec.layout) {
            LayoutKind.SIDEBAR -> layoutSidebar()
            LayoutKind.SINGLE_COLUMN -> layoutSingleColumn()
            LayoutKind.SPLIT_BELOW_HEADER -> layoutSplit()
        }
        ops(0)
        return pageOps.mapIndexed { index, list -> background(index) + list }
    }

    // region Layouts
    private fun layoutSidebar() {
        val ratio = (spec.sidebarRatio + style.columnWidth / 100f).coerceIn(0.22f, 0.46f)
        val sw = pageW * ratio
        val onRight = spec.sidebarOnRight xor style.swapColumns xor rtl
        val sx = if (onRight) pageW - sw else 0f
        val pad = (mh * 0.62f).coerceAtLeast(12f)
        val sideTop = if (spec.angledHeader && !onRight) mv + 6f else mv
        val side = Column(sx + pad, sw - pad * 2, sideTop, mv, pageH - mv)
        val gutter = mh * 0.72f
        val mainLeft = if (onRight) 0f else sw
        val main = Column(mainLeft + gutter, pageW - sw - gutter * 2, mv, mv, pageH - mv)

        val sideBlocks = mutableListOf<Block>()
        if (photo != null) {
            sideBlocks += photoBlock(side.width, min(photoSize, side.width), centered = true)
            sideBlocks += spacer(sectionGap + 2f)
        }
        contactSidebarBlocks(side.width).takeIf { it.isNotEmpty() }?.let { lines ->
            sideBlocks += heading(lang.headingFor(content.section(SectionType.CONTACT) ?: ResumeSection(type = SectionType.CONTACT)), side.width, onSidebar = true)
            sideBlocks += lines
            sideBlocks += spacer(sectionGap)
        }
        val mainBlocks = mutableListOf<Block>()
        nameBlock(main.width, Layout.Alignment.ALIGN_NORMAL)?.let {
            mainBlocks += it
            mainBlocks += spacer(sectionGap)
        }
        visibleSections().forEach { section ->
            if (section.type.isCompact) {
                sideBlocks += sectionBlocks(section, side.width, onSidebar = true, compactAsList = true)
            } else {
                mainBlocks += sectionBlocks(section, main.width, onSidebar = false, compactAsList = false)
            }
        }
        flow(sideBlocks, side)
        flow(mainBlocks, main)

        background = { page ->
            listOf { c ->
                c.drawColor(bg)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = sidebarBg }
                c.drawRect(sx, 0f, sx + sw, pageH.toFloat(), paint)
                if (spec.angledHeader && page == 0 && !onRight) {
                    paint.color = accent
                    val path = Path().apply {
                        moveTo(0f, 0f); lineTo(sw * 1.25f, 0f); lineTo(0f, 150f); close()
                    }
                    c.drawPath(path, paint)
                }
                drawDecorations(c)
            }
        }
    }

    private fun layoutSingleColumn() {
        val col = Column(mh, pageW - mh * 2, mv, mv, pageH - mv)
        val blocks = mutableListOf<Block>()
        blocks += headerBlocks(col.width)
        visibleSections().forEach { section ->
            blocks += sectionBlocks(section, col.width, onSidebar = false, compactAsList = false)
        }
        flow(blocks, col)
        background = {
            listOf { c ->
                c.drawColor(bg)
                drawDecorations(c)
            }
        }
    }

    private fun layoutSplit() {
        val header = stack(headerBlocks(pageW - mh * 2))
        val headerTop = mv
        ops(0).add { c -> header.draw(c, mh, headerTop, pageW - mh * 2) }
        val gutter = 22f
        val ratio = (spec.sidebarRatio + style.columnWidth / 100f).coerceIn(0.24f, 0.48f)
        val usable = pageW - mh * 2 - gutter
        val narrowW = usable * ratio
        val wideW = usable - narrowW
        val narrowOnRight = style.swapColumns xor rtl
        val narrowX = if (narrowOnRight) mh + wideW + gutter else mh
        val wideX = if (narrowOnRight) mh else mh + narrowW + gutter
        val firstTop = headerTop + header.height + 6f
        val narrow = Column(narrowX, narrowW, firstTop, mv, pageH - mv)
        val wide = Column(wideX, wideW, firstTop, mv, pageH - mv)

        val narrowTypes = setOf(SectionType.SKILLS, SectionType.LANGUAGES, SectionType.INTERESTS, SectionType.EDUCATION, SectionType.CERTIFICATIONS)
        val narrowBlocks = mutableListOf<Block>()
        val wideBlocks = mutableListOf<Block>()
        visibleSections().forEach { section ->
            if (section.type in narrowTypes) {
                narrowBlocks += sectionBlocks(section, narrow.width, onSidebar = false, compactAsList = true)
            } else {
                wideBlocks += sectionBlocks(section, wide.width, onSidebar = false, compactAsList = false)
            }
        }
        flow(narrowBlocks, narrow)
        flow(wideBlocks, wide)
        val dividerX = if (narrowOnRight) narrowX - gutter / 2 else narrowX + narrowW + gutter / 2
        background = { page ->
            listOf { c ->
                c.drawColor(bg)
                val paint = Paint().apply { color = ColorUtils.blendARGB(bg, text, 0.15f); strokeWidth = 0.7f }
                c.drawLine(dividerX, if (page == 0) firstTop else mv, dividerX, pageH - mv, paint)
                drawDecorations(c)
            }
        }
    }

    /** Page-level ornaments that sit under the content on every page. */
    private fun drawDecorations(c: Canvas) {
        if (spec.topStripe) {
            c.drawRect(0f, 0f, pageW.toFloat(), 6f, Paint().apply { color = accent })
        }
        if (spec.pageFrame) {
            val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = ColorUtils.blendARGB(bg, accent, 0.55f)
                style = Paint.Style.STROKE
                strokeWidth = 0.9f
            }
            c.drawRect(RectF(14f, 14f, pageW - 14f, pageH - 14f), frame)
        }
    }

    fun coverLetterPages(letter: CoverLetter): List<List<DrawOp>> {
        pageOps.clear()
        val col = Column(mh + 8f, pageW - (mh + 8f) * 2, mv + 10f, mv + 10f, pageH - mv)
        val blocks = mutableListOf<Block>()
        val contact = content.contact
        if (contact.fullName.isNotBlank()) {
            blocks += textBlock(contact.fullName, textPaint(nameSize - 3f, accent, bold), col.width)
        }
        if (contact.jobTitle.isNotBlank()) {
            blocks += textBlock(contact.jobTitle, textPaint(titleSize - 1f, jobTitleColor, regular, letterSpacing + 0.03f), col.width, after = 4f)
        }
        inlineContact(col.width, centered = false, color = muted)?.let { blocks += it }
        blocks += Block(20f) { c, x, y, w ->
            c.drawRect(x, y + 8f, x + w, y + 9.4f, Paint().apply { color = accent })
        }
        blocks += textBlock(DateFormat.getDateInstance(DateFormat.LONG).format(Date(letter.updatedAt)), textPaint(smallSize, muted), col.width, after = 12f)
        if (letter.companyName.isNotBlank()) {
            blocks += textBlock(letter.companyName, textPaint(bodySize + 0.5f, strong, bold), col.width, after = 10f)
        }
        letter.body.trim().split(Regex("\\n\\s*\\n")).forEach { paragraph ->
            blocks += textBlock(paragraph.trim(), textPaint(bodySize + 1.2f, text), col.width, after = 9f)
        }
        flow(blocks, col)
        background = {
            listOf { c ->
                c.drawColor(bg)
                c.drawRect(0f, 0f, pageW.toFloat(), 7f, Paint().apply { color = accent })
            }
        }
        ops(0)
        return pageOps.mapIndexed { index, list -> background(index) + list }
    }
    // endregion

    // region Flow
    private fun flow(blocks: List<Block>, column: Column) {
        var page = 0
        var y = column.firstTop
        var i = 0
        while (i < blocks.size) {
            val block = blocks[i]
            val top = if (page == 0) column.firstTop else column.top
            if (block.spacer && page > 0 && y <= top + 0.5f) {
                i++
                continue
            }
            var needed = block.height
            var j = i
            while (blocks[j].keepWithNext && j + 1 < blocks.size) {
                j++
                needed += blocks[j].height
            }
            if (y + needed > column.bottom && y > top + 0.5f && page < ResumeRenderer.MAX_PAGES - 1) {
                page++
                y = column.top
                continue
            }
            if (!block.spacer) {
                val drawY = y
                ops(page).add { c -> block.draw(c, column.x, drawY, column.width) }
            }
            y += block.height
            i++
        }
    }

    private fun spacer(height: Float) = Block(height, spacer = true)

    private fun stack(blocks: List<Block>): Block = Block(blocks.sumOf { it.height.toDouble() }.toFloat()) { c, x, y, w ->
        var cursor = y
        blocks.forEach { block ->
            block.draw(c, x, cursor, w)
            cursor += block.height
        }
    }
    // endregion

    // region Text primitives
    private fun textPaint(size: Float, color: Int, face: Typeface = regular, spacing: Float = letterSpacing) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = size
            this.color = color
            typeface = face
            letterSpacing = spacing
        }

    private fun staticLayout(
        value: CharSequence,
        paint: TextPaint,
        width: Float,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    ): StaticLayout = StaticLayout.Builder.obtain(value, 0, value.length, paint, max(1, width.toInt()))
        .setAlignment(align)
        .setIncludePad(false)
        .setLineSpacing(0f, lineMult)
        .setTextDirection(if (rtl) TextDirectionHeuristics.RTL else TextDirectionHeuristics.FIRSTSTRONG_LTR)
        .build()

    private fun Canvas.drawLayout(layout: StaticLayout, x: Float, y: Float) {
        save()
        translate(x, y)
        layout.draw(this)
        restore()
    }

    private fun textBlock(
        value: String,
        paint: TextPaint,
        width: Float,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
        after: Float = 0f,
        keep: Boolean = false,
    ): Block {
        val layout = staticLayout(value, paint, width, align)
        return Block(layout.height + after, keep) { c, x, y, _ -> c.drawLayout(layout, x, y) }
    }

    private fun richText(value: String, width: Float, color: Int): List<Block> {
        val paint = textPaint(bodySize, color)
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
        return value.lines().map { it.trim() }.filter { it.isNotEmpty() }.map { line ->
            val isBullet = line.first() in BULLET_CHARS
            if (isBullet) {
                val indent = 10f
                val layout = staticLayout(line.drop(1).trim(), paint, width - indent)
                Block(layout.height + 1.8f) { c, x, y, w ->
                    val dotX = if (rtl) x + w - 3.5f else x + 3.5f
                    c.drawCircle(dotX, y + bodySize * 0.58f, 1.15f, dotPaint)
                    c.drawLayout(layout, if (rtl) x else x + indent, y)
                }
            } else {
                textBlock(line, paint, width, after = 2f)
            }
        }
    }

    private fun offset(block: Block, dx: Float, shrink: Float): Block =
        Block(block.height, block.keepWithNext, block.spacer) { c, x, y, w -> block.draw(c, x + dx, y, w - shrink) }
    // endregion

    // region Header
    private fun nameBlock(
        width: Float,
        align: Layout.Alignment,
        nameTint: Int = nameColor,
        titleTint: Int = jobTitleColor,
    ): Block? {
        val contact = content.contact
        if (contact.fullName.isBlank() && contact.jobTitle.isBlank()) return null
        val parts = mutableListOf<Block>()
        if (contact.fullName.isNotBlank()) {
            val name = if (spec.uppercaseName) contact.fullName.uppercase() else contact.fullName
            parts += textBlock(name, textPaint(nameSize, nameTint, bold, letterSpacing - 0.01f), width, align, after = 2f)
        }
        if (contact.jobTitle.isNotBlank()) {
            val spacing = if (spec.header == HeaderStyle.CENTERED || spec.layout == LayoutKind.SIDEBAR) 0.06f else 0.02f
            parts += textBlock(contact.jobTitle, textPaint(titleSize, titleTint, regular, letterSpacing + spacing), width, align)
        }
        return stack(parts)
    }

    private fun headerBlocks(width: Float): List<Block> {
        val blocks = mutableListOf<Block>()
        when (spec.header) {
            HeaderStyle.CENTERED -> {
                if (photo != null) {
                    blocks += photoBlock(width, photoSize * 0.82f, centered = true)
                    blocks += spacer(8f)
                }
                nameBlock(width, Layout.Alignment.ALIGN_CENTER)?.let { blocks += it }
                inlineContact(width, centered = true, color = muted)?.let {
                    blocks += spacer(6f)
                    blocks += it
                }
                blocks += Block(sectionGap + 4f) { c, x, y, w ->
                    c.drawLine(x, y + 7f, x + w, y + 7f, Paint().apply { color = ColorUtils.blendARGB(bg, text, 0.25f); strokeWidth = 0.8f })
                }
            }
            HeaderStyle.BAND -> {
                val inner = headerRow(width - 24f, photoSize * 0.72f)
                val pad = 16f
                blocks += Block(inner.height + pad * 2 - mv / 2 + sectionGap) { c, x, y, w ->
                    val paint = Paint().apply { color = ColorUtils.blendARGB(bg, accent, 0.11f) }
                    c.drawRect(0f, 0f, pageW.toFloat(), y + inner.height + pad * 2 - mv / 2, paint)
                    paint.color = accent
                    c.drawRect(0f, y + inner.height + pad * 2 - mv / 2 - 2f, pageW.toFloat(), y + inner.height + pad * 2 - mv / 2, paint)
                    inner.draw(c, x + 12f, y + pad - mv / 2 + 4f, w - 24f)
                }
            }
            HeaderStyle.LEFT, HeaderStyle.SIDEBAR_PHOTO -> {
                blocks += headerRow(width, photoSize * 0.66f)
                blocks += accentRule()
            }
            HeaderStyle.SOLID -> {
                val onHeader = if (ColorUtils.calculateLuminance(accent) > 0.6) 0xFF1B1E27.toInt() else white
                val onHeaderMuted = ColorUtils.setAlphaComponent(onHeader, 215)
                val inner = headerRow(
                    width - 24f, photoSize * 0.72f,
                    nameTint = onHeader, titleTint = onHeaderMuted, contactTint = onHeaderMuted, iconTint = onHeader, iconHole = accent,
                )
                val pad = 18f
                val bandBottom = inner.height + pad * 2 - mv / 2
                blocks += Block(bandBottom + sectionGap) { c, x, y, w ->
                    c.drawRect(0f, 0f, pageW.toFloat(), y + bandBottom, Paint().apply { color = accent })
                    inner.draw(c, x + 12f, y + pad - mv / 2 + 4f, w - 24f)
                }
            }
            HeaderStyle.SPLIT -> {
                blocks += splitHeader(width)
                blocks += accentRule()
            }
        }
        return blocks
    }

    private fun accentRule(): Block = Block(sectionGap) { c, x, y, w ->
        c.drawRect(x, y + 5f, x + w, y + 6.2f, Paint().apply { color = accent })
    }

    /** Name (with optional photo) on one side, contact lines stacked on the other. */
    private fun splitHeader(width: Float): Block {
        val contactW = width * 0.38f
        val leftW = width - contactW - 16f
        val hasPhoto = photo != null
        val size = photoSize * 0.66f
        val nameW = if (hasPhoto) leftW - size - 12f else leftW
        val name = nameBlock(nameW, Layout.Alignment.ALIGN_NORMAL)
        val contact = stackedContact(contactW)
        val height = maxOf(name?.height ?: 0f, if (hasPhoto) size else 0f, contact?.height ?: 0f)
        return Block(height + 4f) { c, x, y, w ->
            if (hasPhoto) drawPhoto(c, photo!!, if (rtl) x + w - size else x, y, size)
            val nameX = when {
                rtl -> x + contactW + 16f
                hasPhoto -> x + size + 12f
                else -> x
            }
            name?.let { it.draw(c, nameX, y + (height - it.height) / 2, nameW) }
            contact?.let { it.draw(c, if (rtl) x else x + w - contactW, y + (height - it.height) / 2, contactW) }
        }
    }

    /** Photo on one side, name/title/contact stacked on the other. */
    private fun headerRow(
        width: Float,
        size: Float,
        nameTint: Int = nameColor,
        titleTint: Int = jobTitleColor,
        contactTint: Int = muted,
        iconTint: Int? = null,
        iconHole: Int = bg,
    ): Block {
        val hasPhoto = photo != null
        val textWidth = if (hasPhoto) width - size - 14f else width
        val parts = listOfNotNull(
            nameBlock(textWidth, Layout.Alignment.ALIGN_NORMAL, nameTint, titleTint),
            inlineContact(textWidth, centered = false, color = contactTint, iconTint = iconTint, iconHole = iconHole)
                ?.let { stack(listOf(spacer(5f), it)) },
        )
        val textStack = stack(parts)
        val height = max(textStack.height, if (hasPhoto) size else 0f) + 2f
        return Block(height) { c, x, y, w ->
            if (hasPhoto) {
                val photoX = if (rtl) x + w - size else x
                drawPhoto(c, photo!!, photoX, y, size)
                val textY = y + max(0f, (size - textStack.height) / 2)
                textStack.draw(c, if (rtl) x else x + size + 14f, textY, textWidth)
            } else {
                textStack.draw(c, x, y, w)
            }
        }
    }

    private fun photoBlock(width: Float, size: Float, centered: Boolean): Block = Block(size) { c, x, y, w ->
        val left = if (centered) x + (w - size) / 2 else x
        drawPhoto(c, photo ?: return@Block, left, y, size)
    }

    private fun drawPhoto(c: Canvas, bitmap: Bitmap, left: Float, top: Float, size: Float) {
        val side = min(bitmap.width, bitmap.height)
        val src = Rect((bitmap.width - side) / 2, (bitmap.height - side) / 2, (bitmap.width + side) / 2, (bitmap.height + side) / 2)
        val dst = RectF(left, top, left + size, top + size)
        val radius = size * photoRadius
        c.save()
        c.clipPath(Path().apply { addRoundRect(dst, radius, radius, Path.Direction.CW) })
        c.drawBitmap(bitmap, src, dst, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        c.restore()
        if (photoBorderWidth > 0f) {
            val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = photoBorderWidth
                color = photoBorder
            }
            val inset = RectF(dst).apply { inset(photoBorderWidth / 2, photoBorderWidth / 2) }
            val r = max(0f, radius - photoBorderWidth / 2)
            c.drawRoundRect(inset, r, r, stroke)
        }
    }
    // endregion

    // region Contact
    private fun contactEntries(): List<Pair<ContactField, String>> {
        val contactSection = content.section(SectionType.CONTACT)
        if (contactSection != null && !contactSection.visible) return emptyList()
        return style.contactOrder
            .filter { it !in style.hiddenContactFields }
            .map { it to content.contact.field(it).trim() }
            .filter { it.second.isNotEmpty() }
            .map { (field, value) -> field to value.removePrefix("https://").removePrefix("http://").removePrefix("www.") }
    }

    private fun contactSidebarBlocks(width: Float): List<Block> {
        val showIcons = style.showContactIcons
        val iconSize = smallSize * 1.05f
        val textWidth = if (showIcons) width - iconSize - 6f else width
        val paint = textPaint(smallSize, sidebarText)
        return contactEntries().map { (field, value) ->
            val layout = staticLayout(value, paint, textWidth)
            Block(max(layout.height.toFloat(), iconSize) + 4.5f) { c, x, y, w ->
                if (showIcons) {
                    val iconX = if (rtl) x + w - iconSize else x
                    drawIcon(c, field, iconX, y + 0.5f, iconSize, sidebarMuted, sidebarBg)
                    c.drawLayout(layout, if (rtl) x else x + iconSize + 6f, y)
                } else {
                    c.drawLayout(layout, x, y)
                }
            }
        }
    }

    /** One contact entry per line, aligned to the outer edge with the icon on that edge. */
    private fun stackedContact(width: Float): Block? {
        val entries = contactEntries()
        if (entries.isEmpty()) return null
        val showIcons = style.showContactIcons
        val iconSize = smallSize
        val paint = textPaint(smallSize, muted)
        val rowHeight = smallSize * 1.6f
        return Block(entries.size * rowHeight) { c, x, y, w ->
            val textW = if (showIcons) w - iconSize - 5f else w
            entries.forEachIndexed { index, (field, value) ->
                val baseline = y + index * rowHeight + smallSize
                val shown = ellipsize(value, paint, textW)
                if (rtl) {
                    if (showIcons) drawIcon(c, field, x, baseline - iconSize * 0.9f, iconSize, accentOn(muted), bg)
                    c.drawText(shown, if (showIcons) x + iconSize + 5f else x, baseline, paint)
                } else {
                    val iconX = x + w - iconSize
                    if (showIcons) drawIcon(c, field, iconX, baseline - iconSize * 0.9f, iconSize, accentOn(muted), bg)
                    val textRight = if (showIcons) iconX - 5f else x + w
                    c.drawText(shown, textRight - paint.measureText(shown), baseline, paint)
                }
            }
        }
    }

    private fun inlineContact(width: Float, centered: Boolean, color: Int, iconTint: Int? = null, iconHole: Int = bg): Block? {
        val entries = contactEntries()
        if (entries.isEmpty()) return null
        val showIcons = style.showContactIcons
        val paint = textPaint(smallSize, color)
        val iconSize = if (showIcons) smallSize * 0.98f else 0f
        val gap = if (showIcons) 11f else 16f
        val widths = entries.map { (_, value) -> min(width, iconSize + (if (showIcons) 3.5f else 0f) + paint.measureText(value)) }
        val rows = mutableListOf<MutableList<Int>>()
        var rowWidth = 0f
        widths.forEachIndexed { index, w ->
            if (rows.isEmpty() || rowWidth + gap + w > width) {
                rows.add(mutableListOf(index))
                rowWidth = w
            } else {
                rows.last().add(index)
                rowWidth += gap + w
            }
        }
        val rowHeight = smallSize * 1.55f
        return Block(rows.size * rowHeight) { c, x, y, w ->
            val separator = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = ColorUtils.setAlphaComponent(color, 120) }
            rows.forEachIndexed { rowIndex, row ->
                val total = row.sumOf { widths[it].toDouble() }.toFloat() + gap * (row.size - 1)
                var cursor = when {
                    centered -> x + (w - total) / 2
                    rtl -> x + w - total
                    else -> x
                }
                val baseline = y + rowIndex * rowHeight + smallSize
                row.forEachIndexed { position, index ->
                    val (field, value) = entries[index]
                    if (showIcons) {
                        drawIcon(c, field, cursor, baseline - iconSize * 0.9f, iconSize, iconTint ?: accentOn(color), iconHole)
                        cursor += iconSize + 3.5f
                    }
                    val available = widths[index] - (if (showIcons) iconSize + 3.5f else 0f)
                    val shown = ellipsize(value, paint, available)
                    c.drawText(shown, cursor, baseline, paint)
                    cursor += available
                    if (!showIcons && position < row.size - 1) {
                        c.drawText("|", cursor + gap / 2 - 1.5f, baseline, separator.apply { textSize = smallSize })
                    }
                    cursor += gap
                }
            }
        }
    }

    private fun accentOn(fallback: Int): Int = if (ColorUtils.calculateLuminance(accent) > 0.85) fallback else accent

    private fun ellipsize(value: String, paint: TextPaint, width: Float): String {
        if (paint.measureText(value) <= width) return value
        var end = value.length
        while (end > 1 && paint.measureText(value, 0, end) + paint.measureText("…") > width) end--
        return value.substring(0, end) + "…"
    }

    private fun drawIcon(c: Canvas, field: ContactField, left: Float, top: Float, s: Float, color: Int, hole: Int) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            strokeWidth = s * 0.11f
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }
        val cx = left + s / 2
        val cy = top + s / 2
        when (field) {
            ContactField.EMAIL -> {
                c.drawRoundRect(RectF(left, top + s * 0.2f, left + s, top + s * 0.84f), s * 0.1f, s * 0.1f, paint)
                c.drawPath(Path().apply { moveTo(left + s * 0.05f, top + s * 0.26f); lineTo(cx, cy + s * 0.08f); lineTo(left + s * 0.95f, top + s * 0.26f) }, paint)
            }
            ContactField.PHONE -> {
                c.drawRoundRect(RectF(left + s * 0.26f, top, left + s * 0.74f, top + s), s * 0.12f, s * 0.12f, paint)
                paint.style = Paint.Style.FILL
                c.drawCircle(cx, top + s * 0.8f, s * 0.07f, paint)
            }
            ContactField.LOCATION -> {
                paint.style = Paint.Style.FILL
                c.drawPath(Path().apply {
                    addCircle(cx, top + s * 0.38f, s * 0.34f, Path.Direction.CW)
                    moveTo(cx - s * 0.3f, top + s * 0.52f); lineTo(cx + s * 0.3f, top + s * 0.52f); lineTo(cx, top + s); close()
                }, paint)
                paint.color = hole
                c.drawCircle(cx, top + s * 0.38f, s * 0.13f, paint)
            }
            ContactField.WEBSITE -> {
                c.drawCircle(cx, cy, s * 0.45f, paint)
                c.drawOval(RectF(cx - s * 0.2f, top + s * 0.05f, cx + s * 0.2f, top + s * 0.95f), paint)
                c.drawLine(left + s * 0.06f, cy, left + s * 0.94f, cy, paint)
            }
            ContactField.LINKEDIN, ContactField.GITHUB -> {
                paint.style = Paint.Style.FILL
                if (field == ContactField.LINKEDIN) {
                    c.drawRoundRect(RectF(left, top, left + s, top + s), s * 0.18f, s * 0.18f, paint)
                } else {
                    c.drawCircle(cx, cy, s * 0.5f, paint)
                }
                val label = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = hole
                    typeface = Typeface.DEFAULT_BOLD
                    textSize = s * if (field == ContactField.LINKEDIN) 0.66f else 0.5f
                    textAlign = Paint.Align.CENTER
                }
                c.drawText(if (field == ContactField.LINKEDIN) "in" else "</>", cx, cy + label.textSize * 0.36f, label)
            }
            ContactField.BIRTHDAY -> {
                c.drawRoundRect(RectF(left, top + s * 0.14f, left + s, top + s), s * 0.1f, s * 0.1f, paint)
                c.drawLine(left, top + s * 0.4f, left + s, top + s * 0.4f, paint)
                c.drawLine(left + s * 0.3f, top, left + s * 0.3f, top + s * 0.24f, paint)
                c.drawLine(left + s * 0.7f, top, left + s * 0.7f, top + s * 0.24f, paint)
            }
        }
    }
    // endregion

    // region Sections
    private fun visibleSections(): List<ResumeSection> = content.sections.filter { section ->
        section.visible && when (section.type) {
            SectionType.CONTACT -> false
            SectionType.SUMMARY -> content.summary.isNotBlank()
            SectionType.SIGNATURE -> assets.signature != null || content.signature.fullName.isNotBlank()
            else -> section.items.any { it.title.isNotBlank() }
        }
    }

    private fun sectionBlocks(section: ResumeSection, width: Float, onSidebar: Boolean, compactAsList: Boolean): List<Block> {
        val bodyColor = if (onSidebar) sidebarText else text
        val body: List<Block> = when (section.type) {
            SectionType.CONTACT -> emptyList()
            SectionType.SIGNATURE -> return signatureBlocks(width) + spacer(sectionGap)
            SectionType.SUMMARY -> richText(content.summary, width, bodyColor)
            SectionType.SKILLS, SectionType.LANGUAGES, SectionType.INTERESTS ->
                if (compactAsList) compactList(section, width, onSidebar) else inlineList(section, width)
            SectionType.REFERENCES -> section.items.filter { it.title.isNotBlank() }.flatMapIndexed { index, item ->
                referenceBlocks(item, width, onSidebar) + if (index < section.items.lastIndex) listOf(spacer(itemGap)) else emptyList()
            }
            SectionType.CUSTOM -> if (section.customLayout == CustomLayout.TITLE) {
                compactList(section, width, onSidebar)
            } else {
                entries(section, width, onSidebar)
            }
            else -> entries(section, width, onSidebar)
        }
        if (body.isEmpty()) return emptyList()
        return listOf(heading(lang.headingFor(section), width, onSidebar)) + body + spacer(sectionGap)
    }

    private fun heading(title: String, width: Float, onSidebar: Boolean): Block {
        val baseColor = if (onSidebar) (if (spec.heading == HeadingStyle.UPPERCASE_ACCENT) accent else sidebarText) else headingColor
        return when (spec.heading) {
            HeadingStyle.UNDERLINE -> {
                val layout = staticLayout(title, textPaint(headingSize, baseColor, bold, letterSpacing + 0.02f), width)
                val lineColor = if (onSidebar) ColorUtils.setAlphaComponent(sidebarText, 150) else ColorUtils.blendARGB(headingColor, bg, 0.35f)
                Block(layout.height + 5f + headingGap, keepWithNext = true) { c, x, y, w ->
                    c.drawLayout(layout, x, y)
                    c.drawLine(x, y + layout.height + 2.6f, x + w, y + layout.height + 2.6f, Paint().apply { color = lineColor; strokeWidth = 0.9f })
                }
            }
            HeadingStyle.FILLED_BAR -> {
                val onAccent = if (ColorUtils.calculateLuminance(accent) > 0.6) 0xFF1B1E27.toInt() else white
                val layout = staticLayout(title, textPaint(headingSize - 0.5f, onAccent, bold, letterSpacing + 0.02f), width - 12f)
                Block(layout.height + 7f + headingGap, keepWithNext = true) { c, x, y, w ->
                    c.drawRect(x, y, x + w, y + layout.height + 7f, Paint().apply { color = accent })
                    c.drawLayout(layout, x + 6f, y + 3.5f)
                }
            }
            HeadingStyle.BOXED -> {
                val box = if (onSidebar && sidebarDark) white else accent
                val label = if (onSidebar && sidebarDark) 0xFF1B1E27.toInt() else white
                val layout = staticLayout(title, textPaint(headingSize - 0.5f, label, bold, letterSpacing + 0.06f), width - 8f, Layout.Alignment.ALIGN_CENTER)
                Block(layout.height + 8f + headingGap, keepWithNext = true) { c, x, y, w ->
                    c.drawRect(x, y, x + w, y + layout.height + 8f, Paint().apply { color = box })
                    c.drawLayout(layout, x + 4f, y + 4f)
                }
            }
            HeadingStyle.UPPERCASE_ACCENT -> {
                val color = if (onSidebar) accent else headingColor
                val layout = staticLayout(title.uppercase(), textPaint(headingSize - 1f, color, bold, letterSpacing + 0.12f), width)
                Block(layout.height + 6f + headingGap, keepWithNext = true) { c, x, y, w ->
                    c.drawLayout(layout, x, y)
                    val barX = if (rtl) x + w - 24f else x
                    c.drawRect(barX, y + layout.height + 2.5f, barX + 24f, y + layout.height + 4.5f, Paint().apply { this.color = accent })
                }
            }
            HeadingStyle.LEFT_BAR -> {
                val layout = staticLayout(title, textPaint(headingSize, baseColor, bold, letterSpacing + 0.02f), width - 10f)
                Block(layout.height + headingGap + 2f, keepWithNext = true) { c, x, y, w ->
                    val barX = if (rtl) x + w - 3.2f else x
                    c.drawRect(barX, y + 1f, barX + 3.2f, y + layout.height - 1f, Paint().apply { color = accent })
                    c.drawLayout(layout, if (rtl) x else x + 10f, y)
                }
            }
            HeadingStyle.CENTERED_RULE -> {
                val paint = textPaint(headingSize - 0.5f, baseColor, bold, letterSpacing + 0.1f)
                val label = title.uppercase()
                val labelWidth = min(width * 0.8f, paint.measureText(label))
                val layout = staticLayout(label, paint, width, Layout.Alignment.ALIGN_CENTER)
                Block(layout.height + headingGap + 2f, keepWithNext = true) { c, x, y, w ->
                    c.drawLayout(layout, x, y)
                    val mid = y + layout.height / 2f
                    val rule = Paint().apply { color = ColorUtils.blendARGB(bg, text, 0.3f); strokeWidth = 0.8f }
                    c.drawLine(x, mid, x + (w - labelWidth) / 2 - 10f, mid, rule)
                    c.drawLine(x + (w + labelWidth) / 2 + 10f, mid, x + w, mid, rule)
                }
            }
            HeadingStyle.PILL -> {
                val fill = if (onSidebar && sidebarDark) ColorUtils.setAlphaComponent(white, 40) else ColorUtils.blendARGB(bg, accent, 0.14f)
                val labelColor = if (onSidebar && sidebarDark) white else headingColor
                val paint = textPaint(headingSize - 1f, labelColor, bold, letterSpacing + 0.08f)
                val label = title.uppercase()
                val textW = min(width - 20f, paint.measureText(label) + 2f)
                val layout = staticLayout(label, paint, textW)
                val pillH = layout.height + 8f
                Block(pillH + headingGap + 1f, keepWithNext = true) { c, x, y, w ->
                    val pillW = textW + 20f
                    val left = if (rtl) x + w - pillW else x
                    c.drawRoundRect(RectF(left, y, left + pillW, y + pillH), pillH / 2, pillH / 2, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = fill })
                    c.drawLayout(layout, left + 10f, y + 4f)
                }
            }
            HeadingStyle.DOUBLE_RULE -> {
                val layout = staticLayout(title.uppercase(), textPaint(headingSize - 0.5f, baseColor, bold, letterSpacing + 0.14f), width)
                val lineColor = if (onSidebar) ColorUtils.setAlphaComponent(sidebarText, 150) else accent
                Block(layout.height + 10f + headingGap, keepWithNext = true) { c, x, y, w ->
                    val rule = Paint().apply { color = lineColor; strokeWidth = 1.4f }
                    c.drawLine(x, y + 1f, x + w, y + 1f, rule)
                    c.drawLayout(layout, x, y + 5f)
                    rule.strokeWidth = 0.6f
                    c.drawLine(x, y + layout.height + 8f, x + w, y + layout.height + 8f, rule)
                }
            }
            HeadingStyle.DOT_RULE -> {
                val marker = 7f
                val textArea = width - marker - 8f
                val layout = staticLayout(title, textPaint(headingSize, baseColor, bold, letterSpacing + 0.02f), textArea)
                val textW = if (layout.lineCount == 1) layout.getLineWidth(0) else textArea
                val lineColor = if (onSidebar) ColorUtils.setAlphaComponent(sidebarText, 110) else ColorUtils.blendARGB(bg, text, 0.2f)
                Block(layout.height + headingGap + 2f, keepWithNext = true) { c, x, y, w ->
                    val mid = y + layout.height / 2f
                    val markPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (onSidebar && sidebarDark) white else accent }
                    val rule = Paint().apply { color = lineColor; strokeWidth = 0.8f }
                    if (rtl) {
                        c.drawRect(x + w - marker, mid - marker / 2, x + w, mid + marker / 2, markPaint)
                        c.drawLayout(layout, x, y)
                        val end = x + textArea - textW - 8f
                        if (end > x) c.drawLine(x, mid, end, mid, rule)
                    } else {
                        c.drawRect(x, mid - marker / 2, x + marker, mid + marker / 2, markPaint)
                        c.drawLayout(layout, x + marker + 8f, y)
                        val start = x + marker + 8f + textW + 8f
                        if (start < x + w) c.drawLine(start, mid, x + w, mid, rule)
                    }
                }
            }
            HeadingStyle.TAB -> {
                val tabColor = if (onSidebar && sidebarDark) white else accent
                val labelColor = if (ColorUtils.calculateLuminance(tabColor) > 0.6) 0xFF1B1E27.toInt() else white
                val paint = textPaint(headingSize - 1f, labelColor, bold, letterSpacing + 0.06f)
                val textW = min(width - 16f, paint.measureText(title) + 2f)
                val layout = staticLayout(title, paint, textW)
                val tabH = layout.height + 7f
                Block(tabH + 1.5f + headingGap, keepWithNext = true) { c, x, y, w ->
                    val tabW = textW + 16f
                    val left = if (rtl) x + w - tabW else x
                    val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = tabColor }
                    c.drawRoundRect(RectF(left, y, left + tabW, y + tabH), 5f, 5f, fill)
                    c.drawRect(left, y + tabH / 2, left + tabW, y + tabH, fill)
                    c.drawRect(x, y + tabH, x + w, y + tabH + 1.5f, fill)
                    c.drawLayout(layout, left + 8f, y + 3.5f)
                }
            }
        }
    }

    private fun entries(section: ResumeSection, width: Float, onSidebar: Boolean): List<Block> {
        val fields = section.type.fieldsFor(section.customLayout)
        val items = section.items.filter { it.title.isNotBlank() }
        val timeline = spec.timelineDates && !onSidebar && fields.hasDates && width > 250f
        return items.flatMapIndexed { index, item ->
            entryBlocks(item, fields, width, onSidebar, timeline) + if (index < items.lastIndex) listOf(spacer(itemGap)) else emptyList()
        }
    }

    private fun entryBlocks(item: SectionItem, fields: ItemFields, width: Float, onSidebar: Boolean, timeline: Boolean): List<Block> {
        val titleColor = if (onSidebar) sidebarText else strong
        val secondary = if (onSidebar) sidebarMuted else if (spec.timelineDates) muted else subtitleColor
        val date = if (fields.hasDates) item.dateRange(lang.present) else ""
        val gutter = if (timeline) 66f else 0f
        val contentWidth = width - gutter
        val titlePaint = textPaint(bodySize + 1f, titleColor, bold)
        val datePaint = textPaint(smallSize, if (onSidebar) sidebarMuted else muted, if (timeline) bold else regular)
        val subtitle = listOf(item.subtitle, item.location).filter { it.isNotBlank() }.joinToString(" · ")

        val headerParts = mutableListOf<Block>()
        val header: Block
        if (timeline) {
            val dateLayout = staticLayout(date.replace(" – ", " –\n"), datePaint, gutter - 8f)
            val titleLayout = staticLayout(item.title, titlePaint, contentWidth)
            val subLayout = subtitle.takeIf { it.isNotBlank() }?.let { staticLayout(it, textPaint(smallSize + 0.3f, secondary, italic), contentWidth) }
            val h = max(dateLayout.height.toFloat(), titleLayout.height + (subLayout?.height ?: 0) + 1f) + 3f
            header = Block(h, keepWithNext = item.description.isNotBlank()) { c, x, y, w ->
                val dateX = if (rtl) x + w - gutter + 8f else x
                val bodyX = if (rtl) x else x + gutter
                c.drawLayout(dateLayout, dateX, y + 1f)
                c.drawLayout(titleLayout, bodyX, y)
                subLayout?.let { c.drawLayout(it, bodyX, y + titleLayout.height + 1f) }
            }
        } else {
            val dateWidth = if (date.isBlank()) 0f else min(width * 0.4f, datePaint.measureText(date) + 10f)
            val titleLayout = staticLayout(item.title, titlePaint, width - dateWidth)
            val dateLayout = date.takeIf { it.isNotBlank() }?.let { staticLayout(it, datePaint, dateWidth - 8f) }
            headerParts += Block(max(titleLayout.height.toFloat(), (dateLayout?.height ?: 0).toFloat()) + 1f) { c, x, y, w ->
                c.drawLayout(titleLayout, if (rtl) x + dateWidth else x, y)
                dateLayout?.let { c.drawLayout(it, if (rtl) x else x + w - dateWidth + 10f, y + 1.2f) }
            }
            if (subtitle.isNotBlank()) {
                headerParts += textBlock(subtitle, textPaint(smallSize + 0.3f, secondary, italic), width, after = 1f)
            }
            header = stack(headerParts).let { Block(it.height + 2f, keepWithNext = item.description.isNotBlank(), draw = it.draw) }
        }

        val blocks = mutableListOf(header)
        val tail = mutableListOf<Block>()
        if (item.link.isNotBlank() && fields.linkLabel != null && fields != SectionType.REFERENCES.fields) {
            tail += textBlock(item.link.removePrefix("https://"), textPaint(smallSize, accentOn(text)), contentWidth, after = 1.5f)
        }
        val levelLabels = lang.levelLabels(fields)
        if (item.level > 0 && levelLabels != null) {
            tail += textBlock(levelLabels.getOrElse(item.level - 1) { "" }, textPaint(smallSize, secondary), contentWidth)
        }
        tail += richText(item.description, contentWidth, if (onSidebar) sidebarText else text)
        blocks += if (timeline) tail.map { offset(it, if (rtl) 0f else gutter, gutter) } else tail
        return blocks
    }

    private fun compactList(section: ResumeSection, width: Float, onSidebar: Boolean): List<Block> {
        val color = if (onSidebar) sidebarText else text
        val paint = textPaint(bodySize, color)
        val levels = lang.levelLabels(section.type.fields)
        val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG)
        return section.items.filter { it.title.isNotBlank() }.map { item ->
            when {
                section.type == SectionType.SKILLS && spec.skillBars && item.level > 0 -> {
                    val layout = staticLayout(item.title, paint, width)
                    Block(layout.height + 9f) { c, x, y, w ->
                        c.drawLayout(layout, x, y)
                        val barTop = y + layout.height + 2.5f
                        dotPaint.color = if (onSidebar && sidebarDark) ColorUtils.setAlphaComponent(white, 60) else ColorUtils.blendARGB(bg, text, 0.12f)
                        c.drawRoundRect(RectF(x, barTop, x + w, barTop + 3f), 1.5f, 1.5f, dotPaint)
                        dotPaint.color = accent
                        val filled = w * item.level / 5f
                        val start = if (rtl) x + w - filled else x
                        c.drawRoundRect(RectF(start, barTop, start + filled, barTop + 3f), 1.5f, 1.5f, dotPaint)
                    }
                }
                item.level > 0 && levels != null -> {
                    val dotsWidth = 5 * 7f
                    val layout = staticLayout(item.title, paint, width - dotsWidth - 6f)
                    Block(max(layout.height.toFloat(), 9f) + 4f) { c, x, y, w ->
                        c.drawLayout(layout, if (rtl) x + dotsWidth + 6f else x, y)
                        val startX = if (rtl) x else x + w - dotsWidth
                        val cy = y + layout.height / 2f
                        repeat(5) { i ->
                            dotPaint.color = if (i < item.level) {
                                if (onSidebar && sidebarDark) white else accent
                            } else {
                                if (onSidebar && sidebarDark) ColorUtils.setAlphaComponent(white, 70) else ColorUtils.blendARGB(bg, text, 0.15f)
                            }
                            c.drawCircle(startX + 3f + i * 7f, cy, 2.4f, dotPaint)
                        }
                    }
                }
                spec.heading == HeadingStyle.BOXED -> {
                    val layout = staticLayout(item.title, textPaint(bodySize, color, bold), width)
                    Block(layout.height + 5f) { c, x, y, _ -> c.drawLayout(layout, x, y) }
                }
                else -> {
                    val indent = 9f
                    val layout = staticLayout(item.title, paint, width - indent)
                    Block(layout.height + 3f) { c, x, y, w ->
                        dotPaint.color = if (onSidebar) sidebarMuted else accentOn(color)
                        c.drawCircle(if (rtl) x + w - 3f else x + 3f, y + bodySize * 0.58f, 1.3f, dotPaint)
                        c.drawLayout(layout, if (rtl) x else x + indent, y)
                    }
                }
            }
        }
    }

    private fun inlineList(section: ResumeSection, width: Float): List<Block> {
        val levels = lang.levelLabels(section.type.fields)
        val parts = section.items.filter { it.title.isNotBlank() }.map { item ->
            val level = levels?.getOrNull(item.level - 1)
            if (section.type == SectionType.LANGUAGES && level != null) "${item.title} ($level)" else item.title
        }
        if (parts.isEmpty()) return emptyList()
        return listOf(textBlock(parts.joinToString("   •   "), textPaint(bodySize, text), width, after = 1f))
    }

    private fun referenceBlocks(item: SectionItem, width: Float, onSidebar: Boolean): List<Block> {
        val blocks = mutableListOf<Block>()
        blocks += textBlock(item.title, textPaint(bodySize + 0.6f, if (onSidebar) sidebarText else strong, bold), width, keep = true)
        if (item.subtitle.isNotBlank()) {
            blocks += textBlock(item.subtitle, textPaint(smallSize + 0.2f, if (onSidebar) sidebarMuted else muted, italic), width)
        }
        val reach = listOf(item.location, item.link).filter { it.isNotBlank() }.joinToString("  ·  ")
        if (reach.isNotBlank()) blocks += textBlock(reach, textPaint(smallSize, if (onSidebar) sidebarMuted else muted), width)
        return blocks
    }

    private fun signatureBlocks(width: Float): List<Block> {
        val signature = content.signature
        val image = assets.signature
        val imageHeight = if (image != null) 44f else 0f
        val imageWidth = if (image != null) min(width * 0.5f, imageHeight * image.width / max(1, image.height)) else 0f
        val namePaint = textPaint(bodySize, strong, bold)
        val metaPaint = textPaint(smallSize, muted)
        val meta = listOf(signature.place, signature.date).filter { it.isNotBlank() }.joinToString(", ")
        val lineWidth = max(150f, imageWidth)
        val height = imageHeight + 6f + (if (signature.fullName.isNotBlank()) bodySize * 1.5f else 0f) + (if (meta.isNotBlank()) smallSize * 1.5f else 0f)
        return listOf(
            spacer(10f),
            Block(height) { c, x, y, w ->
                val left = if (rtl) x + w - lineWidth else x
                image?.let {
                    c.drawBitmap(it, null, RectF(left, y, left + imageWidth, y + imageHeight), Paint(Paint.FILTER_BITMAP_FLAG))
                }
                val lineY = y + imageHeight + 2f
                c.drawLine(left, lineY, left + lineWidth, lineY, Paint().apply { color = ColorUtils.blendARGB(bg, text, 0.4f); strokeWidth = 0.7f })
                var cursor = lineY + bodySize + 3f
                if (signature.fullName.isNotBlank()) {
                    c.drawText(signature.fullName, left, cursor, namePaint)
                    cursor += smallSize * 1.5f
                }
                if (meta.isNotBlank()) c.drawText(meta, left, cursor, metaPaint)
            },
        )
    }
    // endregion

    private companion object {
        val BULLET_CHARS = setOf('•', '-', '*', '–', '▪', '●', '◦')
    }
}
