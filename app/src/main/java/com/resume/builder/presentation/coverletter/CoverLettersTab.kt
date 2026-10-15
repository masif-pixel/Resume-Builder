package com.resume.builder.presentation.coverletter

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.resume.builder.domain.model.CoverLetter
import com.resume.builder.domain.model.Resume
import com.resume.builder.domain.model.newId
import com.resume.builder.domain.repository.AiRepository
import com.resume.builder.domain.repository.CoverLetterRepository
import com.resume.builder.domain.repository.ResumeRepository
import com.resume.builder.domain.usecase.DeleteCoverLetterUseCase
import com.resume.builder.presentation.components.AppTopBar
import com.resume.builder.presentation.components.ConfirmDialog
import com.resume.builder.presentation.components.EmptyState
import com.resume.builder.presentation.components.LetterIllustration
import com.resume.builder.presentation.components.formatModified
import com.resume.builder.presentation.main.MainActions
import com.resume.builder.ui.theme.Gradients
import com.resume.builder.ui.theme.Success
import com.resume.builder.ui.theme.Violet
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class CoverLettersViewModel(
    private val letters: CoverLetterRepository,
    resumes: ResumeRepository,
    private val deleteLetter: DeleteCoverLetterUseCase,
) : ViewModel() {
    val letterList: StateFlow<List<CoverLetter>?> = letters.observeLetters().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val resumeList: StateFlow<List<Resume>> = resumes.observeResumes().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun duplicate(letter: CoverLetter) = viewModelScope.launch {
        val now = System.currentTimeMillis()
        letters.upsert(letter.copy(id = newId(), title = "${letter.title} (copy)", createdAt = now))
    }

    fun delete(letter: CoverLetter) = viewModelScope.launch { deleteLetter(letter.id) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoverLettersTab(
    actions: MainActions,
    onOpenDrawer: () -> Unit,
    snackbar: SnackbarHostState,
    viewModel: CoverLettersViewModel = koinViewModel(),
) {
    val letters by viewModel.letterList.collectAsStateWithLifecycle()
    val resumes by viewModel.resumeList.collectAsStateWithLifecycle()
    var showChooser by remember { mutableStateOf(false) }
    var pickResumeFor by remember { mutableStateOf<String?>(null) }
    var deleting by remember { mutableStateOf<CoverLetter?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Cover letters",
                navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Menu, "Menu") } },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showChooser = true },
                icon = { Icon(Icons.Rounded.Add, null) },
                text = { Text("New letter") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        val list = letters
        if (list != null && list.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                EmptyState(
                    title = "Stand out with a cover letter",
                    message = "Generate one tailored to a job in seconds, using the details already in your resume.",
                    illustration = { LetterIllustration() },
                    action = {
                        Button(onClick = { showChooser = true }) {
                            Icon(Icons.Rounded.AutoAwesome, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Create cover letter")
                        }
                    },
                )
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(list.orEmpty(), key = { it.id }) { letter ->
                    LetterCard(
                        letter = letter,
                        onView = { actions.openCoverLetterPreview(letter.id) },
                        onEdit = { actions.openCoverLetterEditor(letter.id, null) },
                        onDuplicate = { viewModel.duplicate(letter) },
                        onDelete = { deleting = letter },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (showChooser) {
        ModalBottomSheet(onDismissRequest = { showChooser = false }) {
            Column(Modifier.padding(horizontal = 20.dp).navigationBarsPadding().padding(bottom = 16.dp)) {
                Text("New cover letter", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(16.dp))
                ChooserOption(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Generate with AI",
                    subtitle = "Tailored to a job description using your resume",
                    highlight = true,
                    onClick = {
                        showChooser = false
                        when {
                            resumes.isEmpty() -> scope.launch { snackbar.showSnackbar("Create a resume first — the AI uses it as your source of truth") }
                            resumes.size == 1 -> actions.openCoverLetterGenerate(resumes.first().id)
                            else -> pickResumeFor = "ai"
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                ChooserOption(
                    icon = Icons.Rounded.EditNote,
                    title = "Write it myself",
                    subtitle = "Start from a clean, professional letter layout",
                    highlight = false,
                    onClick = {
                        showChooser = false
                        if (resumes.size > 1) pickResumeFor = "manual" else actions.openCoverLetterEditor(null, resumes.firstOrNull()?.id)
                    },
                )
            }
        }
    }

    pickResumeFor?.let { mode ->
        AlertDialog(
            onDismissRequest = { pickResumeFor = null },
            title = { Text("Select resume") },
            text = {
                Column {
                    Text(
                        if (mode == "ai") "The letter will be tailored using this resume." else "Its contact details will appear in the letter header.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    resumes.forEach { resume ->
                        ListItem(
                            headlineContent = { Text(resume.title) },
                            supportingContent = { Text("Edited ${formatModified(resume.updatedAt)}") },
                            leadingContent = {
                                Box(Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.primary)
                                }
                            },
                            modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable {
                                pickResumeFor = null
                                if (mode == "ai") actions.openCoverLetterGenerate(resume.id) else actions.openCoverLetterEditor(null, resume.id)
                            },
                        )
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pickResumeFor = null }) { Text("Cancel") } },
        )
    }

    deleting?.let { letter ->
        ConfirmDialog(
            title = "Delete cover letter?",
            message = "“${letter.title}” will be permanently removed.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = { deleting = null; viewModel.delete(letter) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun ChooserOption(icon: ImageVector, title: String, subtitle: String, highlight: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (highlight) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant,
        border = if (highlight) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.then(if (highlight) Modifier.background(Gradients.hero) else Modifier).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val content = if (highlight) Color.White else MaterialTheme.colorScheme.onSurface
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(content.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (highlight) Color.White else Violet)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = content)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.75f))
            }
            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = content)
        }
    }
}

@Composable
private fun LetterCard(
    letter: CoverLetter,
    onView: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    Surface(
        onClick = onEdit,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(Gradients.brand), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Mail, null, tint = Color.White)
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(letter.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Edited ${formatModified(letter.updatedAt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        DropdownMenuItem(text = { Text("Duplicate") }, leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) }, onClick = { menu = false; onDuplicate() })
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = { menu = false; onDelete() },
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                letter.body.lineSequence().map { it.trim() }.filter { it.isNotEmpty() }.drop(1).joinToString(" "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onView, contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.height(38.dp)) {
                    Icon(Icons.Rounded.Visibility, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("View")
                }
                OutlinedButton(onClick = onEdit, contentPadding = PaddingValues(horizontal = 16.dp), modifier = Modifier.height(38.dp)) {
                    Icon(Icons.Rounded.Edit, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }
            }
        }
    }
}
