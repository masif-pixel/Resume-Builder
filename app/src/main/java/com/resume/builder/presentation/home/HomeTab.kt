package com.resume.builder.presentation.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NoteAdd
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resume.builder.R
import com.resume.builder.domain.model.Resume
import com.resume.builder.domain.model.TemplateCatalog
import com.resume.builder.presentation.components.AiOrb
import com.resume.builder.presentation.components.AppTopBar
import com.resume.builder.presentation.components.ConfirmDialog
import com.resume.builder.presentation.components.EmptyState
import com.resume.builder.presentation.components.FileActions
import com.resume.builder.presentation.components.LoadingDialog
import com.resume.builder.presentation.components.ResumeStackIllustration
import com.resume.builder.presentation.components.CreateResumeDialog
import com.resume.builder.presentation.components.TextInputDialog
import com.resume.builder.presentation.components.formatModified
import com.resume.builder.presentation.components.rememberFileOpener
import com.resume.builder.presentation.main.MainActions
import com.resume.builder.ui.theme.Gradients
import com.resume.builder.ui.theme.Success
import com.resume.builder.ui.theme.Teal
import com.resume.builder.ui.theme.Violet
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeTab(
    actions: MainActions,
    onOpenDrawer: () -> Unit,
    snackbar: SnackbarHostState,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val resumes by viewModel.resumeList.collectAsStateWithLifecycle()
    val thumbnails by viewModel.thumbnails.collectAsStateWithLifecycle()
    val importing by viewModel.importing.collectAsStateWithLifecycle()
    val busyId by viewModel.busyResumeId.collectAsStateWithLifecycle()

    val openFile = rememberFileOpener()
    var showCreate by remember { mutableStateOf(false) }
    var showImport by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Resume?>(null) }
    var deleting by remember { mutableStateOf<Resume?>(null) }

    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        showImport = false
        uri?.let(viewModel::import)
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.OpenEditor -> actions.openEditor(event.resumeId)
                is HomeEvent.Message -> snackbar.showSnackbar(event.text)
                is HomeEvent.Deleted -> {
                    val result = snackbar.showSnackbar("“${event.resume.title}” deleted", actionLabel = "Undo", duration = SnackbarDuration.Long)
                    if (result == SnackbarResult.ActionPerformed) viewModel.undoDelete(event.resume)
                }
                is HomeEvent.Exported -> {
                    val result = snackbar.showSnackbar("PDF saved · ${event.file.name}", actionLabel = "Open", duration = SnackbarDuration.Long)
                    if (result == SnackbarResult.ActionPerformed) openFile(event.file.path, event.file.mimeType, event.file.name)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.app_name),
                navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Menu, "Menu") } },
                actions = {
                    IconButton(onClick = actions.openPdfTools) { Icon(Icons.Rounded.PictureAsPdf, "PDF tools") }
                },
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                    Text(
                        "Welcome 👋",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("Let's build your next big move", style = MaterialTheme.typography.headlineMedium)
                }
            }
            item { HeroCard(onClick = { showCreate = true }) }
            item {
                // Intrinsic height keeps both cards equally tall when one subtitle wraps.
                Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QuickAction(
                        title = "Import resume", subtitle = "From an existing PDF", icon = Icons.Rounded.UploadFile, tint = Teal,
                        modifier = Modifier.weight(1f).fillMaxHeight(), onClick = { showImport = true },
                    )
                    QuickAction(
                        title = "AI resume", subtitle = "Write it for me", icon = Icons.Rounded.AutoAwesome, tint = Violet,
                        modifier = Modifier.weight(1f).fillMaxHeight(), onClick = actions.openAiResume,
                    )
                }
            }
            item {
                QuickAction(
                    title = "Browse templates", subtitle = "${TemplateCatalog.all.size} layouts · pick a look first", icon = Icons.Rounded.Dashboard,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth(), onClick = { actions.openTemplatePicker(null) },
                    horizontal = true,
                )
            }
            val list = resumes
            when {
                list == null -> item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                }
                list.isEmpty() -> item {
                    EmptyState(
                        title = "No resumes yet",
                        message = "Start from scratch, import a PDF or let AI draft one for you. Everything saves automatically.",
                        illustration = { ResumeStackIllustration() },
                    )
                }
                else -> {
                    item {
                        Text(
                            "My resumes · ${list.size}",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                        )
                    }
                    items(list, key = { it.id }) { resume ->
                        ResumeCard(
                            resume = resume,
                            thumbnail = thumbnails[resume.id]?.second,
                            busy = busyId == resume.id,
                            onPreview = { actions.openPreview(resume.id) },
                            onEdit = { actions.openEditor(resume.id) },
                            onRename = { renaming = resume },
                            onDuplicate = { viewModel.duplicate(resume) },
                            onTemplate = { actions.openTemplatePicker(resume.id) },
                            onDownload = { viewModel.export(resume) },
                            onDelete = { deleting = resume },
                            modifier = Modifier.animateItem(),
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateResumeDialog(
            label = "e.g. Android Developer",
            supporting = "Use the role you're targeting — you can rename it anytime.",
            onConfirm = { title, language -> showCreate = false; viewModel.create(title, language) },
            onDismiss = { showCreate = false },
        )
    }
    if (showImport) {
        ImportDialog(onPick = { pdfPicker.launch(arrayOf("application/pdf")) }, onDismiss = { showImport = false })
    }
    if (importing) {
        LoadingDialog(
            title = "Reading your resume",
            message = "Extracting your details and sections…",
            onCancel = viewModel::cancelImport,
            visual = { AiOrb(size = 120.dp) },
        )
    }
    renaming?.let { resume ->
        TextInputDialog(
            title = "Rename resume",
            label = "Resume name",
            initial = resume.title,
            onConfirm = { renaming = null; viewModel.rename(resume, it) },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { resume ->
        ConfirmDialog(
            title = "Delete resume?",
            message = "“${resume.title}” will be removed from this device.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = { deleting = null; viewModel.delete(resume) },
            onDismiss = { deleting = null },
        )
    }
}

/** Card titles and descriptions on Home sit 3sp below the theme scale so they fit narrow / large-font devices. */
private fun TextStyle.cardSize(): TextStyle = copy(
    fontSize = (fontSize.value - 3).sp,
    lineHeight = if (lineHeight.isSp) (lineHeight.value - 3).sp else lineHeight,
)

@Composable
private fun HeroCard(onClick: () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Gradients.hero).clickable(onClick = onClick),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(Color.White.copy(alpha = 0.08f), radius = size.height * 0.9f, center = Offset(size.width * 0.95f, size.height * 0.05f))
            drawCircle(Color.White.copy(alpha = 0.07f), radius = size.height * 0.55f, center = Offset(size.width * 0.78f, size.height * 1.05f))
            drawCircle(Color.White.copy(alpha = 0.12f), radius = size.height * 0.28f, center = Offset(size.width * 0.9f, size.height * 0.55f), style = Stroke(2f))
        }
        Row(Modifier.padding(horizontal = 18.dp, vertical = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.NoteAdd, null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Create resume", style = MaterialTheme.typography.headlineSmall.cardSize(), color = Color.White, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text("Start from scratch with guided sections", style = MaterialTheme.typography.bodyMedium.cardSize(), color = Color.White.copy(alpha = 0.85f))
            }
            Spacer(Modifier.width(8.dp))
            Box(Modifier.size(40.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Violet)
            }
        }
    }
}

@Composable
private fun QuickAction(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier,
    onClick: () -> Unit,
    horizontal: Boolean = false,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = tint.copy(alpha = 0.09f),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.18f)),
    ) {
        val iconBox: @Composable () -> Unit = {
            Box(Modifier.size(if (horizontal) 44.dp else 34.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = tint)
            }
        }
        if (horizontal) {
            Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                iconBox()
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium.cardSize())
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall.cardSize(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = tint)
            }
        } else {
            Row(
                Modifier.fillMaxHeight().padding(horizontal = 10.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                iconBox()
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    // Always one line; only shrinks below the card size on very narrow or large-font screens.
                    val titleStyle = MaterialTheme.typography.titleMedium.cardSize()
                    Text(
                        title,
                        style = titleStyle,
                        maxLines = 1,
                        softWrap = false,
                        autoSize = TextAutoSize.StepBased(minFontSize = 10.sp, maxFontSize = titleStyle.fontSize, stepSize = 0.5.sp),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(subtitle, style = MaterialTheme.typography.bodySmall.cardSize(), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun ResumeCard(
    resume: Resume,
    thumbnail: android.graphics.Bitmap?,
    busy: Boolean,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onTemplate: () -> Unit,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    val strength = remember(resume.content) { resume.content.strength() }
    Surface(
        onClick = onEdit,
        modifier = modifier.fillMaxWidth().animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(12.dp)) {
            Box(
                Modifier.width(92.dp).aspectRatio(0.707f).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(10.dp))
                    .clickable(onClick = onPreview),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbnail != null) {
                    Image(thumbnail.asImageBitmap(), contentDescription = "Resume preview", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Column(Modifier.weight(1f)) {
                        Text(resume.title, style = MaterialTheme.typography.titleMedium.cardSize(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "Edited ${formatModified(resume.updatedAt)}",
                            style = MaterialTheme.typography.bodySmall.cardSize(),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box {
                        IconButton(onClick = { menu = true }, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.MoreVert, "More") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            MenuItem("Rename", Icons.Rounded.DriveFileRenameOutline) { menu = false; onRename() }
                            MenuItem("Duplicate", Icons.Rounded.ContentCopy) { menu = false; onDuplicate() }
                            MenuItem("Change template", Icons.Rounded.Dashboard) { menu = false; onTemplate() }
                            MenuItem("Download PDF", Icons.Rounded.Download) { menu = false; onDownload() }
                            MenuItem("Delete", Icons.Rounded.Delete, destructive = true) { menu = false; onDelete() }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Text(
                        TemplateCatalog.byId(resume.templateId).name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress = { strength / 100f },
                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(50)),
                        color = when {
                            strength >= 75 -> Success
                            strength >= 45 -> MaterialTheme.colorScheme.primary
                            else -> Color(0xFFF59E0B)
                        },
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        drawStopIndicator = {},
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("$strength%", style = MaterialTheme.typography.labelMedium)
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onPreview, contentPadding = PaddingValues(horizontal = 14.dp), modifier = Modifier.height(38.dp)) {
                        if (busy) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(Icons.Rounded.Visibility, null, Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text("Preview")
                    }
                    OutlinedButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 14.dp), modifier = Modifier.height(38.dp)) {
                        Icon(Icons.Rounded.Edit, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit")
                    }
                }
            }
        }
    }
}

@Composable
private fun MenuItem(text: String, icon: ImageVector, destructive: Boolean = false, onClick: () -> Unit) {
    val color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
    DropdownMenuItem(text = { Text(text, color = color) }, leadingIcon = { Icon(icon, null, tint = color) }, onClick = onClick)
}

@Composable
private fun ImportDialog(onPick: () -> Unit, onDismiss: () -> Unit) {
    val outline = MaterialTheme.colorScheme.primary
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import existing resume") },
        text = {
            Column {
                Box(
                    Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                        .clickable(onClick = onPick),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.matchParentSize()) {
                        drawRoundRect(
                            color = outline.copy(alpha = 0.6f),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(20.dp.toPx()),
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.UploadFile, null, tint = outline, modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Tap to choose a PDF", style = MaterialTheme.typography.titleSmall)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "PDF up to 5 MB. Text is extracted on-device — review sections after import.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
