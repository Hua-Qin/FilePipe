package dev.bikram.filepipe.ui.screens.ruledetail

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Screenshot
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.TextSnippet
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bikram.filepipe.R
import dev.bikram.filepipe.domain.model.ConflictPolicy
import dev.bikram.filepipe.domain.model.OperationMode
import dev.bikram.filepipe.domain.model.RuleIcon
import dev.bikram.filepipe.domain.model.RuleTemplate
import dev.bikram.filepipe.domain.model.ScheduleType
import dev.bikram.filepipe.ui.components.FileExtensionChips
import dev.bikram.filepipe.ui.components.FolderPickerButton
import dev.bikram.filepipe.ui.components.ScheduleDialog
import dev.bikram.filepipe.ui.components.absoluteStoragePathToOpenTreeInitialUri
import dev.bikram.filepipe.ui.components.safTreeUriToPath
import dev.bikram.filepipe.ui.components.toImageVector
import dev.bikram.filepipe.ui.feedback.rememberPlayTapSound

private val SectionButtonShape = RoundedCornerShape(12.dp)

private sealed class FolderPickIntent {
    data object AddSource : FolderPickIntent()
    data class ReplaceSource(val previousPath: String) : FolderPickIntent()
    data object SetDestination : FolderPickIntent()
}
private val PillShape = RoundedCornerShape(50)

@Composable
private fun ruleIconOptionLabel(icon: RuleIcon): String = stringResource(
    when (icon) {
        RuleIcon.DEFAULT -> R.string.rule_icon_label_default
        RuleIcon.IMAGE -> R.string.rule_icon_label_image
        RuleIcon.SCREENSHOT -> R.string.rule_icon_label_screenshot
        RuleIcon.VIDEO -> R.string.rule_icon_label_video
        RuleIcon.MUSIC -> R.string.rule_icon_label_music
        RuleIcon.DOWNLOAD -> R.string.rule_icon_label_download
        RuleIcon.DOCUMENT -> R.string.rule_icon_label_document
    }
)

@Composable
private fun RuleSectionCard(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RuleDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: RuleDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isDirty by viewModel.isDirty.collectAsStateWithLifecycle()
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var showTemplateSheet by remember { mutableStateOf(viewModel.isNewRule) }
    var ruleIconMenuExpanded by remember { mutableStateOf(false) }
    val previewSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val playTap = rememberPlayTapSound()

    var pendingFolderPick by remember { mutableStateOf<FolderPickIntent?>(null) }
    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        val pickedPath = uri?.let { safTreeUriToPath(it) }
        if (pickedPath == null) {
            pendingFolderPick = null
            return@rememberLauncherForActivityResult
        }
        when (val pending = pendingFolderPick) {
            FolderPickIntent.AddSource -> viewModel.addSourceFolder(pickedPath)
            is FolderPickIntent.ReplaceSource ->
                viewModel.replaceSourceFolder(pending.previousPath, pickedPath)
            FolderPickIntent.SetDestination -> viewModel.setDestination(pickedPath)
            null -> {}
        }
        pendingFolderPick = null
    }

    fun launchFolderPicker(intent: FolderPickIntent, initialAbsolutePath: String?) {
        pendingFolderPick = intent
        folderPickerLauncher.launch(initialAbsolutePath?.let { absoluteStoragePathToOpenTreeInitialUri(it) })
    }

    fun withTapSound(action: () -> Unit) {
        playTap()
        action()
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onNavigateBack()
    }

    fun tryNavigateBack() {
        when {
            showDiscardDialog -> showDiscardDialog = false
            isDirty -> showDiscardDialog = true
            else -> onNavigateBack()
        }
    }

    BackHandler(onBack = ::tryNavigateBack)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNewRule) stringResource(R.string.new_rule) else stringResource(R.string.edit_rule)) },
                navigationIcon = {
                    IconButton(onClick = { withTapSound(::tryNavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { withTapSound { viewModel.loadPreview() } },
                        enabled = state.sourceFolderPaths.isNotEmpty() && state.fileExtensions.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Visibility, contentDescription = stringResource(R.string.preview_rule))
                    }
                }
            )
        },
        bottomBar = {
            if (!state.isLoading) {
                Surface(tonalElevation = 3.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { withTapSound(::tryNavigateBack) },
                            modifier = Modifier.weight(1f),
                            shape = PillShape
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = { withTapSound { viewModel.save() } },
                            modifier = Modifier.weight(1f),
                            shape = PillShape
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (state.isLoading) {
            Column(
                Modifier.fillMaxSize().padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.errors.isNotEmpty()) {
                state.errors.forEach { error ->
                    Text(
                        text = "• $error",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    FilledTonalIconButton(
                        onClick = { withTapSound { ruleIconMenuExpanded = true } },
                        modifier = Modifier.size(52.dp),
                        shape = SectionButtonShape,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                        )
                    ) {
                        Icon(
                            imageVector = state.icon.toImageVector(),
                            contentDescription = stringResource(R.string.rule_icon_picker_cd),
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    DropdownMenu(
                        expanded = ruleIconMenuExpanded,
                        onDismissRequest = { ruleIconMenuExpanded = false },
                        modifier = Modifier.heightIn(max = 360.dp)
                    ) {
                        RuleIcon.entries.forEach { iconOption ->
                            DropdownMenuItem(
                                text = { Text(ruleIconOptionLabel(iconOption)) },
                                onClick = {
                                    withTapSound {
                                        viewModel.setIcon(iconOption)
                                        ruleIconMenuExpanded = false
                                    }
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = iconOption.toImageVector(),
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::setName,
                    label = { Text(stringResource(R.string.rule_name_label)) },
                    placeholder = { Text(stringResource(R.string.rule_name_placeholder)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            RuleSectionCard(
                title = stringResource(R.string.rule_section_extensions_title),
                subtitle = stringResource(R.string.rule_section_extensions_subtitle),
                icon = Icons.Filled.Extension
            ) {
                FileExtensionChips(
                    extensions = state.fileExtensions,
                    onAdd = viewModel::addExtension,
                    onAddGroup = viewModel::addExtensions,
                    onRemove = viewModel::removeExtension
                )
            }

            RuleSectionCard(
                title = stringResource(R.string.source_folders_label),
                subtitle = stringResource(R.string.rule_section_source_subtitle),
                icon = Icons.Filled.Search
            ) {
                state.sourceFolderPaths.forEach { path ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = path,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    withTapSound {
                                        launchFolderPicker(FolderPickIntent.ReplaceSource(path), path)
                                    }
                                }
                        )
                        IconButton(onClick = { withTapSound { viewModel.removeSourceFolder(path) } }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FolderPickerButton(
                        label = stringResource(R.string.add_source_folder),
                        onClick = { launchFolderPicker(FolderPickIntent.AddSource, null) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.rule_scan_subdirs_label),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = state.scanSubdirectories,
                        onCheckedChange = { enabled ->
                            withTapSound { viewModel.setScanSubdirectories(enabled) }
                        }
                    )
                }
            }

            RuleSectionCard(
                title = stringResource(R.string.destination_label),
                subtitle = stringResource(R.string.rule_section_destination_subtitle),
                icon = Icons.Filled.FolderSpecial
            ) {
                if (state.destinationFolderPath.isNotBlank()) {
                    Text(
                        text = state.destinationFolderPath,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable {
                            withTapSound {
                                launchFolderPicker(
                                    FolderPickIntent.SetDestination,
                                    state.destinationFolderPath
                                )
                            }
                        }
                    )
                }
                FolderPickerButton(
                    label = if (state.destinationFolderPath.isBlank()) {
                        stringResource(R.string.pick_folder)
                    } else {
                        stringResource(R.string.change_destination)
                    },
                    onClick = {
                        launchFolderPicker(
                            FolderPickIntent.SetDestination,
                            state.destinationFolderPath.takeIf { it.isNotBlank() }
                        )
                    }
                )
            }

            RuleSectionCard(
                title = stringResource(R.string.rule_section_operation_title),
                subtitle = null,
                icon = Icons.Filled.ContentCopy
            ) {
                Text(
                    text = stringResource(R.string.rule_operation_mode_label),
                    style = MaterialTheme.typography.bodyMedium
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    OperationMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = state.operationMode == mode,
                            onClick = { withTapSound { viewModel.setOperationMode(mode) } },
                            shape = SegmentedButtonDefaults.itemShape(index, OperationMode.entries.size),
                            label = {
                                Text(
                                    when (mode) {
                                        OperationMode.MOVE -> stringResource(R.string.operation_move)
                                        OperationMode.COPY -> stringResource(R.string.operation_copy)
                                    }
                                )
                            }
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = stringResource(R.string.rule_conflict_policy_label),
                    style = MaterialTheme.typography.bodyMedium
                )
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ConflictPolicy.entries.forEachIndexed { index, policy ->
                        SegmentedButton(
                            selected = state.conflictPolicy == policy,
                            onClick = { withTapSound { viewModel.setConflictPolicy(policy) } },
                            shape = SegmentedButtonDefaults.itemShape(index, ConflictPolicy.entries.size),
                            label = {
                                Text(
                                    when (policy) {
                                        ConflictPolicy.SKIP -> stringResource(R.string.conflict_skip)
                                        ConflictPolicy.OVERWRITE -> stringResource(R.string.conflict_overwrite)
                                        ConflictPolicy.RENAME_SUFFIX -> stringResource(R.string.conflict_rename)
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }
            }

            RuleSectionCard(
                title = stringResource(R.string.schedule_label),
                subtitle = stringResource(R.string.rule_section_schedule_subtitle),
                icon = Icons.Default.DateRange
            ) {
                val schedule = state.schedule
                if (schedule != null) {
                    val scheduleText = when (schedule.type) {
                        ScheduleType.DAILY -> "Daily at %02d:%02d".format(schedule.hour, schedule.minute)
                        ScheduleType.WEEKLY -> {
                            val days = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            val dayName = schedule.dayOfWeek?.let { days.getOrNull(it - 2) } ?: "?"
                            "Weekly ($dayName) at %02d:%02d".format(schedule.hour, schedule.minute)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { withTapSound { showScheduleDialog = true } },
                            modifier = Modifier.weight(1f),
                            shape = SectionButtonShape
                        ) {
                            Icon(
                                Icons.Default.DateRange,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(text = "  $scheduleText")
                        }
                        IconButton(onClick = { withTapSound { viewModel.setSchedule(null) } }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_schedule))
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { withTapSound { showScheduleDialog = true } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = SectionButtonShape
                    ) {
                        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text(text = "  ${stringResource(R.string.add_schedule_chip)}")
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text(stringResource(R.string.unsaved_changes_title)) },
            text = { Text(stringResource(R.string.unsaved_changes_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        withTapSound {
                            showDiscardDialog = false
                            onNavigateBack()
                        }
                    }
                ) {
                    Text(stringResource(R.string.discard_changes))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { withTapSound { showDiscardDialog = false } }) {
                        Text(stringResource(R.string.keep_editing))
                    }
                    TextButton(
                        onClick = {
                            withTapSound {
                                showDiscardDialog = false
                                viewModel.save()
                            }
                        }
                    ) {
                        Text(stringResource(R.string.save_and_exit))
                    }
                }
            }
        )
    }

    if (showScheduleDialog) {
        ScheduleDialog(
            initialSchedule = state.schedule,
            onDismiss = { showScheduleDialog = false },
            onSave = { schedule ->
                viewModel.setSchedule(schedule)
                showScheduleDialog = false
            }
        )
    }

    // Template picker — shown only for new rules, once
    if (showTemplateSheet && viewModel.isNewRule) {
        ModalBottomSheet(
            onDismissRequest = { showTemplateSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = stringResource(R.string.template_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "Pick a starting point — you can customize everything after.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                RuleTemplate.ALL.forEach { template ->
                    ElevatedCard(
                        onClick = {
                            withTapSound {
                                viewModel.applyTemplate(template)
                                showTemplateSheet = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(
                                    imageVector = template.suggestedIcon.toImageVector(),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(template.name, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    template.extensions.take(5).joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
                TextButton(
                    onClick = { withTapSound { showTemplateSheet = false } },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = PillShape
                ) {
                    Text(stringResource(R.string.template_skip))
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Preview bottom sheet
    if (state.previewFiles != null || state.isPreviewLoading) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissPreview() },
            sheetState = previewSheetState
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    text = stringResource(R.string.preview_title),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (state.isPreviewLoading) {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally).padding(32.dp))
                } else {
                    val files = state.previewFiles ?: emptyList()
                    if (files.isEmpty()) {
                        Text(
                            text = stringResource(R.string.preview_empty),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 16.dp)
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.preview_count, files.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            items(files) { file ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = file.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val sizeKb = file.size / 1024
                                    Text(
                                        text = if (sizeKb > 1024) "${sizeKb / 1024} MB" else "$sizeKb KB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
