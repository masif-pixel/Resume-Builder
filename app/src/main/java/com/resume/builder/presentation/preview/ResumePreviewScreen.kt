package com.resume.builder.presentation.preview

import android.graphics.Typeface
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Contacts
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FontDownload
import androidx.compose.material.icons.rounded.FormatLineSpacing
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.ViewColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resume.builder.domain.model.ContactField
import com.resume.builder.domain.model.LayoutKind
import com.resume.builder.domain.model.PageSize
import com.resume.builder.domain.model.Resume
import com.resume.builder.domain.model.ResumeFont
import com.resume.builder.domain.model.ResumeLanguage
import com.resume.builder.domain.model.ResumeStyle
import com.resume.builder.domain.model.SavedFile
import com.resume.builder.domain.model.TemplateCatalog
import com.resume.builder.presentation.components.AppTopBar
import com.resume.builder.presentation.components.ChoiceChips
import com.resume.builder.presentation.components.ColorPickerDialog
import com.resume.builder.presentation.components.ExportedDialog
import com.resume.builder.presentation.components.LabeledSlider
import com.resume.builder.presentation.components.PagesPreview
import com.resume.builder.presentation.components.SwatchRow
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ResumePreviewScreen(
    resumeId: String,
    onBack: () -> Unit,
    onEditContent: () -> Unit,
    onOpenSaved: () -> Unit,
    viewModel: ResumePreviewViewModel = koinViewModel(key = "preview-$resumeId") { parametersOf(resumeId) },
) {
    val resume by viewModel.resume.collectAsStateWithLifecycle()
    val pages by viewModel.pages.collectAsStateWithLifecycle()
    val rendering by viewModel.rendering.collectAsStateWithLifecycle()
    val exporting by viewModel.exporting.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var exported by remember { mutableStateOf<SavedFile?>(null) }
    var category by rememberSaveable { mutableStateOf(CustomizeCategory.TEMPLATE) }
    var panelExpanded by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PreviewEvent.Exported -> exported = event.file
                is PreviewEvent.Error -> snackbar.showSnackbar(event.message)
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Preview",
                subtitle = resume?.let { TemplateCatalog.byId(it.templateId).name },
                onBack = onBack,
                actions = {
                    IconButton(onClick = viewModel::undo, enabled = canUndo) { Icon(Icons.AutoMirrored.Rounded.Undo, "Undo") }
                    IconButton(onClick = onEditContent) { Icon(Icons.Rounded.EditNote, "Edit content") }
                    Button(onClick = viewModel::export, enabled = !exporting && resume != null, contentPadding = PaddingValues(horizontal = 14.dp), modifier = Modifier.padding(end = 8.dp)) {
                        if (exporting) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White) else Icon(Icons.Rounded.Download, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save PDF")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Box(Modifier.weight(1f).fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer)) {
                if (pages.isEmpty()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else {
                    PagesPreview(pages, Modifier.fillMaxSize())
                }
                if (rendering && pages.isNotEmpty()) {
                    LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
                }
            }
            resume?.let { current ->
                CustomizePanel(
                    resume = current,
                    category = category,
                    expanded = panelExpanded,
                    onToggleExpanded = { panelExpanded = !panelExpanded },
                    onCategory = { category = it; panelExpanded = true },
                    viewModel = viewModel,
                )
            }
        }
    }

    exported?.let { file ->
        ExportedDialog(file = file, onDismiss = { exported = null }, onViewSaved = { exported = null; onOpenSaved() }, message = "Your resume PDF is ready to send.")
    }
}

private fun CustomizeCategory.icon(): ImageVector = when (this) {
    CustomizeCategory.TEMPLATE -> Icons.Rounded.Dashboard
    CustomizeCategory.COLORS -> Icons.Rounded.Palette
    CustomizeCategory.FONT -> Icons.Rounded.FontDownload
    CustomizeCategory.SIZE -> Icons.Rounded.FormatSize
    CustomizeCategory.SPACING -> Icons.Rounded.FormatLineSpacing
    CustomizeCategory.LAYOUT -> Icons.Rounded.ViewColumn
    CustomizeCategory.PHOTO -> Icons.Rounded.Person
    CustomizeCategory.CONTACT -> Icons.Rounded.Contacts
    CustomizeCategory.LANGUAGE -> Icons.Rounded.Language
}

@Composable
private fun CustomizePanel(
    resume: Resume,
    category: CustomizeCategory,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onCategory: (CustomizeCategory) -> Unit,
    viewModel: ResumePreviewViewModel,
) {
    Surface(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Column(Modifier.navigationBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Customize", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (category != CustomizeCategory.TEMPLATE) {
                    TextButton(onClick = { viewModel.reset(category) }) {
                        Icon(Icons.Rounded.RestartAlt, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Reset")
                    }
                }
                IconButton(onClick = onToggleExpanded) { Icon(if (expanded) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess, if (expanded) "Collapse" else "Expand") }
            }
            Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CustomizeCategory.entries.forEach { item ->
                    FilterChip(
                        selected = item == category && expanded,
                        onClick = { onCategory(item) },
                        label = { Text(item.label) },
                        leadingIcon = { Icon(item.icon(), null, Modifier.size(18.dp)) },
                        shape = RoundedCornerShape(50),
                    )
                }
            }
            AnimatedVisibility(expanded) {
                Box(Modifier.fillMaxWidth().heightIn(max = 290.dp).padding(top = 8.dp, bottom = 12.dp)) {
                    when (category) {
                        CustomizeCategory.TEMPLATE -> TemplateOptions(resume, viewModel)
                        CustomizeCategory.COLORS -> ColorOptions(resume, viewModel)
                        CustomizeCategory.FONT -> FontOptions(resume.style, viewModel)
                        CustomizeCategory.SIZE -> ScrollColumn {
                            val s = resume.style
                            LabeledSlider("Name", s.nameSize, { v -> viewModel.updateStyle { it.copy(nameSize = v) } }, -8f..12f, steps = 19)
                            LabeledSlider("Job title", s.jobTitleSize, { v -> viewModel.updateStyle { it.copy(jobTitleSize = v) } }, -4f..8f, steps = 11)
                            LabeledSlider("Section titles", s.headingSize, { v -> viewModel.updateStyle { it.copy(headingSize = v) } }, -3f..6f, steps = 17, valueLabel = ::signed1)
                            LabeledSlider("Body text", s.bodySize, { v -> viewModel.updateStyle { it.copy(bodySize = v) } }, -2f..4f, steps = 11, valueLabel = ::signed1)
                        }
                        CustomizeCategory.SPACING -> ScrollColumn {
                            val s = resume.style
                            LabeledSlider("Side margins", s.marginHorizontal, { v -> viewModel.updateStyle { it.copy(marginHorizontal = v) } }, -20f..40f)
                            LabeledSlider("Top & bottom margins", s.marginVertical, { v -> viewModel.updateStyle { it.copy(marginVertical = v) } }, -20f..40f)
                            LabeledSlider("Space between sections", s.sectionSpacing, { v -> viewModel.updateStyle { it.copy(sectionSpacing = v) } }, -8f..24f)
                            LabeledSlider("Space under titles", s.headingSpacing, { v -> viewModel.updateStyle { it.copy(headingSpacing = v) } }, -5f..14f)
                            LabeledSlider("Line spacing", s.lineSpacing, { v -> viewModel.updateStyle { it.copy(lineSpacing = v) } }, 0f..0.8f, valueLabel = { "×%.2f".format(1.12f + it) })
                            LabeledSlider("Letter spacing", s.letterSpacing, { v -> viewModel.updateStyle { it.copy(letterSpacing = v) } }, -0.03f..0.12f, valueLabel = { "%.2f".format(it) })
                        }
                        CustomizeCategory.LAYOUT -> LayoutOptions(resume, viewModel)
                        CustomizeCategory.PHOTO -> PhotoOptions(resume, viewModel)
                        CustomizeCategory.CONTACT -> ContactOptions(resume.style, viewModel)
                        CustomizeCategory.LANGUAGE -> ScrollColumn {
                            Text(
                                "Choose the language your resume is written in. Section titles are translated and right-to-left layout is applied for Arabic, Urdu, Persian, Hebrew and Pashto. Your own text isn't translated.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.height(10.dp))
                            ResumeLanguage.entries.chunked(2).forEach { row ->
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                                    row.forEach { lang ->
                                        OptionTile(lang.nativeName, selected = resume.style.language == lang, modifier = Modifier.weight(1f)) {
                                            viewModel.updateStyle { it.copy(language = lang) }
                                        }
                                    }
                                    repeat(2 - row.size) { Spacer(Modifier.weight(1f)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun signed1(v: Float): String = if (v > 0) "+%.1f".format(v) else "%.1f".format(v)

@Composable
private fun ScrollColumn(content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp)) { content() }
}

@Composable
private fun OptionTile(label: String, selected: Boolean, modifier: Modifier = Modifier, fontFamily: FontFamily? = null, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 52.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(if (selected) 2.dp else 0.dp, if (selected) MaterialTheme.colorScheme.primary else Color.Transparent),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, fontFamily = fontFamily, maxLines = 1)
        }
    }
}

@Composable
private fun TemplateOptions(resume: Resume, viewModel: ResumePreviewViewModel) {
    val thumbnails by viewModel.thumbnails.collectAsStateWithLifecycle()
    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        items(TemplateCatalog.all, key = { it.id }) { spec ->
            val selected = spec.id == resume.templateId
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(108.dp).clickable { viewModel.setTemplate(spec.id) }) {
                Box(
                    Modifier.fillMaxWidth().aspectRatio(0.707f).clip(RoundedCornerShape(10.dp))
                        .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    thumbnails[spec.id]?.let { Image(it.asImageBitmap(), spec.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                        ?: CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                }
                Spacer(Modifier.height(6.dp))
                Text(spec.name, style = MaterialTheme.typography.labelLarge, color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, maxLines = 1)
            }
        }
    }
}

private enum class ColorTarget(val label: String) {
    ACCENT("Accent"), SIDEBAR("Sidebar"), BACKGROUND("Background"), NAME("Name"), JOB_TITLE("Job title"),
    HEADINGS("Headings"), TEXT("Text"), PHOTO_BORDER("Photo border"),
}

private fun ResumeStyle.colorOf(target: ColorTarget): Long? = when (target) {
    ColorTarget.ACCENT -> accentColor
    ColorTarget.SIDEBAR -> sidebarColor
    ColorTarget.BACKGROUND -> backgroundColor
    ColorTarget.NAME -> nameColor
    ColorTarget.JOB_TITLE -> jobTitleColor
    ColorTarget.HEADINGS -> headingColor
    ColorTarget.TEXT -> textColor
    ColorTarget.PHOTO_BORDER -> photoBorderColor
}

private fun ResumeStyle.withColor(target: ColorTarget, value: Long?): ResumeStyle = when (target) {
    ColorTarget.ACCENT -> copy(accentColor = value)
    ColorTarget.SIDEBAR -> copy(sidebarColor = value)
    ColorTarget.BACKGROUND -> copy(backgroundColor = value)
    ColorTarget.NAME -> copy(nameColor = value)
    ColorTarget.JOB_TITLE -> copy(jobTitleColor = value)
    ColorTarget.HEADINGS -> copy(headingColor = value)
    ColorTarget.TEXT -> copy(textColor = value)
    ColorTarget.PHOTO_BORDER -> copy(photoBorderColor = value)
}

@Composable
private fun ColorOptions(resume: Resume, viewModel: ResumePreviewViewModel) {
    val spec = TemplateCatalog.byId(resume.templateId)
    val targets = ColorTarget.entries.filter { it != ColorTarget.SIDEBAR || spec.layout == LayoutKind.SIDEBAR }
    var target by rememberSaveable { mutableStateOf(ColorTarget.ACCENT) }
    var picking by remember { mutableStateOf(false) }
    Column(Modifier.verticalScroll(rememberScrollState())) {
        ChoiceChips(options = targets, selected = target, label = { it.label }, onSelect = { target = it }, contentPadding = PaddingValues(horizontal = 16.dp))
        Spacer(Modifier.height(12.dp))
        SwatchRow(
            selected = resume.style.colorOf(target),
            onSelect = { argb -> viewModel.updateStyle { it.withColor(target, argb) } },
            onCustom = { picking = true },
        )
        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (resume.style.colorOf(target) == null) "Using template default" else "Custom ${target.label.lowercase()} color",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            if (resume.style.colorOf(target) != null) {
                TextButton(onClick = { viewModel.updateStyle { it.withColor(target, null) } }) { Text("Use default") }
            }
        }
    }
    if (picking) {
        val initial = Color((resume.style.colorOf(target) ?: spec.accent).toInt())
        ColorPickerDialog(
            initial = initial,
            title = "${target.label} color",
            onDismiss = { picking = false },
            onConfirm = { color ->
                picking = false
                val argb = color.value.toLong() ushr 32
                viewModel.updateStyle { it.withColor(target, argb) }
            },
        )
    }
}

@Composable
private fun FontOptions(style: ResumeStyle, viewModel: ResumePreviewViewModel) {
    ScrollColumn {
        ResumeFont.entries.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(bottom = 10.dp)) {
                row.forEach { font ->
                    val family = remember(font) { FontFamily(Typeface.create(font.family, Typeface.NORMAL)) }
                    OptionTile(font.label, selected = style.font == font, fontFamily = family, modifier = Modifier.weight(1f)) {
                        viewModel.updateStyle { it.copy(font = font) }
                    }
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun LayoutOptions(resume: Resume, viewModel: ResumePreviewViewModel) {
    val spec = TemplateCatalog.byId(resume.templateId)
    val style = resume.style
    ScrollColumn {
        Text("Page size", style = MaterialTheme.typography.titleSmall)
        Spacer(Modifier.height(8.dp))
        ChoiceChips(options = PageSize.entries, selected = style.pageSize, label = { it.label }, onSelect = { size -> viewModel.updateStyle { it.copy(pageSize = size) } })
        Spacer(Modifier.height(14.dp))
        if (spec.layout != LayoutKind.SINGLE_COLUMN) {
            LabeledSlider("Column width", style.columnWidth, { v -> viewModel.updateStyle { it.copy(columnWidth = v) } }, -10f..12f, steps = 21, valueLabel = { "${if (it > 0) "+" else ""}${it.toInt()}%" })
            Row(Modifier.fillMaxWidth().clickable { viewModel.updateStyle { it.copy(swapColumns = !it.swapColumns) } }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Swap columns", style = MaterialTheme.typography.bodyLarge)
                    Text("Move the narrow column to the other side", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(checked = style.swapColumns, onCheckedChange = { checked -> viewModel.updateStyle { it.copy(swapColumns = checked) } })
            }
        } else {
            Text("This template uses a single column. Try Midnight, Evergreen or Editorial for two-column layouts.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PhotoOptions(resume: Resume, viewModel: ResumePreviewViewModel) {
    val style = resume.style
    val hasPhoto = resume.content.contact.photoPath != null
    ScrollColumn {
        if (!hasPhoto) {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                Text("Add a profile photo in the Contact section to use these options.", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(12.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Show profile photo", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = style.showPhoto, onCheckedChange = { checked -> viewModel.updateStyle { it.copy(showPhoto = checked) } })
        }
        LabeledSlider("Photo size", style.photoSize, { v -> viewModel.updateStyle { it.copy(photoSize = v) } }, -40f..50f)
        LabeledSlider(
            "Corner roundness", style.photoCornerRadius ?: if (TemplateCatalog.byId(resume.templateId).photoRound) 50f else 10f,
            { v -> viewModel.updateStyle { it.copy(photoCornerRadius = v) } }, 0f..50f, valueLabel = { "${it.toInt()}%" },
        )
        LabeledSlider("Border width", style.photoBorderWidth ?: 2f, { v -> viewModel.updateStyle { it.copy(photoBorderWidth = v) } }, 0f..8f, steps = 15, valueLabel = { "%.1f".format(it) })
    }
}

@Composable
private fun ContactOptions(style: ResumeStyle, viewModel: ResumePreviewViewModel) {
    ScrollColumn {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("Show contact icons", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = style.showContactIcons, onCheckedChange = { checked -> viewModel.updateStyle { it.copy(showContactIcons = checked) } })
        }
        Text("Order & visibility", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
        style.contactOrder.forEachIndexed { index, field ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = field !in style.hiddenContactFields,
                    onCheckedChange = { show ->
                        viewModel.updateStyle { it.copy(hiddenContactFields = if (show) it.hiddenContactFields - field else it.hiddenContactFields + field) }
                    },
                )
                Text(field.label, modifier = Modifier.weight(1f))
                IconButton(onClick = { viewModel.updateStyle { it.copy(contactOrder = it.contactOrder.moved(index, -1)) } }, enabled = index > 0) {
                    Icon(Icons.Rounded.KeyboardArrowUp, "Move up")
                }
                IconButton(onClick = { viewModel.updateStyle { it.copy(contactOrder = it.contactOrder.moved(index, 1)) } }, enabled = index < style.contactOrder.lastIndex) {
                    Icon(Icons.Rounded.KeyboardArrowDown, "Move down")
                }
            }
        }
    }
}

private fun List<ContactField>.moved(index: Int, delta: Int): List<ContactField> {
    val target = index + delta
    if (target !in indices) return this
    return toMutableList().apply { add(target, removeAt(index)) }
}
