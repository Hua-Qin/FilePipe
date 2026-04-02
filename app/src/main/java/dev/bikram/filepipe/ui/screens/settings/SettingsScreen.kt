package dev.bikram.filepipe.ui.screens.settings

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import dev.bikram.filepipe.BuildConfig
import dev.bikram.filepipe.R
import dev.bikram.filepipe.data.preferences.AppPreferences
import dev.bikram.filepipe.data.preferences.AppThemeMode
import dev.bikram.filepipe.data.preferences.SwipeAction
import dev.bikram.filepipe.ui.components.AppIconImage
import dev.bikram.filepipe.ui.components.displayPath
import dev.bikram.filepipe.ui.components.safTreeUriToPath
import dev.bikram.filepipe.ui.feedback.rememberPlayTapSound

private val themePickerOrder = listOf(
    AppThemeMode.SYSTEM,
    AppThemeMode.LIGHT,
    AppThemeMode.DARK,
    AppThemeMode.BLACK
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    onOpenIntro: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val playTap = rememberPlayTapSound()
    val preferences by viewModel.preferencesFlow.collectAsStateWithLifecycle(initialValue = AppPreferences.DEFAULT)
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val updateInfo by viewModel.updateInfo.collectAsStateWithLifecycle()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)

    val folderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val path = safTreeUriToPath(uri) ?: uri.toString()
            viewModel.setExportFolderUri(path)
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Theme ──────────────────────────────────────────────────────────
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.settings_theme_section)
                )
                Spacer(Modifier.height(8.dp))
                // Connected toggle-button group for theme mode
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
                ) {
                    themePickerOrder.forEachIndexed { index, mode ->
                        ToggleButton(
                            checked = preferences.themeMode == mode,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    playTap()
                                    viewModel.setThemeMode(mode)
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .semantics { role = Role.RadioButton },
                            shapes = when (index) {
                                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                                themePickerOrder.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
                            }
                        ) {
                            Text(
                                text = themeModeLabel(mode),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Material You toggle
                SettingsItemCard {
                    SettingsToggleItem(
                        title = stringResource(R.string.theme_material_you),
                        subtitle = stringResource(R.string.settings_material_you_hint),
                        checked = preferences.useMaterialYou,
                        onCheckedChange = { enabled ->
                            playTap()
                            viewModel.setUseMaterialYou(enabled)
                        }
                    )
                }
            }

            // ── Feedback ──────────────────────────────────────────────────────
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.Vibration,
                    title = stringResource(R.string.settings_feedback_section)
                )
                Spacer(Modifier.height(8.dp))
                SettingsItemCard {
                    SettingsToggleItem(
                        icon = Icons.Default.Vibration,
                        title = stringResource(R.string.settings_haptic_feedback),
                        subtitle = stringResource(R.string.settings_haptic_feedback_desc),
                        checked = preferences.hapticFeedbackEnabled,
                        onCheckedChange = { enabled ->
                            playTap()
                            viewModel.setHapticFeedbackEnabled(enabled)
                        }
                    )
                    SettingsToggleItem(
                        icon = Icons.Default.BlurOn,
                        title = stringResource(R.string.settings_progressive_blur),
                        subtitle = stringResource(R.string.settings_progressive_blur_desc),
                        checked = preferences.progressiveBlurEnabled,
                        onCheckedChange = { enabled ->
                            playTap()
                            viewModel.setProgressiveBlurEnabled(enabled)
                        },
                        isLast = true
                    )
                }
            }

            // ── Swipe Actions ─────────────────────────────────────────────────
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.SwipeLeft,
                    title = "Swipe actions"
                )
                Spacer(Modifier.height(8.dp))
                SettingsItemCard {
                    ListItem(
                        headlineContent = { Text("Swipe right →", style = MaterialTheme.typography.bodyLarge) },
                        trailingContent = {
                            SwipeActionDropdown(
                                current = preferences.swipeStartToEnd,
                                excluded = preferences.swipeEndToStart,
                                onSelect = { viewModel.setSwipeStartToEnd(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                    ListItem(
                        headlineContent = { Text("← Swipe left", style = MaterialTheme.typography.bodyLarge) },
                        trailingContent = {
                            SwipeActionDropdown(
                                current = preferences.swipeEndToStart,
                                excluded = preferences.swipeStartToEnd,
                                onSelect = { viewModel.setSwipeEndToStart(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                }
            }

            // ── History ───────────────────────────────────────────────────────
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.settings_history_section)
                )
                Spacer(Modifier.height(8.dp))
                SettingsItemCard {
                    ListItem(
                        headlineContent = {
                            Text(
                                stringResource(R.string.settings_log_retention),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        supportingContent = {
                            Text(
                                stringResource(R.string.settings_log_retention_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            LogRetentionDropdown(
                                currentDays = preferences.logRetentionDays,
                                onSelect = { viewModel.setLogRetentionDays(it) }
                            )
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                }
            }

            // ── Backup ────────────────────────────────────────────────────────
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.FolderOpen,
                    title = stringResource(R.string.settings_backup_section)
                )
                Spacer(Modifier.height(8.dp))
                SettingsItemCard {
                    val folderLabel = preferences.exportFolderUri
                        .takeIf { it.isNotBlank() }
                        ?.let { uriOrPath ->
                            if (uriOrPath.startsWith("content://")) {
                                android.net.Uri.parse(uriOrPath).authority
                                    ?.substringBefore(".")
                                    ?.replaceFirstChar { it.uppercase() }
                                    ?.let { "Cloud: $it" }
                                    ?: uriOrPath
                            } else {
                                displayPath(uriOrPath)
                            }
                        }
                        ?: stringResource(R.string.settings_no_folder_chosen)

                    ListItem(
                        headlineContent = { Text(folderLabel, style = MaterialTheme.typography.bodyLarge) },
                        supportingContent = {
                            Text(
                                stringResource(R.string.settings_export_folder_hint),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            OutlinedButton(onClick = {
                                playTap()
                                folderLauncher.launch(null)
                            }) {
                                Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                        )
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { playTap(); viewModel.exportNow() },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.settings_export_now)) }
                        OutlinedButton(
                            onClick = { playTap(); importLauncher.launch("application/json") },
                            modifier = Modifier.weight(1f)
                        ) { Text(stringResource(R.string.settings_import_rules)) }
                    }
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_auto_export_on_change),
                        subtitle = stringResource(R.string.settings_auto_export_on_change_hint),
                        checked = preferences.autoExportOnRuleChange,
                        onCheckedChange = { enabled ->
                            playTap()
                            viewModel.setAutoExportOnChange(enabled)
                        }
                    )
                    SettingsToggleItem(
                        title = stringResource(R.string.settings_scheduled_export),
                        subtitle = stringResource(R.string.settings_scheduled_export_hint),
                        checked = preferences.scheduledExportEnabled,
                        onCheckedChange = { enabled ->
                            playTap()
                            viewModel.setScheduledExportEnabled(enabled)
                        },
                        isLast = true
                    )
                }
            }

            // ── Updates (github flavor only) ──────────────────────────────────
            if (BuildConfig.SHOW_UPDATES) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = "Updates",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(12.dp))
                            if (updateInfo != null) {
                                Text(
                                    text = "Version ${updateInfo!!.versionName} is available",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                if (updateInfo!!.releaseNotes.isNotBlank()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = updateInfo!!.releaseNotes,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        playTap()
                                        viewModel.downloadAndInstall(updateInfo!!.downloadUrl)
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("Download & Install") }
                            } else {
                                OutlinedButton(
                                    onClick = { playTap(); viewModel.checkForUpdate() },
                                    enabled = !isCheckingUpdate,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isCheckingUpdate) {
                                        CircularProgressIndicator(
                                            modifier = Modifier
                                                .height(18.dp)
                                                .padding(end = 8.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }
                                    Text("Check for updates")
                                }
                            }
                        }
                    }
                }
            }

            // ── About ─────────────────────────────────────────────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenIntro() }
                        .padding(top = 24.dp, bottom = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    AppIconImage(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(percent = 25))
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.titleMedium
                    )
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
                        .padding(bottom = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsItemCard(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = { content() }
    )
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    isLast: Boolean = false
) {
    ListItem(
        headlineContent = { Text(title, style = MaterialTheme.typography.bodyLarge) },
        supportingContent = if (subtitle != null) {
            { Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        leadingContent = if (icon != null) {
            { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = Modifier.clickable { onCheckedChange(!checked) },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    )
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
    OutlinedButton(onClick = { playTap(); expanded = true }) {
        Text(logRetentionLabel(currentDays))
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            LOG_RETENTION_OPTIONS.forEach { option ->
                DropdownMenuItem(
                    text = { Text(logRetentionLabel(option)) },
                    onClick = { playTap(); onSelect(option); expanded = false }
                )
            }
        }
    }
}

@Composable
private fun swipeActionLabel(action: SwipeAction): String = when (action) {
    SwipeAction.EDIT -> "Edit"
    SwipeAction.DELETE -> "Delete"
    SwipeAction.DUPLICATE -> "Duplicate"
    SwipeAction.PREVIEW -> "Preview"
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
    OutlinedButton(onClick = { playTap(); expanded = true }) {
        Text(swipeActionLabel(current))
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            SwipeAction.entries.filter { it != excluded }.forEach { action ->
                DropdownMenuItem(
                    text = { Text(swipeActionLabel(action)) },
                    onClick = { playTap(); onSelect(action); expanded = false }
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
