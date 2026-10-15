package com.resume.builder.presentation.editor

import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Cake
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.Work
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.resume.builder.domain.model.ContactInfo
import com.resume.builder.domain.model.ImproveKind
import com.resume.builder.domain.model.ItemFields
import com.resume.builder.domain.model.ResumeSection
import com.resume.builder.domain.model.SectionItem
import com.resume.builder.domain.model.SectionType
import com.resume.builder.domain.model.SignatureInfo
import com.resume.builder.presentation.components.AppTopBar
import com.resume.builder.presentation.components.ChoiceChips
import com.resume.builder.presentation.components.ConfirmDialog
import com.resume.builder.presentation.components.SignaturePad
import com.resume.builder.presentation.components.TextInputDialog
import com.resume.builder.presentation.components.rememberSignatureState
import com.resume.builder.ui.theme.Gradients
import com.resume.builder.ui.theme.Violet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SectionEditorScreen(
    resumeId: String,
    sectionId: String,
    onBack: () -> Unit,
    viewModel: SectionEditorViewModel = koinViewModel(key = "section-$resumeId-$sectionId") { parametersOf(resumeId, sectionId) },
) {
    val resume by viewModel.resume.collectAsStateWithLifecycle()
    val aiBusy by viewModel.aiBusy.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var renaming by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<SectionItem?>(null) }
    var deletingItem by remember { mutableStateOf<SectionItem?>(null) }

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    val section = resume?.content?.sectionById(sectionId)
    Scaffold(
        topBar = {
            AppTopBar(
                title = section?.title ?: "",
                onBack = onBack,
                actions = {
                    if (section != null && section.type.isListSection) {
                        IconButton(onClick = { renaming = true }) { Icon(Icons.Rounded.DriveFileRenameOutline, "Rename section") }
                    }
                    TextButton(onClick = onBack) { Text("Done") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        val content = resume?.content
        if (content == null || section == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        val body = Modifier.fillMaxSize().padding(padding).imePadding()
        when (section.type) {
            SectionType.CONTACT -> ContactEditor(
                contact = content.contact,
                onChange = viewModel::updateContact,
                onPhoto = viewModel::setPhoto,
                onRemovePhoto = viewModel::removePhoto,
                modifier = body,
            )
            SectionType.SUMMARY -> SummaryEditor(
                summary = content.summary,
                aiBusy = aiBusy,
                onChange = viewModel::updateSummary,
                onImprove = { viewModel.improve(ImproveKind.SUMMARY, content.summary) { viewModel.updateSummary(it) } },
                modifier = body,
            )
            SectionType.SIGNATURE -> SignatureEditor(
                signature = content.signature,
                fallbackName = content.contact.fullName,
                onChange = viewModel::updateSignature,
                onSave = viewModel::saveSignature,
                onClear = viewModel::clearSignature,
                modifier = body,
            )
            else -> ItemsEditor(
                section = section,
                onAdd = { editingItem = SectionItem() },
                onEdit = { editingItem = it },
                onDelete = { deletingItem = it },
                onMove = viewModel::moveItem,
                onQuickAdd = viewModel::quickAdd,
                modifier = body,
            )
        }
    }

    if (renaming && section != null) {
        TextInputDialog(title = "Rename section", label = "Section title", initial = section.title, onConfirm = { renaming = false; viewModel.renameSection(it) }, onDismiss = { renaming = false })
    }
    val currentSection = section
    editingItem?.let { item ->
        if (currentSection != null) {
            ItemEditorSheet(
                initial = item,
                fields = currentSection.type.fieldsFor(currentSection.customLayout),
                isNew = currentSection.items.none { it.id == item.id },
                aiBusy = aiBusy,
                onImprove = { text, apply -> viewModel.improve(ImproveKind.BULLETS, text, apply) },
                onSave = { viewModel.upsertItem(it); editingItem = null },
                onDismiss = { editingItem = null },
            )
        }
    }
    deletingItem?.let { item ->
        ConfirmDialog(
            title = "Delete entry?",
            message = "“${item.title.ifBlank { "Untitled" }}” will be removed.",
            confirmText = "Delete",
            destructive = true,
            onConfirm = { deletingItem = null; viewModel.deleteItem(item.id) },
            onDismiss = { deletingItem = null },
        )
    }
}

// region Contact
private data class ContactFieldUi(
    val label: String,
    val icon: ImageVector,
    val value: String,
    val keyboard: KeyboardType,
    val capitalization: KeyboardCapitalization,
    val update: (ContactInfo, String) -> ContactInfo,
)

@Composable
private fun ContactEditor(
    contact: ContactInfo,
    onChange: ((ContactInfo) -> ContactInfo) -> Unit,
    onPhoto: (android.net.Uri) -> Unit,
    onRemovePhoto: () -> Unit,
    modifier: Modifier,
) {
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> uri?.let(onPhoto) }
    val photo by rememberBitmap(contact.photoPath)
    val fields = listOf(
        ContactFieldUi("Full name", Icons.Rounded.Person, contact.fullName, KeyboardType.Text, KeyboardCapitalization.Words) { c, v -> c.copy(fullName = v) },
        ContactFieldUi("Job title", Icons.Rounded.Work, contact.jobTitle, KeyboardType.Text, KeyboardCapitalization.Words) { c, v -> c.copy(jobTitle = v) },
        ContactFieldUi("Email", Icons.Rounded.Email, contact.email, KeyboardType.Email, KeyboardCapitalization.None) { c, v -> c.copy(email = v) },
        ContactFieldUi("Phone", Icons.Rounded.Phone, contact.phone, KeyboardType.Phone, KeyboardCapitalization.None) { c, v -> c.copy(phone = v) },
        ContactFieldUi("Location", Icons.Rounded.LocationOn, contact.location, KeyboardType.Text, KeyboardCapitalization.Words) { c, v -> c.copy(location = v) },
        ContactFieldUi("Website / portfolio", Icons.Rounded.Language, contact.website, KeyboardType.Uri, KeyboardCapitalization.None) { c, v -> c.copy(website = v) },
        ContactFieldUi("LinkedIn", Icons.Rounded.Badge, contact.linkedin, KeyboardType.Uri, KeyboardCapitalization.None) { c, v -> c.copy(linkedin = v) },
        ContactFieldUi("GitHub", Icons.Rounded.Code, contact.github, KeyboardType.Uri, KeyboardCapitalization.None) { c, v -> c.copy(github = v) },
        ContactFieldUi("Date of birth (optional)", Icons.Rounded.Cake, contact.dateOfBirth, KeyboardType.Text, KeyboardCapitalization.None) { c, v -> c.copy(dateOfBirth = v) },
    )
    Column(modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 8.dp)) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    contentAlignment = Alignment.Center,
                ) {
                    photo?.let { Image(it, "Profile photo", contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                        ?: Icon(Icons.Rounded.AddAPhoto, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Profile photo", style = MaterialTheme.typography.titleMedium)
                    Text("Optional — some templates show it. Skip it for ATS-focused applications.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilledTonalButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, contentPadding = PaddingValues(horizontal = 14.dp)) {
                            Text(if (contact.photoPath == null) "Upload" else "Change")
                        }
                        if (contact.photoPath != null) {
                            TextButton(onClick = onRemovePhoto) { Text("Remove", color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        fields.forEach { field ->
            OutlinedTextField(
                value = field.value,
                onValueChange = { value -> onChange { field.update(it, value) } },
                label = { Text(field.label) },
                leadingIcon = { Icon(field.icon, null) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = field.keyboard, capitalization = field.capitalization, imeAction = ImeAction.Next),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun rememberBitmap(path: String?) = produceState<ImageBitmap?>(null, path) {
    value = path?.let { withContext(Dispatchers.IO) { BitmapFactory.decodeFile(it, BitmapFactory.Options().apply { inSampleSize = 2 })?.asImageBitmap() } }
}
// endregion

// region Summary
@Composable
private fun SummaryEditor(summary: String, aiBusy: Boolean, onChange: (String) -> Unit, onImprove: () -> Unit, modifier: Modifier) {
    var showTips by remember { mutableStateOf(false) }
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        AiAssistCard(
            title = if (summary.isBlank()) "Draft it with AI" else "Polish with AI",
            subtitle = if (summary.isBlank()) "Uses your job title to write a strong opening" else "Make it sharper, specific and recruiter-ready",
            busy = aiBusy,
            onClick = onImprove,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = summary,
            onValueChange = onChange,
            label = { Text("Professional summary") },
            placeholder = { Text("e.g. Product designer with 6+ years crafting fintech experiences used by millions…") },
            minLines = 8,
            shape = RoundedCornerShape(18.dp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
            supportingText = {
                val count = summary.length
                Text(
                    "$count characters · ${if (count in 250..700) "great length" else "aim for 250–700"}",
                    color = if (count in 250..700) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = { showTips = !showTips }) {
            Icon(Icons.Rounded.TipsAndUpdates, null, Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(if (showTips) "Hide writing tips" else "Show writing tips")
        }
        AnimatedVisibility(showTips) {
            Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(
                        "Open with your title and years of experience",
                        "Mention 2–3 skills that match the jobs you want",
                        "Add one measurable achievement",
                        "Keep it to 3–4 sentences, no \"I\" needed",
                    ).forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium) }
                }
            }
        }
    }
}

@Composable
private fun AiAssistCard(title: String, subtitle: String, busy: Boolean, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Gradients.hero).clickable(enabled = !busy, onClick = onClick).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
                if (busy) CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp) else Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White)
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text(if (busy) "Writing…" else title, style = MaterialTheme.typography.titleMedium, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.82f))
            }
        }
    }
}
// endregion

// region Signature
@Composable
private fun SignatureEditor(
    signature: SignatureInfo,
    fallbackName: String,
    onChange: ((SignatureInfo) -> SignatureInfo) -> Unit,
    onSave: (android.graphics.Bitmap) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier,
) {
    val padState = rememberSignatureState()
    val image by rememberBitmap(signature.imagePath)
    Column(modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Draw your signature", style = MaterialTheme.typography.titleMedium)
        Text("It appears at the end of your resume. Great for applications that require a signed CV.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))
        if (signature.imagePath != null && image != null) {
            Box(
                Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(20.dp)).background(Color.White).border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(20.dp)).padding(20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Image(image!!, "Signature", contentScale = ContentScale.Fit)
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onClear, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.Delete, null, Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Remove & draw again")
            }
        } else {
            SignaturePad(padState)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = padState::clear, modifier = Modifier.weight(1f)) { Text("Clear") }
                Button(onClick = { padState.toBitmap()?.let(onSave) }, enabled = !padState.isEmpty, modifier = Modifier.weight(1f)) { Text("Use signature") }
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedTextField(
            value = signature.fullName,
            onValueChange = { v -> onChange { it.copy(fullName = v) } },
            label = { Text("Name under signature") },
            placeholder = { Text(fallbackName) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = signature.place, onValueChange = { v -> onChange { it.copy(place = v) } }, label = { Text("Place") }, singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f))
            OutlinedTextField(value = signature.date, onValueChange = { v -> onChange { it.copy(date = v) } }, label = { Text("Date") }, singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f))
        }
    }
}
// endregion

// region List sections
@Composable
private fun ItemsEditor(
    section: ResumeSection,
    onAdd: () -> Unit,
    onEdit: (SectionItem) -> Unit,
    onDelete: (SectionItem) -> Unit,
    onMove: (String, Int) -> Unit,
    onQuickAdd: (String) -> Unit,
    modifier: Modifier,
) {
    val fields = section.type.fieldsFor(section.customLayout)
    var quick by remember { mutableStateOf("") }
    LazyColumn(modifier, contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (fields.quickAdd) {
            item {
                Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.padding(14.dp)) {
                        Text("Quick add", style = MaterialTheme.typography.titleSmall)
                        Text("Separate multiple ${fields.titleLabel.lowercase()}s with commas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = quick,
                                onValueChange = { quick = it },
                                placeholder = { Text(if (section.type == SectionType.LANGUAGES) "English, Spanish" else "Kotlin, Figma, SQL") },
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp),
                                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { onQuickAdd(quick); quick = "" }),
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { onQuickAdd(quick); quick = "" },
                                enabled = quick.isNotBlank(),
                                modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primary),
                            ) { Icon(Icons.Rounded.Add, "Add", tint = MaterialTheme.colorScheme.onPrimary) }
                        }
                    }
                }
            }
        }
        if (section.items.isEmpty()) {
            item {
                Column(Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(72.dp).clip(CircleShape).background(section.type.tint().copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
                        Icon(section.type.icon(), null, tint = section.type.tint(), modifier = Modifier.size(34.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("No ${section.title.lowercase()} yet", style = MaterialTheme.typography.titleMedium)
                    Text("Add your first entry below", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        itemsIndexed(section.items, key = { _, item -> item.id }) { index, item ->
            ItemCard(
                item = item,
                fields = fields,
                canMoveUp = index > 0,
                canMoveDown = index < section.items.lastIndex,
                onClick = { onEdit(item) },
                onMoveUp = { onMove(item.id, -1) },
                onMoveDown = { onMove(item.id, 1) },
                onDelete = { onDelete(item) },
                modifier = Modifier.animateItem(),
            )
        }
        item {
            Button(onClick = onAdd, modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp), shape = RoundedCornerShape(18.dp)) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(8.dp))
                Text("Add ${fields.titleLabel.lowercase()}")
            }
        }
    }
}

@Composable
private fun ItemCard(
    item: SectionItem,
    fields: ItemFields,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(Modifier.padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val meta = listOf(item.subtitle, item.dateRange()).filter { it.isNotBlank() }.joinToString(" · ")
                if (meta.isNotBlank()) Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (item.level > 0 && fields.levelLabels != null) {
                    Row(Modifier.padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        repeat(5) { i ->
                            Box(Modifier.padding(end = 4.dp).size(8.dp).clip(CircleShape).background(if (i < item.level) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(fields.levelLabels.getOrElse(item.level - 1) { "" }, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Column {
                IconButton(onClick = onMoveUp, enabled = canMoveUp, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.KeyboardArrowUp, "Move up") }
                IconButton(onClick = onMoveDown, enabled = canMoveDown, modifier = Modifier.size(32.dp)) { Icon(Icons.Rounded.KeyboardArrowDown, "Move down") }
            }
            IconButton(onClick = onDelete) { Icon(Icons.Rounded.DeleteOutline, "Delete", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemEditorSheet(
    initial: SectionItem,
    fields: ItemFields,
    isNew: Boolean,
    aiBusy: Boolean,
    onImprove: (String, (String) -> Unit) -> Unit,
    onSave: (SectionItem) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember(initial.id) { mutableStateOf(initial) }
    var description by remember(initial.id) { mutableStateOf(TextFieldValue(initial.description)) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).imePadding().navigationBarsPadding(),
        ) {
            Text(if (isNew) "Add ${fields.titleLabel.lowercase()}" else "Edit ${fields.titleLabel.lowercase()}", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(16.dp))
            SheetField(fields.titleLabel, draft.title, KeyboardCapitalization.Words) { draft = draft.copy(title = it) }
            fields.subtitleLabel?.let { label -> SheetField(label, draft.subtitle, KeyboardCapitalization.Words) { draft = draft.copy(subtitle = it) } }
            fields.locationLabel?.let { label ->
                SheetField(label, draft.location, if (label == "Email") KeyboardCapitalization.None else KeyboardCapitalization.Words, if (label == "Email") KeyboardType.Email else KeyboardType.Text) { draft = draft.copy(location = it) }
            }
            if (fields.hasDates) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = draft.startDate, onValueChange = { draft = draft.copy(startDate = it) }, label = { Text("Start") },
                        placeholder = { Text("Jan 2022") }, singleLine = true, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = if (draft.isCurrent) "Present" else draft.endDate, onValueChange = { draft = draft.copy(endDate = it) }, label = { Text("End") },
                        placeholder = { Text("Mar 2024") }, singleLine = true, enabled = !draft.isCurrent, shape = RoundedCornerShape(16.dp), modifier = Modifier.weight(1f),
                    )
                }
                if (fields.hasCurrent) {
                    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("I currently ${if (fields.subtitleLabel == "School / University") "study" else "work"} here", modifier = Modifier.weight(1f))
                        Switch(checked = draft.isCurrent, onCheckedChange = { draft = draft.copy(isCurrent = it) })
                    }
                }
                Spacer(Modifier.height(6.dp))
            }
            fields.linkLabel?.let { label ->
                SheetField(label, draft.link, KeyboardCapitalization.None, if (label == "Phone") KeyboardType.Phone else KeyboardType.Uri, icon = Icons.Rounded.Link) { draft = draft.copy(link = it) }
            }
            fields.levelLabels?.let { labels ->
                Text("Proficiency", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))
                ChoiceChips(options = (0..5).toList(), selected = draft.level, label = { if (it == 0) "Don't show" else labels[it - 1] }, onSelect = { draft = draft.copy(level = it) })
                Spacer(Modifier.height(14.dp))
            }
            fields.descriptionLabel?.let { label ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        val text = description.text
                        val prefix = if (text.isEmpty() || text.endsWith("\n")) "• " else "\n• "
                        val updated = text + prefix
                        description = TextFieldValue(updated, TextRange(updated.length))
                    }) { Icon(Icons.Rounded.FormatListBulleted, "Add bullet") }
                    TextButton(
                        enabled = !aiBusy,
                        onClick = {
                            val context = listOf(draft.title, draft.subtitle).filter { it.isNotBlank() }.joinToString(" at ")
                            onImprove(if (description.text.isBlank()) "Role: $context" else description.text) { result ->
                                description = TextFieldValue(result, TextRange(result.length))
                            }
                        },
                    ) {
                        if (aiBusy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(Icons.Rounded.AutoAwesome, null, tint = Violet, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (description.text.isBlank()) "Write with AI" else "Improve")
                    }
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("• Increased conversion by 18% by redesigning checkout") },
                    minLines = 5,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("Cancel") }
                Button(
                    onClick = { onSave(draft.copy(description = description.text.trim())) },
                    enabled = draft.title.isNotBlank(),
                    modifier = Modifier.weight(1f).heightIn(min = 52.dp),
                ) { Text("Save") }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun ColumnScope.SheetField(
    label: String,
    value: String,
    capitalization: KeyboardCapitalization,
    keyboard: KeyboardType = KeyboardType.Text,
    icon: ImageVector? = null,
    onChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        leadingIcon = icon?.let { { Icon(it, null) } },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(capitalization = capitalization, keyboardType = keyboard, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
    )
}
// endregion
