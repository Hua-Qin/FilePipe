package dev.bikram.filepipe.ui.screens.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bikram.filepipe.R
import dev.bikram.filepipe.data.preferences.AppPreferences
import dev.bikram.filepipe.data.preferences.AppThemeMode
import dev.bikram.filepipe.data.preferences.SwipeAction
import dev.bikram.filepipe.ui.components.displayPath
import dev.bikram.filepipe.ui.components.safTreeUriToPath
import dev.bikram.filepipe.ui.feedback.rememberPlayTapSound

private val themePickerOrder = listOf(
    AppThemeMode.SYSTEM,
    AppThemeMode.LIGHT,
    AppThemeMode.DARK,
    AppThemeMode.BLACK
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val playTap = rememberPlayTapSound()
    val preferences by viewModel.preferencesFlow.collectAsStateWithLifecycle(initialValue = AppPreferences.DEFAULT)
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = safTreeUriToPath(uri)
            if (path != null) viewModel.setExportFolderUri(path)
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            viewModel.importFromUri(uri)
        }
    }

    LaunchedEffect(userMessage) {
        val message = userMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.clearUserMessage()
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding())
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + contentPadding.calculateBottomPadding() + 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_theme_section),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        themePickerOrder.forEach { mode ->
                            val selected = preferences.themeMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectable(
                                        selected = selected,
                                        onClick = {
                                            playTap()
                                            viewModel.setThemeMode(mode)
                                        },
                                        role = Role.RadioButton
                                    )
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selected,
                                    onClick = null
                                )
                                Text(
                                    text = themeModeLabel(mode),
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.theme_material_you),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.settings_material_you_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = preferences.useMaterialYou,
                                onCheckedChange = { enabled ->
                                    playTap()
                                    viewModel.setUseMaterialYou(enabled)
                                }
                            )
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "Swipe actions",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Swipe right →", style = MaterialTheme.typography.bodyLarge)
                            SwipeActionDropdown(
                                current = preferences.swipeStartToEnd,
                                excluded = preferences.swipeEndToStart,
                                onSelect = { viewModel.setSwipeStartToEnd(it) }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("← Swipe left", style = MaterialTheme.typography.bodyLarge)
                            SwipeActionDropdown(
                                current = preferences.swipeEndToStart,
                                excluded = preferences.swipeStartToEnd,
                                onSelect = { viewModel.setSwipeEndToStart(it) }
                            )
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_history_section),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_log_retention),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.settings_log_retention_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            LogRetentionDropdown(
                                currentDays = preferences.logRetentionDays,
                                onSelect = { viewModel.setLogRetentionDays(it) }
                            )
                        }
                    }
                }
            }

            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.settings_backup_section),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.settings_export_folder_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val folderLabel = preferences.exportFolderUri
                            .takeIf { it.isNotBlank() }
                            ?.let { path -> displayPath(path) }
                            ?: stringResource(R.string.settings_no_folder_chosen)
                        Text(
                            text = folderLabel,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                        OutlinedButton(
                            onClick = {
                                playTap()
                                folderLauncher.launch(null)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.FolderOpen, contentDescription = null)
                            Text(text = "  ${stringResource(R.string.settings_choose_export_folder)}")
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    playTap()
                                    viewModel.exportNow()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.settings_export_now))
                            }
                            OutlinedButton(
                                onClick = {
                                    playTap()
                                    importLauncher.launch("application/json")
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.settings_import_rules))
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_auto_export_on_change),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.settings_auto_export_on_change_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = preferences.autoExportOnRuleChange,
                                onCheckedChange = { enabled ->
                                    playTap()
                                    viewModel.setAutoExportOnChange(enabled)
                                }
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.settings_scheduled_export),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = stringResource(R.string.settings_scheduled_export_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = preferences.scheduledExportEnabled,
                                onCheckedChange = { enabled ->
                                    playTap()
                                    viewModel.setScheduledExportEnabled(enabled)
                                }
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = stringResource(R.string.settings_byline),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 8.dp)
                )
            }
        }
    }
}

private val LOG_RETENTION_OPTIONS = listOf(7, 14, 30, 90, -1)

@Composable
private fun logRetentionLabel(days: Int): String = when (days) {
    7 -> "7 days"
    14 -> "14 days"
    30 -> "30 days"
    90 -> "90 days"
    else -> "Never"
}

@Composable
private fun LogRetentionDropdown(currentDays: Int, onSelect: (Int) -> Unit) {
    val playTap = rememberPlayTapSound()
    var expanded by remember { mutableStateOf(false) }
    OutlinedButton(onClick = {
        playTap()
        expanded = true
    }) {
        Text(logRetentionLabel(currentDays))
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LOG_RETENTION_OPTIONS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(logRetentionLabel(option)) },
                    onClick = {
                        playTap()
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun swipeActionLabel(action: SwipeAction): String = when (action) {
    SwipeAction.DELETE -> "Delete"
    SwipeAction.EDIT -> "Edit"
    SwipeAction.DUPLICATE -> "Duplicate"
    SwipeAction.VIEW_HISTORY -> "View history"
}

@Composable
private fun SwipeActionDropdown(
    current: SwipeAction,
    excluded: SwipeAction,
    onSelect: (SwipeAction) -> Unit
) {
    val playTap = rememberPlayTapSound()
    var expanded by remember { mutableStateOf(false) }
    OutlinedButton(onClick = {
        playTap()
        expanded = true
    }) {
        Text(swipeActionLabel(current))
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SwipeAction.entries.filter { it != excluded }.forEach { action ->
                DropdownMenuItem(
                    text = { Text(swipeActionLabel(action)) },
                    onClick = {
                        playTap()
                        onSelect(action)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun themeModeLabel(mode: AppThemeMode): String = when (mode) {
    AppThemeMode.LIGHT -> stringResource(R.string.theme_light)
    AppThemeMode.DARK -> stringResource(R.string.theme_dark)
    AppThemeMode.SYSTEM -> stringResource(R.string.theme_system)
    AppThemeMode.BLACK -> stringResource(R.string.theme_black)
}
