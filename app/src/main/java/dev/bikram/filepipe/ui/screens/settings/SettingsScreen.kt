package dev.bikram.filepipe.ui.screens.settings

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.NewReleases
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.SwipeLeft
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bikram.filepipe.BuildConfig
import dev.bikram.filepipe.R
import dev.bikram.filepipe.data.preferences.AppColorSource
import dev.bikram.filepipe.data.preferences.AppPreferences
import dev.bikram.filepipe.data.preferences.AppThemeMode
import dev.bikram.filepipe.data.preferences.SwipeAction
import dev.bikram.filepipe.ui.components.AboutAuthorPhoto
import dev.bikram.filepipe.ui.components.AppIconImage
import dev.bikram.filepipe.ui.components.containers.GroupPosition
import dev.bikram.filepipe.ui.components.containers.GroupedListColumn
import dev.bikram.filepipe.ui.components.containers.GroupedListItem
import dev.bikram.filepipe.ui.components.displayPath
import dev.bikram.filepipe.ui.components.safTreeUriToPath
import dev.bikram.filepipe.ui.feedback.rememberPlayTapSound
import dev.bikram.filepipe.ui.theme.LocalUseGradientBackground
import dev.bikram.filepipe.update.UpdateInfo

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
    val downloadProgress by viewModel.downloadProgress.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    fun computeNotificationsEnabled(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }
    var notificationsGranted by remember { mutableStateOf(computeNotificationsEnabled()) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                notificationsGranted = computeNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val changelogUi by viewModel.changelogUi.collectAsStateWithLifecycle()
    val changelogSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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

    if (changelogUi !is ChangelogUiState.Hidden) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.dismissChangelogSheet() },
            sheetState = changelogSheetState
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = stringResource(R.string.settings_changelog_title),
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(Modifier.height(12.dp))
                when (val state = changelogUi) {
                    ChangelogUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(modifier = Modifier.size(40.dp))
                        }
                    }
                    is ChangelogUiState.Ready -> {
                        val changelogScroll = rememberScrollState()
                        Text(
                            text = state.text,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 480.dp)
                                .verticalScroll(changelogScroll)
                        )
                    }
                    is ChangelogUiState.Failed -> {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    else -> {}
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = if (LocalUseGradientBackground.current) Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
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
            // ── Appearance ───────────────────────────────────────────────────
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.Palette,
                    title = stringResource(R.string.settings_appearance_section)
                )
                Spacer(Modifier.height(8.dp))
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
                ThemeColorSection(
                    colorSource = preferences.colorSource,
                    themePaletteStyle = preferences.themePaletteStyle,
                    onColorSource = { source ->
                        playTap()
                        viewModel.setColorSource(source)
                    },
                    onPaletteStyle = { style ->
                        playTap()
                        viewModel.setThemePaletteStyle(style)
                    }
                )
                if (preferences.colorSource == AppColorSource.MATERIAL_YOU && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.settings_material_you_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(12.dp))
                GroupedListColumn {
                    GroupedListItem(position = GroupPosition.FIRST) {
                        SettingsToggleItem(
                            title = stringResource(R.string.settings_gradient_background),
                            subtitle = stringResource(R.string.settings_gradient_background_desc),
                            checked = preferences.useGradientBackground,
                            onCheckedChange = { enabled ->
                                playTap()
                                viewModel.setUseGradientBackground(enabled)
                            }
                        )
                    }
                    GroupedListItem(position = GroupPosition.LAST) {
                        SettingsToggleItem(
                            icon = Icons.Default.BlurOn,
                            title = stringResource(R.string.settings_progressive_blur),
                            subtitle = stringResource(R.string.settings_progressive_blur_desc),
                            checked = preferences.progressiveBlurEnabled,
                            onCheckedChange = { enabled ->
                                playTap()
                                viewModel.setProgressiveBlurEnabled(enabled)
                            }
                        )
                    }
                }
            }

            // ── Touch & Sound ────────────────────────────────────────────────
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.Vibration,
                    title = stringResource(R.string.settings_touch_sound_section)
                )
                Spacer(Modifier.height(8.dp))
                GroupedListColumn {
                    GroupedListItem(position = GroupPosition.FIRST) {
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
                    }
                    GroupedListItem(position = GroupPosition.LAST) {
                        ListItem(
                            headlineContent = {
                                Text(
                                    stringResource(R.string.settings_notifications),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            },
                            supportingContent = {
                                Text(
                                    stringResource(R.string.settings_notifications_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingContent = {
                                Icon(
                                    Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            trailingContent = {
                                Switch(
                                    checked = notificationsGranted,
                                    onCheckedChange = { wantEnabled ->
                                        playTap()
                                        when {
                                            wantEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            wantEnabled && !NotificationManagerCompat.from(context).areNotificationsEnabled() ->
                                                viewModel.openAppNotificationSettings()
                                            !wantEnabled ->
                                                viewModel.openAppNotificationSettings()
                                        }
                                    }
                                )
                            },
                            modifier = Modifier.clickable {
                                playTap()
                                if (!notificationsGranted) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                                        viewModel.openAppNotificationSettings()
                                    }
                                } else {
                                    viewModel.openAppNotificationSettings()
                                }
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            // ── Swipe Actions ─────────────────────────────────────────────────
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.SwipeLeft,
                    title = "Swipe actions"
                )
                Spacer(Modifier.height(8.dp))
                GroupedListColumn {
                    GroupedListItem(position = GroupPosition.FIRST) {
                        ListItem(
                            headlineContent = { Text("Swipe right \u2192", style = MaterialTheme.typography.bodyLarge) },
                            trailingContent = {
                                SwipeActionDropdown(
                                    current = preferences.swipeStartToEnd,
                                    excluded = preferences.swipeEndToStart,
                                    onSelect = { viewModel.setSwipeStartToEnd(it) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                    GroupedListItem(position = GroupPosition.LAST) {
                        ListItem(
                            headlineContent = { Text("\u2190 Swipe left", style = MaterialTheme.typography.bodyLarge) },
                            trailingContent = {
                                SwipeActionDropdown(
                                    current = preferences.swipeEndToStart,
                                    excluded = preferences.swipeStartToEnd,
                                    onSelect = { viewModel.setSwipeEndToStart(it) }
                                )
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            // ── History ───────────────────────────────────────────────────────
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.History,
                    title = stringResource(R.string.settings_history_section)
                )
                Spacer(Modifier.height(8.dp))
                GroupedListColumn {
                    GroupedListItem(position = GroupPosition.ONLY) {
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
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
            }

            // ── Import/Export ────────────────────────────────────────────────
            item {
                SettingsSectionHeader(
                    icon = Icons.Default.FolderOpen,
                    title = stringResource(R.string.settings_backup_section)
                )
                Spacer(Modifier.height(8.dp))

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

                GroupedListColumn {
                    GroupedListItem(position = GroupPosition.FIRST) {
                        ListItem(
                            headlineContent = { Text(folderLabel, style = MaterialTheme.typography.bodyLarge) },
                            supportingContent = {
                                Text(
                                    stringResource(R.string.settings_export_folder_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
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
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                    GroupedListItem(position = GroupPosition.MIDDLE) {
                        SettingsToggleItem(
                            title = stringResource(R.string.settings_auto_export_on_change),
                            subtitle = stringResource(R.string.settings_auto_export_on_change_hint),
                            checked = preferences.autoExportOnRuleChange,
                            onCheckedChange = { enabled ->
                                playTap()
                                viewModel.setAutoExportOnChange(enabled)
                            }
                        )
                    }
                    GroupedListItem(position = GroupPosition.MIDDLE) {
                        SettingsToggleItem(
                            title = stringResource(R.string.settings_scheduled_export),
                            subtitle = stringResource(R.string.settings_scheduled_export_hint),
                            checked = preferences.scheduledExportEnabled,
                            onCheckedChange = { enabled ->
                                playTap()
                                viewModel.setScheduledExportEnabled(enabled)
                            }
                        )
                    }
                    GroupedListItem(position = GroupPosition.LAST) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { playTap(); importLauncher.launch("application/json") },
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.settings_import_rules)) }
                            OutlinedButton(
                                onClick = { playTap(); viewModel.exportNow() },
                                modifier = Modifier.weight(1f)
                            ) { Text(stringResource(R.string.settings_export_now)) }
                        }
                    }
                }
            }

            // ── Updates (github flavor only) ──────────────────────────────────
            if (BuildConfig.SHOW_UPDATES) {
                item {
                    Column {
                        SettingsSectionHeader(
                            icon = Icons.Default.SystemUpdate,
                            title = stringResource(R.string.settings_updates_section)
                        )
                        Spacer(Modifier.height(8.dp))
                        GroupedListColumn {
                            GroupedListItem(position = GroupPosition.FIRST) {
                                SettingsToggleItem(
                                    title = stringResource(R.string.settings_auto_check_updates),
                                    subtitle = stringResource(R.string.settings_auto_check_updates_desc),
                                    checked = preferences.autoCheckForUpdates,
                                    onCheckedChange = { enabled ->
                                        playTap()
                                        viewModel.setAutoCheckForUpdates(enabled)
                                    }
                                )
                            }
                            GroupedListItem(position = GroupPosition.MIDDLE) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            stringResource(R.string.settings_see_whats_new),
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    },
                                    leadingContent = {
                                        Icon(
                                            Icons.Default.NewReleases,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    },
                                    trailingContent = {
                                        Icon(Icons.Default.ChevronRight, contentDescription = null)
                                    },
                                    modifier = Modifier.clickable {
                                        playTap()
                                        viewModel.openChangelogSheet()
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                            if (updateInfo != null) {
                                GroupedListItem(position = GroupPosition.MIDDLE) {
                                    ListItem(
                                        headlineContent = {
                                            Text(
                                                stringResource(
                                                    R.string.settings_update_available,
                                                    updateInfo!!.versionName
                                                ),
                                                style = MaterialTheme.typography.bodyLarge
                                            )
                                        },
                                        supportingContent = if (updateInfo!!.releaseNotes.isNotBlank()) {
                                            {
                                                Text(
                                                    updateInfo!!.releaseNotes,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 4,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        } else null,
                                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                    )
                                }
                            }
                            GroupedListItem(position = GroupPosition.LAST) {
                                UpdateMorphingButtonRow(
                                    updateInfo = updateInfo,
                                    isCheckingUpdate = isCheckingUpdate,
                                    downloadProgress = downloadProgress,
                                    onCheckClick = {
                                        playTap()
                                        viewModel.checkForUpdate()
                                    },
                                    onDownloadClick = { info ->
                                        playTap()
                                        viewModel.downloadAndInstall(info.downloadUrl)
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // ── About ─────────────────────────────────────────────────────────
            item {
                val context = LocalContext.current
                val githubRepo = BuildConfig.GITHUB_REPO.trim()
                Column(modifier = Modifier.padding(top = 24.dp)) {
                    SettingsSectionHeader(
                        icon = Icons.Default.Info,
                        title = stringResource(R.string.settings_about_section)
                    )
                    Spacer(Modifier.height(8.dp))
                    GroupedListColumn {
                        GroupedListItem(position = GroupPosition.ONLY) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${stringResource(R.string.app_name)} v${BuildConfig.VERSION_NAME}",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(10.dp))
                                Text(
                                    text = stringResource(R.string.settings_about_tagline),
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(20.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AppIconImage(
                                        modifier = Modifier
                                            .size(84.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                            .clickable {
                                                playTap()
                                                onOpenIntro()
                                            }
                                    )
                                    Spacer(Modifier.width(20.dp))
                                    AboutAuthorPhoto(
                                        modifier = Modifier
                                            .size(84.dp)
                                            .clip(RoundedCornerShape(16.dp))
                                    )
                                }
                                Spacer(Modifier.height(20.dp))
                                Text(
                                    text = stringResource(R.string.settings_byline),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                if (githubRepo.isNotEmpty()) {
                                    Spacer(Modifier.height(16.dp))
                                    OutlinedButton(
                                        onClick = {
                                            playTap()
                                            val url = "https://github.com/$githubRepo"
                                            runCatching {
                                                context.startActivity(
                                                    Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                )
                                            }
                                        },
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_github_mark),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(stringResource(R.string.settings_view_on_github))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun UpdateMorphingButtonRow(
    updateInfo: UpdateInfo?,
    isCheckingUpdate: Boolean,
    downloadProgress: Float?,
    onCheckClick: () -> Unit,
    onDownloadClick: (UpdateInfo) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val buttonHeight = 48.dp
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        when {
            downloadProgress != null -> {
                val progressValue = downloadProgress
                val label = when {
                    progressValue == -1f -> stringResource(R.string.settings_installing)
                    progressValue == -2f -> stringResource(R.string.settings_downloading)
                    else -> stringResource(
                        R.string.settings_downloading_percent,
                        progressValue.toInt().coerceIn(0, 100)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight)
                        .clip(shape)
                ) {
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(scheme.onSurface.copy(alpha = 0.12f))
                    )
                    when {
                        progressValue >= 0f && progressValue <= 100f -> {
                            Box(
                                Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((progressValue / 100f).coerceIn(0f, 1f))
                                    .align(Alignment.CenterStart)
                                    .background(scheme.primary.copy(alpha = 0.85f))
                            )
                        }
                        progressValue == -1f || progressValue == -2f -> {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .background(scheme.primary.copy(alpha = 0.22f))
                            )
                        }
                    }
                    if (progressValue == -1f || progressValue == -2f) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .height(4.dp),
                            color = scheme.primary.copy(alpha = 0.48f),
                            trackColor = Color.Transparent
                        )
                    }
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurface.copy(alpha = 0.78f),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            isCheckingUpdate -> {
                OutlinedButton(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    shape = shape
                ) {
                    LoadingIndicator(modifier = Modifier.size(24.dp))
                }
            }
            updateInfo != null -> {
                val availableUpdate = updateInfo
                Button(
                    onClick = { onDownloadClick(availableUpdate) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    shape = shape
                ) {
                    Text(
                        text = stringResource(
                            R.string.settings_download_install,
                            availableUpdate.versionName
                        ),
                        maxLines = 1
                    )
                }
            }
            else -> {
                OutlinedButton(
                    onClick = onCheckClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(buttonHeight),
                    shape = shape
                ) {
                    Text(stringResource(R.string.settings_check_for_updates))
                }
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
            {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
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
            containerColor = Color.Transparent
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
