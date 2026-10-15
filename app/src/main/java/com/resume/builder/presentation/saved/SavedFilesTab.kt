package com.resume.builder.presentation.saved

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.resume.builder.domain.model.FileCategory
import com.resume.builder.domain.model.SavedFile
import com.resume.builder.domain.repository.SavedFileRepository
import com.resume.builder.presentation.components.AppTopBar
import com.resume.builder.presentation.components.ChoiceChips
import com.resume.builder.presentation.components.ConfirmDialog
import com.resume.builder.presentation.components.EmptyState
import com.resume.builder.presentation.components.FileActions
import com.resume.builder.presentation.components.FolderIllustration
import com.resume.builder.presentation.components.TextInputDialog
import com.resume.builder.presentation.components.formatBytes
import com.resume.builder.presentation.components.formatModified
import com.resume.builder.presentation.components.rememberFileOpener
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

class SavedFilesViewModel(private val repository: SavedFileRepository) : ViewModel() {
    val files: StateFlow<List<SavedFile>?> = repository.observeFiles().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    val folderLabel: String = repository.publicFolderLabel()

    fun rename(file: SavedFile, name: String) = viewModelScope.launch { repository.rename(file, name) }
    fun delete(file: SavedFile) = viewModelScope.launch { repository.delete(file) }
}

@Composable
fun SavedFilesTab(
    onOpenDrawer: () -> Unit,
    snackbar: SnackbarHostState,
    viewModel: SavedFilesViewModel = koinViewModel(),
) {
    val context = LocalContext.current
    val openFile = rememberFileOpener()
    val scope = rememberCoroutineScope()
    val files by viewModel.files.collectAsStateWithLifecycle()
    var filter by rememberSaveable { mutableStateOf<FileCategory?>(null) }
    var renaming by remember { mutableStateOf<SavedFile?>(null) }
    var deleting by remember { mutableStateOf<SavedFile?>(null) }
    val visible = files.orEmpty().filter { filter == null || it.category == filter }

    Scaffold(
        topBar = {
            AppTopBar(
                title = "Saved files",
                navigationIcon = { IconButton(onClick = onOpenDrawer) { Icon(Icons.Rounded.Menu, "Menu") } },
            )
        },
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0),
    ) { padding ->
        LazyColumn(
            Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                ChoiceChips(
                    options = listOf<FileCategory?>(null) + FileCategory.entries,
                    selected = filter,
                    label = { it?.label ?: "All" },
                    onSelect = { filter = it },
                )
            }
            item {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Folder, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Also saved to", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(viewModel.folderLabel, style = MaterialTheme.typography.titleSmall)
                        }
                        Text("${visible.size} files", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (files != null && visible.isEmpty()) {
                item {
                    EmptyState(
                        title = if (filter == null) "Nothing saved yet" else "No ${filter!!.label.lowercase()} files",
                        message = "PDFs you download and files created with PDF tools will appear here.",
                        illustration = { FolderIllustration() },
                    )
                }
            }
            items(visible, key = { it.id }) { file ->
                SavedFileRow(
                    file = file,
                    onOpen = { openFile(file.path, file.mimeType, file.name) },
                    onShare = { FileActions.share(context, file.path, file.mimeType) },
                    onRename = { renaming = file },
                    onDelete = { deleting = file },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }

    renaming?.let { file ->
        TextInputDialog(
            title = "Rename file",
            label = "File name",
            initial = file.name.substringBeforeLast('.'),
            onConfirm = { renaming = null; viewModel.rename(file, it) },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { file ->
        ConfirmDialog(
            title = "Delete file?",
            message = "“${file.name}” will be removed from the app. Copies already in your Documents folder stay there.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = {
                deleting = null
                viewModel.delete(file)
                scope.launch { snackbar.showSnackbar("File deleted") }
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun SavedFileRow(
    file: SavedFile,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menu by remember { mutableStateOf(false) }
    val isImage = file.mimeType.startsWith("image")
    val tint = if (isImage) Color(0xFF0EA5E9) else Color(0xFFE11D48)
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(tint.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                Icon(if (isImage) Icons.Rounded.Image else Icons.Rounded.PictureAsPdf, null, tint = tint)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(file.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Text(
                    "${formatModified(file.createdAt)} · ${formatBytes(file.sizeBytes)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Surface(shape = RoundedCornerShape(50), color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(file.category.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp))
                }
            }
            IconButton(onClick = onShare) { Icon(Icons.Rounded.Share, "Share") }
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Rounded.MoreVert, "More") }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Open") }, leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, null) }, onClick = { menu = false; onOpen() })
                    DropdownMenuItem(text = { Text("Rename") }, leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, null) }, onClick = { menu = false; onRename() })
                    DropdownMenuItem(
                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = MaterialTheme.colorScheme.error) },
                        onClick = { menu = false; onDelete() },
                    )
                }
            }
        }
    }
}
