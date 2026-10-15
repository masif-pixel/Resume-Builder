package com.resume.builder.presentation.editor

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resume.builder.domain.model.CustomLayout
import com.resume.builder.domain.model.Resume
import com.resume.builder.domain.model.ResumeSection
import com.resume.builder.domain.model.SectionType
import com.resume.builder.presentation.components.AppTopBar
import com.resume.builder.presentation.components.ConfirmDialog
import com.resume.builder.presentation.components.GradientButton
import com.resume.builder.presentation.components.TextInputDialog
import com.resume.builder.ui.theme.Success
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@Composable
fun ResumeEditorScreen(
    resumeId: String,
    onBack: () -> Unit,
    onEditSection: (String) -> Unit,
    onPreview: () -> Unit,
    viewModel: ResumeEditorViewModel = koinViewModel(key = "editor-$resumeId") { parametersOf(resumeId) },
) {
    val resume by viewModel.resume.collectAsStateWithLifecycle()
    var showManage by remember { mutableStateOf(false) }
    var showCustom by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var deletingSection by remember { mutableStateOf<ResumeSection?>(null) }

    LaunchedEffect(Unit) { viewModel.openSection.collect(onEditSection) }

    Scaffold(
        topBar = {
            AppTopBar(
                title = resume?.title ?: "",
                subtitle = "Tap a section to edit · drag to reorder",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { renaming = true }) { Icon(Icons.Rounded.Edit, "Rename resume") }
                    IconButton(onClick = { showManage = true }) { Icon(Icons.Rounded.Tune, "Show or hide sections") }
                },
            )
        },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.background) {
                GradientButton(
                    text = "Preview & download",
                    icon = Icons.Rounded.Visibility,
                    onClick = onPreview,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        },
    ) { padding ->
        val current = resume
        if (current == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val sections = current.content.sections.filter { it.visible }
        val listState = rememberLazyListState()
        val haptics = LocalHapticFeedback.current
        val reorderState = rememberReorderableLazyListState(listState) { from, to ->
            val fromKey = from.key as? String ?: return@rememberReorderableLazyListState
            val toKey = to.key as? String ?: return@rememberReorderableLazyListState
            if (sections.any { it.id == fromKey } && sections.any { it.id == toKey }) {
                viewModel.move(fromKey, toKey)
                haptics.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "strength") { StrengthCard(current) }
            items(sections, key = { it.id }) { section ->
                ReorderableItem(reorderState, key = section.id) { dragging ->
                    val elevation by animateDpAsState(if (dragging) 10.dp else 0.dp, label = "drag-elevation")
                    SectionRow(
                        section = section,
                        resume = current,
                        dragging = dragging,
                        modifier = Modifier.shadow(elevation, RoundedCornerShape(22.dp)),
                        onClick = { onEditSection(section.id) },
                        onDelete = if (section.type == SectionType.CUSTOM) ({ deletingSection = section }) else null,
                        handle = {
                            IconButton(
                                onClick = {},
                                modifier = Modifier.draggableHandle(
                                    onDragStarted = { haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
                                    onDragStopped = { haptics.performHapticFeedback(HapticFeedbackType.GestureEnd) },
                                ),
                            ) { Icon(Icons.Rounded.DragHandle, "Reorder", tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        },
                    )
                }
            }
            item(key = "actions") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                    OutlinedButton(onClick = { showCustom = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.Rounded.Add, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add custom section")
                    }
                    OutlinedButton(onClick = { showManage = true }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(18.dp)) {
                        Icon(Icons.Rounded.Tune, null)
                        Spacer(Modifier.width(8.dp))
                        val hidden = current.content.sections.count { !it.visible }
                        Text(if (hidden > 0) "Show / hide sections · $hidden hidden" else "Show / hide sections")
                    }
                }
            }
        }
    }

    val current = resume
    if (showManage && current != null) {
        ManageSectionsDialog(current.content.sections, onToggle = viewModel::setVisible, onDismiss = { showManage = false })
    }
    if (showCustom) {
        CustomSectionDialog(onConfirm = { name, layout -> showCustom = false; viewModel.addCustomSection(name, layout) }, onDismiss = { showCustom = false })
    }
    if (renaming && current != null) {
        TextInputDialog(title = "Rename resume", label = "Resume name", initial = current.title, onConfirm = { renaming = false; viewModel.rename(it) }, onDismiss = { renaming = false })
    }
    deletingSection?.let { section ->
        ConfirmDialog(
            title = "Delete section?",
            message = "“${section.title}” and its ${section.items.size} entries will be removed.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = { deletingSection = null; viewModel.deleteSection(section.id) },
            onDismiss = { deletingSection = null },
        )
    }
}

@Composable
private fun StrengthCard(resume: Resume) {
    val strength = resume.content.strength()
    val progress by animateFloatAsState(strength / 100f, label = "strength")
    val tips = remember(resume.content) { improvementTips(resume) }
    val color = when {
        strength >= 75 -> Success
        strength >= 45 -> MaterialTheme.colorScheme.primary
        else -> Color(0xFFF59E0B)
    }
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(64.dp),
                    color = color,
                    strokeWidth = 7.dp,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Text("$strength", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    when {
                        strength >= 85 -> "Excellent resume"
                        strength >= 65 -> "Strong resume"
                        strength >= 40 -> "Getting there"
                        else -> "Let's build it up"
                    },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (tips.isEmpty()) {
                    Text("You're ready to download and apply.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    tips.take(2).forEach { tip ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 3.dp)) {
                            Icon(Icons.Rounded.TipsAndUpdates, null, tint = color, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(tip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}

private fun improvementTips(resume: Resume): List<String> {
    val c = resume.content
    return buildList {
        if (c.contact.fullName.isBlank()) add("Add your name and contact details")
        if (c.summary.length < 120) add("Write a 3–4 sentence summary")
        if (c.section(SectionType.EXPERIENCE)?.items.isNullOrEmpty()) add("Add your work experience")
        if ((c.section(SectionType.SKILLS)?.items?.size ?: 0) < 6) add("List at least 6 key skills")
        if (c.section(SectionType.EDUCATION)?.items.isNullOrEmpty()) add("Add your education")
        if (c.contact.linkedin.isBlank()) add("Link your LinkedIn profile")
    }
}

@Composable
private fun SectionRow(
    section: ResumeSection,
    resume: Resume,
    dragging: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    handle: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    val complete = resume.content.isComplete(section)
    val tint = section.type.tint()
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = if (dragging) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, if (dragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(tint.copy(alpha = 0.13f)), contentAlignment = Alignment.Center) {
                Icon(section.type.icon(), null, tint = tint)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(section.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    when {
                        section.type.isListSection && section.items.isNotEmpty() -> "${section.items.size} ${if (section.items.size == 1) "entry" else "entries"}"
                        complete -> "Completed"
                        section.type == SectionType.SIGNATURE -> "Optional"
                        else -> "Tap to add"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (complete) Success else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (complete) {
                Icon(Icons.Rounded.CheckCircle, "Complete", tint = Success, modifier = Modifier.size(22.dp))
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, "Delete section", tint = MaterialTheme.colorScheme.error) }
            }
            handle()
        }
    }
}

@Composable
private fun ManageSectionsDialog(sections: List<ResumeSection>, onToggle: (String, Boolean) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage sections") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Info, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("Hidden sections keep their content but won't appear on your resume.", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
                sections.forEach { section ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable { onToggle(section.id, !section.visible) }.padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(section.type.icon(), null, tint = section.type.tint())
                        Spacer(Modifier.width(12.dp))
                        Text(section.title, modifier = Modifier.weight(1f))
                        Switch(checked = section.visible, onCheckedChange = { onToggle(section.id, it) })
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Done") } },
    )
}

@Composable
private fun CustomSectionDialog(onConfirm: (String, CustomLayout) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var layout by remember { mutableStateOf(CustomLayout.TITLE_DESCRIPTION) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create custom section") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Section name") }, placeholder = { Text("e.g. Volunteering") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                Text("Entry layout", style = MaterialTheme.typography.titleSmall)
                CustomLayout.entries.forEach { option ->
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                            .selectable(selected = layout == option, onClick = { layout = option }, role = Role.RadioButton)
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = layout == option, onClick = null)
                        Spacer(Modifier.width(10.dp))
                        Text(option.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(name.trim(), layout) }, enabled = name.isNotBlank()) { Text("Create") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
