package com.resume.builder.presentation.templates

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.pixelforge.ads.nativead.NativeAdSlot
import com.pixelforge.ads.nativead.NativeAdTemplate
import com.pixelforge.ads.remoteconfig.AdFormat
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.resume.builder.domain.model.ResumeLanguage
import com.resume.builder.domain.model.TemplateCatalog
import com.resume.builder.domain.model.TemplateCategory
import com.resume.builder.domain.model.TemplateSpec
import com.resume.builder.domain.repository.ResumeRepository
import com.resume.builder.domain.usecase.CreateResumeUseCase
import com.resume.builder.domain.usecase.DocumentExporter
import com.resume.builder.presentation.components.AppTopBar
import com.resume.builder.presentation.components.ChoiceChips
import com.resume.builder.presentation.components.GradientButton
import com.resume.builder.presentation.components.CreateResumeDialog
import com.resume.builder.presentation.main.MainActions
import com.resume.builder.ui.theme.Success
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

sealed interface TemplateEvent {
    data class Created(val resumeId: String) : TemplateEvent
    data class Applied(val name: String) : TemplateEvent
}

class TemplatesViewModel(
    private val exporter: DocumentExporter,
    private val resumes: ResumeRepository,
    private val createResume: CreateResumeUseCase,
) : ViewModel() {

    private val _thumbnails = MutableStateFlow<Map<String, Bitmap>>(emptyMap())
    val thumbnails: StateFlow<Map<String, Bitmap>> = _thumbnails.asStateFlow()

    private val _events = Channel<TemplateEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            TemplateCatalog.all.forEach { spec ->
                runCatching { exporter.templateThumbnail(spec.id, THUMB_WIDTH) }
                    .onSuccess { bitmap -> _thumbnails.update { it + (spec.id to bitmap) } }
            }
        }
    }

    suspend fun largePreview(templateId: String): Bitmap = exporter.templateThumbnail(templateId, 1080)

    fun currentTemplate(resumeId: String): Flow<String?> = resumes.observeResume(resumeId).map { it?.templateId }

    fun apply(resumeId: String, spec: TemplateSpec) = viewModelScope.launch {
        val resume = resumes.getResume(resumeId) ?: return@launch
        resumes.upsert(resume.copy(templateId = spec.id))
        _events.send(TemplateEvent.Applied(spec.name))
    }

    fun create(spec: TemplateSpec, title: String, language: ResumeLanguage) = viewModelScope.launch {
        val resume = createResume(title, templateId = spec.id, language = language)
        _events.send(TemplateEvent.Created(resume.id))
    }

    private companion object {
        const val THUMB_WIDTH = 420
    }
}

/** Templates tab inside the main screen: choosing a template starts a new resume. */
@Composable
fun TemplatesTab(actions: MainActions, onOpenDrawer: () -> Unit, viewModel: TemplatesViewModel = koinViewModel()) {
    var creating by remember { mutableStateOf<TemplateSpec?>(null) }
    LaunchedEffect(Unit) {
        viewModel.events.collect { event -> if (event is TemplateEvent.Created) actions.openEditor(event.resumeId) }
    }
    Scaffold(
        topBar = {
            AppTopBar(
                title = "Templates",
                subtitle = "Recruiter-approved designs",
                navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Menu, "Menu") } },
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        TemplateGallery(
            viewModel = viewModel,
            selectedId = null,
            actionLabel = "Use this template",
            onChoose = { creating = it },
            modifier = Modifier.padding(padding),
        )
    }
    creating?.let { spec ->
        CreateResumeDialog(
            label = "e.g. Product Designer",
            supporting = "Starting with the ${spec.name} template.",
            onConfirm = { title, language -> creating = null; viewModel.create(spec, title, language) },
            onDismiss = { creating = null },
        )
    }
}

/** Stand-alone picker: switches the template of [resumeId], or creates a new resume when it is null. */
@Composable
fun TemplatePickerScreen(
    resumeId: String?,
    onBack: () -> Unit,
    onCreated: (String) -> Unit,
    viewModel: TemplatesViewModel = koinViewModel(),
) {
    val snackbar = remember { SnackbarHostState() }
    var creating by remember { mutableStateOf<TemplateSpec?>(null) }
    val currentId by (if (resumeId != null) viewModel.currentTemplate(resumeId) else kotlinx.coroutines.flow.flowOf(null))
        .let { flow -> remember(resumeId) { flow } }
        .collectAsStateWithLifecycle(initialValue = null)

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is TemplateEvent.Created -> onCreated(event.resumeId)
                is TemplateEvent.Applied -> snackbar.showSnackbar("${event.name} applied")
            }
        }
    }
    Scaffold(
        topBar = { AppTopBar(title = if (resumeId == null) "Choose a template" else "Change template", onBack = onBack) },
        bottomBar = {
            NativeAdSlot(
                placement = AdFormat.NATIVE_TEMPLATES,
                template = NativeAdTemplate.MEDIUM,
                screen = "Templates",
                // Loaded once; leaving and re-opening the picker reuses it.
                retain = true,
                modifier = Modifier.navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        TemplateGallery(
            viewModel = viewModel,
            selectedId = currentId,
            actionLabel = if (resumeId == null) "Use this template" else "Apply to resume",
            onChoose = { spec -> if (resumeId == null) creating = spec else viewModel.apply(resumeId, spec) },
            modifier = Modifier.padding(padding),
        )
    }
    creating?.let { spec ->
        CreateResumeDialog(
            label = "e.g. Marketing Manager",
            supporting = "Starting with the ${spec.name} template.",
            onConfirm = { title, language -> creating = null; viewModel.create(spec, title, language) },
            onDismiss = { creating = null },
        )
    }
}

/**
 * Like [GridCells.Adaptive] with a 150dp minimum, but never fewer than two columns, so even narrow phones or a
 * large "Display size" setting keep the two-up layout; wider screens (tablets, foldables) still gain columns.
 */
private object TemplateGridCells : GridCells {
    override fun Density.calculateCrossAxisCellSizes(availableSize: Int, spacing: Int): List<Int> {
        val minCell = 150.dp.roundToPx()
        val count = ((availableSize + spacing) / (minCell + spacing)).coerceAtLeast(2)
        val usable = availableSize - spacing * (count - 1)
        val base = usable / count
        val extra = usable % count
        return List(count) { base + if (it < extra) 1 else 0 }
    }
}

@Composable
private fun TemplateGallery(
    viewModel: TemplatesViewModel,
    selectedId: String?,
    actionLabel: String,
    onChoose: (TemplateSpec) -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbnails by viewModel.thumbnails.collectAsStateWithLifecycle()
    var category by rememberSaveable { mutableStateOf<TemplateCategory?>(null) }
    var previewing by remember { mutableStateOf<TemplateSpec?>(null) }
    val filtered = TemplateCatalog.all.filter { category == null || it.category == category }

    LazyVerticalGrid(
        columns = TemplateGridCells,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            ChoiceChips(
                options = listOf<TemplateCategory?>(null) + TemplateCategory.entries,
                selected = category,
                label = { it?.label ?: "All" },
                onSelect = { category = it },
                modifier = Modifier.padding(bottom = 4.dp),
            )
        }
        items(filtered, key = { it.id }) { spec ->
            TemplateCard(
                spec = spec,
                thumbnail = thumbnails[spec.id],
                selected = spec.id == selectedId,
                onClick = { previewing = spec },
            )
        }
    }

    previewing?.let { spec ->
        TemplatePreviewDialog(
            spec = spec,
            viewModel = viewModel,
            placeholder = thumbnails[spec.id],
            actionLabel = if (spec.id == selectedId) "Current template" else actionLabel,
            actionEnabled = spec.id != selectedId,
            onDismiss = { previewing = null },
            onChoose = { previewing = null; onChoose(spec) },
        )
    }
}

@Composable
private fun TemplateCard(spec: TemplateSpec, thumbnail: Bitmap?, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(if (selected) 2.5.dp else 1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column {
            Box(
                Modifier.fillMaxWidth().aspectRatio(0.72f).background(Color(spec.accent.toInt()).copy(alpha = 0.08f)).padding(10.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (thumbnail != null) {
                    Image(
                        thumbnail.asImageBitmap(),
                        contentDescription = "${spec.name} template",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().shadow(6.dp, RoundedCornerShape(4.dp)).clip(RoundedCornerShape(4.dp)),
                    )
                } else {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                if (selected) {
                    Icon(
                        Icons.Rounded.CheckCircle, "Selected", tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.TopEnd).background(Color.White, CircleShape),
                    )
                }
                if (spec.atsFriendly) {
                    Surface(shape = RoundedCornerShape(50), color = Success, modifier = Modifier.align(Alignment.TopStart)) {
                        Row(Modifier.padding(horizontal = 7.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Verified, null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(Modifier.width(3.dp))
                            Text("ATS", color = Color.White, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(Color(spec.accent.toInt())))
                    Spacer(Modifier.width(6.dp))
                    Text(spec.name, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                }
                Text(spec.tagline, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun TemplatePreviewDialog(
    spec: TemplateSpec,
    viewModel: TemplatesViewModel,
    placeholder: Bitmap?,
    actionLabel: String,
    actionEnabled: Boolean,
    onDismiss: () -> Unit,
    onChoose: () -> Unit,
) {
    val large by produceState(placeholder, spec.id) { value = runCatching { viewModel.largePreview(spec.id) }.getOrDefault(placeholder) }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                Row(Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, "Close") }
                    Column(Modifier.weight(1f)) {
                        Text(spec.name, style = MaterialTheme.typography.titleLarge)
                        Text("${spec.category.label} · ${spec.tagline}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Box(
                    Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    large?.let {
                        Image(
                            it.asImageBitmap(),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(6.dp)).clip(RoundedCornerShape(6.dp)),
                        )
                    }
                }
                AnimatedVisibility(visible = true) {
                    GradientButton(
                        text = actionLabel,
                        onClick = onChoose,
                        enabled = actionEnabled,
                        modifier = Modifier.fillMaxWidth().padding(20.dp),
                    )
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
