package dev.bikram.filepipe.ui.screens.ruledetail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.FolderSpecial
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import dev.bikram.filepipe.domain.model.ScheduleType
import dev.bikram.filepipe.ui.components.FileExtensionChips
import dev.bikram.filepipe.ui.components.FolderPickerButton
import dev.bikram.filepipe.ui.components.ScheduleDialog
import dev.bikram.filepipe.ui.components.uriToDisplayName

private val SectionButtonShape = RoundedCornerShape(12.dp)

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
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RuleDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: RuleDetailViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isDirty by viewModel.isDirty.collectAsStateWithLifecycle()
    var showScheduleDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

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
                    IconButton(onClick = ::tryNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                            onClick = ::tryNavigateBack,
                            modifier = Modifier.weight(1f),
                            shape = SectionButtonShape
                        ) {
                            Text(stringResource(R.string.cancel))
                        }
                        Button(
                            onClick = { viewModel.save() },
                            modifier = Modifier.weight(1f),
                            shape = SectionButtonShape
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

            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = { Text(stringResource(R.string.rule_name_label)) },
                placeholder = { Text(stringResource(R.string.rule_name_placeholder)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            RuleSectionCard(
                title = stringResource(R.string.rule_section_extensions_title),
                subtitle = stringResource(R.string.rule_section_extensions_subtitle),
                icon = Icons.Filled.Extension
            ) {
                FileExtensionChips(
                    extensions = state.fileExtensions,
                    onAdd = viewModel::addExtension,
                    onRemove = viewModel::removeExtension
                )
            }

            RuleSectionCard(
                title = stringResource(R.string.source_folders_label),
                subtitle = stringResource(R.string.rule_section_source_subtitle),
                icon = Icons.Filled.Search
            ) {
                state.sourceFolderUris.forEach { uri ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = uriToDisplayName(uri),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.removeSourceFolder(uri) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(18.dp))
                        }
                    }
                }
                FolderPickerButton(
                    label = stringResource(R.string.add_source_folder),
                    onFolderPicked = viewModel::addSourceFolder
                )
            }

            RuleSectionCard(
                title = stringResource(R.string.destination_label),
                subtitle = stringResource(R.string.rule_section_destination_subtitle),
                icon = Icons.Filled.FolderSpecial
            ) {
                if (state.destinationFolderUri.isNotBlank()) {
                    Text(
                        text = uriToDisplayName(state.destinationFolderUri),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FolderPickerButton(
                    label = if (state.destinationFolderUri.isBlank()) {
                        stringResource(R.string.pick_folder)
                    } else {
                        stringResource(R.string.change_destination)
                    },
                    onFolderPicked = viewModel::setDestination
                )
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
                            onClick = { showScheduleDialog = true },
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
                        IconButton(onClick = { viewModel.setSchedule(null) }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(R.string.remove_schedule))
                        }
                    }
                } else {
                    OutlinedButton(
                        onClick = { showScheduleDialog = true },
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
                        showDiscardDialog = false
                        onNavigateBack()
                    }
                ) {
                    Text(stringResource(R.string.discard_changes))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showDiscardDialog = false }) {
                        Text(stringResource(R.string.keep_editing))
                    }
                    TextButton(
                        onClick = {
                            showDiscardDialog = false
                            viewModel.save()
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
}
