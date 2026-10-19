package com.resume.builder.presentation.pdftools

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.resume.builder.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pixelforge.ads.nativead.NativeAdSlot
import com.pixelforge.ads.nativead.NativeAdTemplate
import com.pixelforge.ads.remoteconfig.AdFormat
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resume.builder.data.pdf.CompressionLevel
import com.resume.builder.data.pdf.ImageFormat
import com.resume.builder.data.pdf.ImageQuality
import com.resume.builder.data.pdf.ToolOrientation
import com.resume.builder.data.pdf.ToolPageSize
import com.resume.builder.presentation.components.AppTopBar
import com.resume.builder.presentation.components.ChoiceChips
import com.resume.builder.presentation.components.FileActions
import com.resume.builder.presentation.components.rememberFileOpener
import com.resume.builder.presentation.components.GradientButton
import com.resume.builder.presentation.components.LabeledSlider
import com.resume.builder.presentation.components.LoadingDialog
import com.resume.builder.presentation.components.SignaturePad
import com.resume.builder.presentation.components.formatBytes
import com.resume.builder.presentation.components.rememberSignatureState
import com.resume.builder.ui.theme.Success
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.math.roundToInt

@Composable
fun PdfToolsScreen(onBack: () -> Unit, onTool: (PdfTool) -> Unit) {
    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.drawer_pdf_tools), subtitle = stringResource(R.string.pdf_tools_subtitle), onBack = onBack) },
        bottomBar = {
            NativeAdSlot(
                placement = AdFormat.NATIVE_PDF_TOOLS,
                template = NativeAdTemplate.MEDIUM,
                screen = "PdfTools",
                // Loaded once; leaving and re-opening PDF tools reuses it.
                retain = true,
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            itemsIndexed(PdfTool.entries) { _, tool ->
                Surface(
                    onClick = { onTool(tool) },
                    shape = RoundedCornerShape(24.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(54.dp).clip(RoundedCornerShape(18.dp)).background(tool.color), contentAlignment = Alignment.Center) {
                            Icon(tool.icon, null, tint = Color.White)
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(tool.title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(tool.subtitle), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun PdfToolScreen(
    toolName: String,
    onBack: () -> Unit,
    onOpenSaved: () -> Unit,
) {
    val tool = remember(toolName) { runCatching { PdfTool.valueOf(toolName) }.getOrDefault(PdfTool.MERGE) }
    val viewModel: PdfToolViewModel = koinViewModel(key = "tool-$toolName") { parametersOf(tool) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it.resolve(context)) } }

    Scaffold(
        topBar = { AppTopBar(title = stringResource(tool.title), onBack = onBack) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            val result = state.result
            when {
                result != null -> ResultContent(result, onAgain = viewModel::startOver, onOpenSaved = onOpenSaved)
                tool == PdfTool.MERGE -> MergeContent(state, viewModel)
                state.pdf == null -> PickPdfContent(tool, onPick = viewModel::loadPdf)
                tool == PdfTool.COMPRESS -> CompressContent(state, viewModel)
                tool == PdfTool.SPLIT -> PageSelectContent(state, viewModel, actionLabel = { stringResource(R.string.tool_extract_count, it) }, onAction = viewModel::extract)
                tool == PdfTool.TO_IMAGES -> PageSelectContent(state, viewModel, actionLabel = { stringResource(R.string.tool_export_count, it) }, onAction = viewModel::exportImages) {
                    Text(stringResource(R.string.tool_format), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    ChoiceChips(ImageFormat.entries, state.imageFormat, { it.label }, viewModel::setImageFormat)
                    Spacer(Modifier.height(10.dp))
                    Text(stringResource(R.string.tool_quality), style = MaterialTheme.typography.titleSmall)
                    Spacer(Modifier.height(6.dp))
                    ChoiceChips(ImageQuality.entries, state.imageQuality, { it.displayName() }, viewModel::setImageQuality)
                }
                tool == PdfTool.SIGN -> SignContent(state, viewModel)
            }
        }
    }
    if (state.working) {
        LoadingDialog(title = stringResource(R.string.tool_working_title), message = stringResource(R.string.tool_working_message))
    }
}

@Composable
private fun FileHeader(name: String, size: Long, pages: Int, onClear: () -> Unit) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE11D48).copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.PictureAsPdf, null, tint = Color(0xFFE11D48))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.file_size_pages, formatBytes(size), pluralStringResource(R.plurals.pages_count, pages, pages)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onClear) { Icon(Icons.Rounded.Close, stringResource(R.string.tool_remove_file)) }
        }
    }
}

@Composable
private fun PickPdfContent(tool: PdfTool, onPick: (android.net.Uri) -> Unit) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(onPick) }
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(110.dp).clip(CircleShape).background(tool.color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            Icon(tool.icon, null, tint = tool.color, modifier = Modifier.size(52.dp))
        }
        Spacer(Modifier.height(20.dp))
        Text(stringResource(tool.title), style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(6.dp))
        Text(stringResource(tool.subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))
        GradientButton(stringResource(R.string.tool_choose_pdf), onClick = { picker.launch(arrayOf("application/pdf")) }, icon = Icons.Rounded.UploadFile)
    }
}

@Composable
private fun MergeContent(state: PdfToolState, viewModel: PdfToolViewModel) {
    var chooser by remember { mutableStateOf(false) }
    val photos = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia()) { uris -> if (uris.isNotEmpty()) viewModel.addMergeFiles(uris) }
    val files = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris -> if (uris.isNotEmpty()) viewModel.addMergeFiles(uris) }

    if (state.mergeItems.isEmpty()) {
        Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(110.dp).clip(CircleShape).background(PdfTool.MERGE.color.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(PdfTool.MERGE.icon, null, tint = PdfTool.MERGE.color, modifier = Modifier.size(52.dp))
            }
            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.merge_intro), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(24.dp))
            GradientButton(stringResource(R.string.merge_add_files), onClick = { chooser = true }, icon = Icons.Rounded.Add)
        }
    } else {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Column {
                        Text(stringResource(R.string.merge_image_page_size), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        ChoiceChips(ToolPageSize.entries, state.pageSize, { it.displayName() }, viewModel::setPageSize)
                        Spacer(Modifier.height(12.dp))
                        Text(stringResource(R.string.merge_orientation), style = MaterialTheme.typography.titleSmall)
                        Spacer(Modifier.height(6.dp))
                        ChoiceChips(ToolOrientation.entries, state.orientation, { it.displayName() }, viewModel::setOrientation)
                        Spacer(Modifier.height(12.dp))
                        LabeledSlider(stringResource(R.string.merge_image_margin), state.marginMm, viewModel::setMargin, 0f..20f, steps = 19, valueLabel = { "${it.roundToInt()} mm" })
                    }
                }
                itemsIndexed(state.mergeItems, key = { index, item -> "$index-${item.uri}" }) { index, item ->
                    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            val tint = if (item.isPdf) Color(0xFFE11D48) else Color(0xFF0EA5E9)
                            Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                                Icon(if (item.isPdf) Icons.Rounded.PictureAsPdf else Icons.Rounded.Image, null, tint = tint)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(if (item.isPdf) stringResource(R.string.merge_item_pdf, formatBytes(item.sizeBytes)) else stringResource(R.string.merge_item_image, formatBytes(item.sizeBytes)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { viewModel.moveMergeItem(index, -1) }, enabled = index > 0) { Icon(Icons.Rounded.KeyboardArrowUp, stringResource(R.string.move_up)) }
                            IconButton(onClick = { viewModel.moveMergeItem(index, 1) }, enabled = index < state.mergeItems.lastIndex) { Icon(Icons.Rounded.KeyboardArrowDown, stringResource(R.string.move_down)) }
                            IconButton(onClick = { viewModel.removeMergeItem(index) }) { Icon(Icons.Rounded.Close, stringResource(R.string.common_remove)) }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { chooser = true }, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.merge_add_more))
                }
                GradientButton(stringResource(R.string.tool_merge_count, state.mergeItems.size), onClick = viewModel::merge, modifier = Modifier.weight(1f))
            }
        }
    }

    if (chooser) {
        AlertDialog(
            onDismissRequest = { chooser = false },
            title = { Text(stringResource(R.string.merge_add_files)) },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.merge_photos)) },
                        supportingContent = { Text(stringResource(R.string.merge_photos_desc)) },
                        leadingContent = { Icon(Icons.Rounded.Image, null) },
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable {
                            chooser = false
                            photos.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    )
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.merge_files)) },
                        supportingContent = { Text(stringResource(R.string.merge_files_desc)) },
                        leadingContent = { Icon(Icons.Rounded.PictureAsPdf, null) },
                        modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable {
                            chooser = false
                            files.launch(arrayOf("application/pdf", "image/*"))
                        },
                    )
                }
            },
            confirmButton = { TextButton(onClick = { chooser = false }) { Text(stringResource(R.string.common_cancel)) } },
        )
    }
}

@Composable
private fun CompressContent(state: PdfToolState, viewModel: PdfToolViewModel) {
    val pdf = state.pdf ?: return
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FileHeader(pdf.name, pdf.sizeBytes, pdf.pageCount, viewModel::clearPdf)
            Text(stringResource(R.string.compression_level), style = MaterialTheme.typography.titleMedium)
            CompressionLevel.entries.forEach { level ->
                val selected = level == state.compression
                Surface(
                    onClick = { viewModel.setCompression(level) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
                    border = BorderStroke(if (selected) 2.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(level.displayName(), style = MaterialTheme.typography.titleSmall)
                            Text(level.displayDescription(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (selected) Icon(Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Text(
                stringResource(R.string.compression_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        GradientButton(stringResource(R.string.tool_compress), onClick = viewModel::compress, modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp))
    }
}

@Composable
private fun PageSelectContent(
    state: PdfToolState,
    viewModel: PdfToolViewModel,
    actionLabel: @Composable (Int) -> String,
    onAction: () -> Unit,
    options: (@Composable () -> Unit)? = null,
) {
    val pdf = state.pdf ?: return
    Column(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(104.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    FileHeader(pdf.name, pdf.sizeBytes, pdf.pageCount, viewModel::clearPdf)
                    options?.let { Column { it() } }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(stringResource(R.string.pages_tap_to_select), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                        val all = state.selectedPages.size == pdf.pageCount
                        TextButton(onClick = { viewModel.selectAll(!all) }) { Text(if (all) stringResource(R.string.common_clear) else stringResource(R.string.pages_select_all)) }
                    }
                }
            }
            items(pdf.pageCount) { index ->
                PageThumb(
                    bitmap = state.thumbnails[index],
                    label = stringResource(R.string.page_number, index + 1),
                    selected = index in state.selectedPages,
                    badge = if (index in state.selectedPages) "${state.selectedPages.sorted().indexOf(index) + 1}" else null,
                    onClick = { viewModel.togglePage(index) },
                )
            }
        }
        GradientButton(
            actionLabel(state.selectedPages.size),
            onClick = onAction,
            enabled = state.selectedPages.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
        )
    }
}

@Composable
private fun PageThumb(bitmap: android.graphics.Bitmap?, label: String, selected: Boolean, badge: String?, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier.clip(shape)
            .border(if (selected) 3.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, shape)
            .clickable(onClick = onClick),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(0.72f).background(Color.White), contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(bitmap.asImageBitmap(), label, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            } else {
                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            }
            badge?.let {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center,
                ) { Text(it, color = Color.White, style = MaterialTheme.typography.labelMedium) }
            }
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth().background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant).padding(vertical = 6.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun SignContent(state: PdfToolState, viewModel: PdfToolViewModel) {
    val pdf = state.pdf ?: return
    val padState = rememberSignatureState()
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FileHeader(pdf.name, pdf.sizeBytes, pdf.pageCount, viewModel::clearPdf)
            if (state.signature == null) {
                Text(stringResource(R.string.sign_step_draw), style = MaterialTheme.typography.titleMedium)
                SignaturePad(padState)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = padState::clear, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.common_clear)) }
                    Button(onClick = { viewModel.setSignature(padState.toBitmap()) }, enabled = !padState.isEmpty, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.common_next)) }
                }
            } else {
                Text(stringResource(R.string.sign_step_page), style = MaterialTheme.typography.titleMedium)
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    repeat(pdf.pageCount) { index ->
                        Box(Modifier.width(84.dp)) {
                            PageThumb(state.thumbnails[index], stringResource(R.string.page_number, index + 1), index == state.signPage, null) { viewModel.setSignPage(index) }
                        }
                    }
                }
                Text(stringResource(R.string.sign_step_place), style = MaterialTheme.typography.titleMedium)
                SignPlacement(state, viewModel)
                LabeledSlider(stringResource(R.string.sign_size), state.signWidth * 100, { viewModel.setSignatureWidth(it / 100f) }, 10f..70f, valueLabel = { "${it.roundToInt()}%" })
                TextButton(onClick = { viewModel.setSignature(null) }) { Text(stringResource(R.string.sign_redraw)) }
            }
        }
        GradientButton(
            stringResource(R.string.sign_apply),
            onClick = viewModel::sign,
            enabled = state.signature != null,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
        )
    }
}

@Composable
private fun SignPlacement(state: PdfToolState, viewModel: PdfToolViewModel) {
    val signature = state.signature ?: return
    val page by produceState(state.thumbnails[state.signPage], state.signPage) { value = viewModel.renderLargePage(state.signPage) ?: value }
    val bitmap = page ?: return
    val density = LocalDensity.current
    BoxWithConstraints(
        Modifier.fillMaxWidth().aspectRatio(bitmap.width.toFloat() / bitmap.height).clip(RoundedCornerShape(6.dp))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)),
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val sigW = widthPx * state.signWidth
        val sigH = sigW * signature.height / signature.width
        Image(bitmap.asImageBitmap(), stringResource(R.string.page_preview), contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
        Image(
            signature.asImageBitmap(),
            stringResource(R.string.signature_desc),
            modifier = Modifier
                .offset { IntOffset((state.signX * widthPx - sigW / 2).roundToInt(), (state.signY * heightPx - sigH / 2).roundToInt()) }
                .size(with(density) { sigW.toDp() }, with(density) { sigH.toDp() })
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.7f), RoundedCornerShape(4.dp))
                .pointerInput(widthPx, heightPx) {
                    detectDragGestures { change, drag ->
                        change.consume()
                        val current = viewModel.state.value
                        viewModel.moveSignature(current.signX + drag.x / widthPx, current.signY + drag.y / heightPx)
                    }
                },
        )
    }
}

@Composable
private fun ResultContent(result: ToolResult, onAgain: () -> Unit, onOpenSaved: () -> Unit) {
    val context = LocalContext.current
    val openFile = rememberFileOpener()
    val first = result.files.firstOrNull()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(24.dp))
        Box(Modifier.size(96.dp).clip(CircleShape).background(Success.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.CheckCircle, null, tint = Success, modifier = Modifier.size(60.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text(result.headline.resolve(context), style = MaterialTheme.typography.headlineMedium)
        result.detail?.let {
            Spacer(Modifier.height(6.dp))
            Text(it.resolve(context), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(24.dp))
        result.files.take(4).forEach { file ->
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(file.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(formatBytes(file.sizeBytes), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { FileActions.share(context, file.path, file.mimeType) }) { Icon(Icons.Rounded.Share, stringResource(R.string.common_share)) }
                }
            }
        }
        if (result.files.size > 4) Text(stringResource(R.string.result_more_in_saved, result.files.size - 4), style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(16.dp))
        if (first != null) {
            GradientButton(stringResource(R.string.common_open), onClick = { openFile(first.path, first.mimeType, first.name) }, icon = Icons.AutoMirrored.Rounded.OpenInNew, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
        }
        OutlinedButton(onClick = onOpenSaved, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(stringResource(R.string.saved_view_in_files)) }
        TextButton(onClick = onAgain) { Text(stringResource(R.string.tool_start_over)) }
    }
}

@Composable
private fun ToolPageSize.displayName(): String = when (this) {
    ToolPageSize.A4 -> label
    ToolPageSize.LETTER -> stringResource(R.string.page_size_letter)
    ToolPageSize.FIT -> stringResource(R.string.tool_page_fit)
}

@Composable
private fun ToolOrientation.displayName(): String = stringResource(
    when (this) {
        ToolOrientation.AUTO -> R.string.orientation_auto
        ToolOrientation.PORTRAIT -> R.string.orientation_portrait
        ToolOrientation.LANDSCAPE -> R.string.orientation_landscape
    },
)

@Composable
private fun CompressionLevel.displayName(): String = stringResource(
    when (this) {
        CompressionLevel.LOW -> R.string.compression_light
        CompressionLevel.MEDIUM -> R.string.compression_balanced
        CompressionLevel.HIGH -> R.string.compression_strong
    },
)

@Composable
private fun CompressionLevel.displayDescription(): String = stringResource(
    when (this) {
        CompressionLevel.LOW -> R.string.compression_light_desc
        CompressionLevel.MEDIUM -> R.string.compression_balanced_desc
        CompressionLevel.HIGH -> R.string.compression_strong_desc
    },
)

@Composable
private fun ImageQuality.displayName(): String = stringResource(
    when (this) {
        ImageQuality.LOW -> R.string.quality_low
        ImageQuality.MEDIUM -> R.string.quality_medium
        ImageQuality.HIGH -> R.string.quality_high
    },
)
