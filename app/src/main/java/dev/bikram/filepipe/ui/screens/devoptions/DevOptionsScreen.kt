package dev.bikram.filepipe.ui.screens.devoptions

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bikram.filepipe.R
import dev.bikram.filepipe.diagnostics.DiagnosticLog
import dev.bikram.filepipe.ui.common.FilePipeMaterialRoundedSymbol
import dev.bikram.filepipe.ui.components.FilePipeButton
import dev.bikram.filepipe.ui.components.FilePipeIconButton
import dev.bikram.filepipe.ui.components.FilePipeTextButton
import dev.bikram.filepipe.ui.components.containers.GroupPosition
import dev.bikram.filepipe.ui.components.containers.GroupedListColumn
import dev.bikram.filepipe.ui.components.containers.GroupedListItem
import dev.bikram.filepipe.ui.feedback.tapSoundClickable
import dev.bikram.filepipe.ui.modifiers.applyToScrollableList
import dev.bikram.filepipe.ui.navigation.DEV_OPTIONS_SHARED_BOUNDS_KEY
import dev.bikram.filepipe.ui.navigation.LocalNavAnimatedVisibilityScope
import dev.bikram.filepipe.ui.navigation.LocalSharedTransitionScope
import dev.bikram.filepipe.ui.screens.settings.SettingsViewModel
import dev.bikram.filepipe.ui.theme.LocalProgressiveBlurStyle
import dev.bikram.filepipe.ui.theme.gradientOverlayTopAppBarColors
import dev.bikram.filepipe.ui.theme.reducedMotionAwareSpec

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalSharedTransitionApi::class,
)
@Composable
fun DevOptionsScreen(
    contentPadding: PaddingValues,
    onNavigateBack: () -> Unit,
    settingsViewModel: SettingsViewModel,
    viewModel: DevOptionsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBlurModifier = LocalProgressiveBlurStyle.current?.applyToScrollableList() ?: Modifier
    val lazyListState =
        rememberLazyListState(
            initialFirstVisibleItemIndex = DevOptionsScreenSessionState.firstVisibleItemIndex,
            initialFirstVisibleItemScrollOffset = DevOptionsScreenSessionState.firstVisibleItemScrollOffset,
        )
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    val sharedTransitionScope = LocalSharedTransitionScope.current
    val animatedVisibilityScope = LocalNavAnimatedVisibilityScope.current
    val sharedBoundsModifier =
        if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedBounds(
                    sharedContentState = rememberSharedContentState(DEV_OPTIONS_SHARED_BOUNDS_KEY),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        } else {
            Modifier
        }
    var pendingDialog by remember { mutableStateOf<DevOptionsDialog?>(null) }
    var infoCollapsed by remember { mutableStateOf(DevOptionsScreenSessionState.infoCollapsed) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    LaunchedEffect(lazyListState) {
        snapshotFlow {
            lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
        }.collect { (index, offset) ->
            DevOptionsScreenSessionState.firstVisibleItemIndex = index
            DevOptionsScreenSessionState.firstVisibleItemScrollOffset = offset
        }
    }

    pendingDialog?.let { dialog ->
        AlertDialog(
            onDismissRequest = { pendingDialog = null },
            title = { Text(dialog.title) },
            text = { Text(dialog.message) },
            confirmButton = {
                FilePipeButton(
                    onClick = {
                        pendingDialog = null
                        dialog.onConfirm()
                    },
                ) {
                    Text(dialog.confirmLabel)
                }
            },
            dismissButton = {
                FilePipeTextButton(onClick = { pendingDialog = null }) {
                    Text(stringResource(R.string.cancel))
                }
            },
        )
    }

    Surface(
        modifier =
            Modifier
                .fillMaxSize()
                .then(sharedBoundsModifier),
        color = Color.Transparent,
    ) {
        Scaffold(
            modifier =
                Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                MediumTopAppBar(
                    title = {
                        Text(
                            text = stringResource(R.string.dev_options_title),
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    navigationIcon = {
                        FilePipeIconButton(onClick = onNavigateBack) {
                            FilePipeMaterialRoundedSymbol(
                                name = "arrow_back",
                                contentDescription = stringResource(R.string.dev_options_back),
                                autoMirror = true,
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior,
                    colors = gradientOverlayTopAppBarColors(),
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
                )
            },
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets.statusBars.only(WindowInsetsSides.Top),
        ) { scaffoldPadding ->
            LazyColumn(
                state = lazyListState,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .then(scrollBlurModifier)
                        .consumeWindowInsets(scaffoldPadding),
                contentPadding =
                    PaddingValues(
                        start = 16.dp,
                        top = scaffoldPadding.calculateTopPadding() + 8.dp,
                        end = 16.dp,
                        bottom = contentPadding.calculateBottomPadding() + 24.dp,
                    ),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                item(key = "developer_mode") {
                    GroupedListColumn(modifier = Modifier.fillMaxWidth()) {
                        GroupedListItem(position = GroupPosition.ONLY) {
                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.dev_options_mode_label),
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    Text(
                                        text = stringResource(R.string.dev_options_mode_subtitle),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(
                                    checked = state.developerOptionsEnabled,
                                    onCheckedChange = { enabled ->
                                        viewModel.setDeveloperOptionsEnabled(enabled)
                                        if (!enabled) onNavigateBack()
                                    },
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }

                item(key = "warning") {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.large,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            FilePipeMaterialRoundedSymbol(
                                name = "warning",
                                size = 18.dp,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                            )
                            Text(
                                text = stringResource(R.string.dev_options_warning),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                item(key = "info") {
                    DevCollapsibleInfoSection(
                        title = stringResource(R.string.dev_options_section_info),
                        collapsed = infoCollapsed,
                        onToggleCollapsed = {
                            infoCollapsed = !infoCollapsed
                            DevOptionsScreenSessionState.infoCollapsed = infoCollapsed
                        },
                    ) {
                        DevInfoCard(
                            title = stringResource(R.string.dev_options_section_overview),
                            rows = state.overview,
                        )
                        Spacer(Modifier.height(8.dp))
                        DevInfoCard(
                            title = stringResource(R.string.dev_options_section_permissions_storage),
                            rows = state.permissionsAndStorage,
                        )
                        Spacer(Modifier.height(8.dp))
                        DevInfoCard(
                            title = stringResource(R.string.dev_options_section_database),
                            rows = state.database,
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }

                item(key = "settings") {
                    DevActionSection(
                        iconName = "settings",
                        title = stringResource(R.string.dev_options_section_settings),
                        actions =
                            listOf(
                                DevAction(stringResource(R.string.dev_options_action_open_app_details)) { viewModel.openAppDetails() },
                                DevAction(stringResource(R.string.dev_options_action_notification_settings)) { viewModel.openNotificationSettings() },
                                DevAction(stringResource(R.string.dev_options_action_manage_all_files_access)) { viewModel.openManageAllFilesAccessSettings() },
                                DevAction(stringResource(R.string.dev_options_action_battery_optimization)) { viewModel.openBatteryOptimizationSettings() },
                            ),
                    )
                    Spacer(Modifier.height(24.dp))
                }

                item(key = "mock_operations") {
                    DevActionSection(
                        iconName = "experiment",
                        title = stringResource(R.string.dev_options_section_mock_operations),
                        actions =
                            listOf(
                                DevAction(
                                    label = stringResource(R.string.dev_options_action_add_mock_large_file_move_rule),
                                    onClick = { viewModel.addMockLargeFileMoveRule() },
                                ),
                                DevAction(
                                    label = stringResource(R.string.dev_options_action_remove_mock_large_file_move_rule),
                                    onClick = { viewModel.removeMockLargeFileMoveRuleAndHistory() },
                                ),
                                DevAction(
                                    label = stringResource(R.string.dev_options_action_arm_update_promo),
                                    enabled = state.updateMocksAvailable && state.showUpdates,
                                    onClick = {
                                        settingsViewModel.devReleaseMockArmRulesUpdatePromoForRulesTab()
                                        onNavigateBack()
                                    },
                                ),
                                DevAction(
                                    label = stringResource(R.string.dev_options_action_start_play_banner),
                                    enabled = state.updateMocksAvailable,
                                    onClick = {
                                        settingsViewModel.devReleaseMockStartPlayUpdateBannerSequence()
                                        onNavigateBack()
                                    },
                                ),
                                DevAction(
                                    label = stringResource(R.string.dev_options_action_post_mock_update_notification),
                                    enabled = state.showUpdates,
                                    onClick = {
                                        settingsViewModel.devReleaseMockArmRulesUpdatePromoForRulesTab()
                                        viewModel.postMockUpdateNotification()
                                    },
                                ),
                                DevAction(
                                    label = stringResource(R.string.dev_options_action_post_mock_file_operation_notification),
                                    onClick = { viewModel.postMockFileOperationNotification() },
                                ),
                            ),
                    )
                    Spacer(Modifier.height(24.dp))
                }

                item(key = "workers") {
                    DevActionSection(
                        iconName = "work_history",
                        title = stringResource(R.string.dev_options_section_workers),
                        actions =
                            listOf(
                                DevAction(stringResource(R.string.dev_options_action_sync_scheduled_rules)) { viewModel.syncScheduledRules() },
                                DevAction(stringResource(R.string.dev_options_action_sync_update_checks)) { viewModel.syncUpdateCheckWorker() },
                                DevAction(stringResource(R.string.dev_options_action_sync_log_pruning)) { viewModel.syncLogPruneWorker() },
                            ),
                    )
                    Spacer(Modifier.height(24.dp))
                }

                item(key = "diagnostics") {
                    DevActionSection(
                        iconName = "bug_report",
                        title = stringResource(R.string.dev_options_section_diagnostics),
                        actions =
                            listOf(
                                DevAction(stringResource(R.string.dev_options_action_force_crash)) {
                                    pendingDialog =
                                        DevOptionsDialog(
                                            title = context.getString(R.string.dev_options_dialog_force_crash_title),
                                            message = context.getString(R.string.dev_options_dialog_force_crash_message),
                                            confirmLabel = context.getString(R.string.dev_options_dialog_force_crash_confirm),
                                            onConfirm = { viewModel.forceCrash() },
                                        )
                                },
                                DevAction(stringResource(R.string.dev_options_action_copy_diagnostics)) {
                                    copyDiagnostics(context, state)
                                },
                                DevAction(stringResource(R.string.dev_options_action_share_diagnostics)) {
                                    shareDiagnostics(context, state)
                                },
                                DevAction(stringResource(R.string.dev_options_action_clear_diagnostics_log)) {
                                    viewModel.clearDiagnosticsLog()
                                },
                            ),
                    )
                    Spacer(Modifier.height(24.dp))
                }

                item(key = "reset_preferences") {
                    DevActionSection(
                        iconName = "restart_alt",
                        title = stringResource(R.string.dev_options_section_settings_reset),
                        actions =
                            listOf(
                                DevAction(stringResource(R.string.dev_options_action_reset_first_launch_flag)) {
                                    viewModel.resetFirstLaunchFlag()
                                },
                                DevAction(
                                    label = stringResource(R.string.dev_options_action_reset_github_ack),
                                    enabled = state.isGithubFlavor,
                                    onClick = { viewModel.resetSkippedGithubReleaseAck() },
                                ),
                                DevAction(
                                    label = stringResource(R.string.dev_options_action_clear_update_dedupe),
                                    enabled = state.showUpdates,
                                    onClick = { viewModel.clearUpdateNotificationDedupe() },
                                ),
                                DevAction(
                                    label = stringResource(R.string.dev_options_action_delete_cached_update_apk),
                                    enabled = state.isGithubFlavor,
                                    onClick = { viewModel.deleteCachedUpdateApk() },
                                ),
                            ),
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            pendingDialog =
                                DevOptionsDialog(
                                    title = context.getString(R.string.dev_options_dialog_reset_settings_title),
                                    message = context.getString(R.string.dev_options_dialog_reset_settings_message),
                                    confirmLabel = context.getString(R.string.dev_options_dialog_reset_settings_confirm),
                                    onConfirm = { viewModel.resetSettingsPreferences() },
                                )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Text(
                            text = stringResource(R.string.dev_options_action_reset_settings_preferences),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

private data class DevAction(
    val label: String,
    val enabled: Boolean = true,
    val onClick: () -> Unit,
)

private data class DevOptionsDialog(
    val title: String,
    val message: String,
    val confirmLabel: String,
    val onConfirm: () -> Unit,
)

private object DevOptionsScreenSessionState {
    var firstVisibleItemIndex = 0
    var firstVisibleItemScrollOffset = 0
    var infoCollapsed = true
}

@Composable
private fun DevCollapsibleInfoSection(
    title: String,
    collapsed: Boolean,
    onToggleCollapsed: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    val spatialSpec = reducedMotionAwareSpec(MaterialTheme.motionScheme.slowSpatialSpec<IntSize>())
    val fadeInSpec = reducedMotionAwareSpec(MaterialTheme.motionScheme.defaultEffectsSpec<Float>())
    val fadeOutSpec = reducedMotionAwareSpec(MaterialTheme.motionScheme.fastEffectsSpec<Float>())
    Column {
        DevExpandableSectionHeader(
            iconName = "info",
            title = title,
            collapsed = collapsed,
            onToggle = onToggleCollapsed,
        )
        AnimatedVisibility(
            visible = !collapsed,
            enter =
                expandVertically(
                    animationSpec = spatialSpec,
                    expandFrom = Alignment.Top,
                ) + fadeIn(fadeInSpec),
            exit =
                shrinkVertically(
                    animationSpec = spatialSpec,
                    shrinkTowards = Alignment.Top,
                ) + fadeOut(fadeOutSpec),
        ) {
            Column {
                Spacer(Modifier.height(8.dp))
                content()
            }
        }
    }
}

@Composable
private fun DevInfoCard(
    title: String,
    rows: List<DevOptionsInfoRow>,
) {
    GroupedListColumn(modifier = Modifier.fillMaxWidth()) {
        GroupedListItem(position = GroupPosition.ONLY) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                rows.forEach { row -> DevInfoRow(row) }
            }
        }
    }
}

@Composable
private fun DevActionSection(
    iconName: String,
    title: String,
    actions: List<DevAction>,
) {
    DevSectionHeader(iconName = iconName, title = title)
    Spacer(Modifier.height(8.dp))
    GroupedListColumn(modifier = Modifier.fillMaxWidth()) {
        actions.forEachIndexed { index, action ->
            GroupedListItem(position = groupPositionFor(index, actions.size)) {
                DevActionRow(action)
            }
        }
    }
}

@Composable
private fun DevInfoRow(row: DevOptionsInfoRow) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(row.labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.42f),
        )
        Text(
            text = row.value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(0.58f),
        )
    }
}

@Composable
private fun DevActionRow(action: DevAction) {
    ListItem(
        headlineContent = {
            Text(
                text = action.label,
                color =
                    if (action.enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
            )
        },
        modifier =
            if (action.enabled) {
                Modifier.tapSoundClickable(onClick = action.onClick)
            } else {
                Modifier
            },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

@Composable
private fun DevSectionHeader(
    iconName: String,
    title: String,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilePipeMaterialRoundedSymbol(
            name = iconName,
            contentDescription = null,
            size = 18.dp,
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun DevExpandableSectionHeader(
    iconName: String,
    title: String,
    collapsed: Boolean,
    onToggle: () -> Unit,
) {
    val rotation by animateFloatAsState(
        targetValue = if (collapsed) 0f else 90f,
        animationSpec = reducedMotionAwareSpec(MaterialTheme.motionScheme.defaultSpatialSpec<Float>()),
        label = "dev_options_section_chevron_rotation",
    )
    val cdExpand = stringResource(R.string.settings_section_expand_cd, title)
    val cdCollapse = stringResource(R.string.settings_section_collapse_cd, title)
    val interactionSource = remember { MutableInteractionSource() }
    val spatialSpec = reducedMotionAwareSpec(MaterialTheme.motionScheme.defaultSpatialSpec<Dp>())
    val colorSpec = reducedMotionAwareSpec(MaterialTheme.motionScheme.defaultEffectsSpec<Color>())
    val headerCorner by animateDpAsState(
        targetValue = if (collapsed) 28.dp else 4.dp,
        animationSpec = spatialSpec,
        label = "dev_options_section_header_corner",
    )
    val headerColor by animateColorAsState(
        targetValue = if (collapsed) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent,
        animationSpec = colorSpec,
        label = "dev_options_section_header_color",
    )
    val horizontalPadding by animateDpAsState(
        targetValue = if (collapsed) 12.dp else 0.dp,
        animationSpec = spatialSpec,
        label = "dev_options_section_header_horizontal_padding",
    )
    val verticalPadding by animateDpAsState(
        targetValue = if (collapsed) 8.dp else 4.dp,
        animationSpec = spatialSpec,
        label = "dev_options_section_header_vertical_padding",
    )
    val iconContainerSize by animateDpAsState(
        targetValue = if (collapsed) 36.dp else 20.dp,
        animationSpec = spatialSpec,
        label = "dev_options_section_icon_container_size",
    )
    val iconSize by animateDpAsState(
        targetValue = if (collapsed) 21.dp else 19.dp,
        animationSpec = spatialSpec,
        label = "dev_options_section_icon_size",
    )
    val iconContainerColor by animateColorAsState(
        targetValue =
            if (collapsed) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
            } else {
                Color.Transparent
            },
        animationSpec = colorSpec,
        label = "dev_options_section_icon_container_color",
    )
    val chevronContainerSize by animateDpAsState(
        targetValue = if (collapsed) 32.dp else 20.dp,
        animationSpec = spatialSpec,
        label = "dev_options_section_chevron_container_size",
    )
    val chevronSize by animateDpAsState(
        targetValue = if (collapsed) 20.dp else 18.dp,
        animationSpec = spatialSpec,
        label = "dev_options_section_chevron_size",
    )
    val chevronContainerColor by animateColorAsState(
        targetValue = if (collapsed) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent,
        animationSpec = colorSpec,
        label = "dev_options_section_chevron_container_color",
    )
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(headerCorner))
                .background(headerColor)
                .semantics { contentDescription = if (collapsed) cdExpand else cdCollapse }
                .tapSoundClickable(
                    onClick = onToggle,
                    indication = null,
                    interactionSource = interactionSource,
                ).padding(
                    horizontal = horizontalPadding.coerceAtLeast(0.dp),
                    vertical = verticalPadding.coerceAtLeast(0.dp),
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(iconContainerSize)
                    .clip(MaterialTheme.shapes.extraExtraLarge)
                    .background(iconContainerColor),
            contentAlignment = Alignment.Center,
        ) {
            FilePipeMaterialRoundedSymbol(
                name = iconName,
                contentDescription = null,
                size = iconSize,
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier =
                Modifier
                    .size(chevronContainerSize)
                    .clip(MaterialTheme.shapes.extraExtraLarge)
                    .background(chevronContainerColor),
            contentAlignment = Alignment.Center,
        ) {
            FilePipeMaterialRoundedSymbol(
                name = "chevron_right",
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = rotation },
                size = chevronSize,
                autoMirror = true,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun groupPositionFor(
    index: Int,
    count: Int,
): GroupPosition =
    when {
        count <= 1 -> GroupPosition.ONLY
        index == 0 -> GroupPosition.FIRST
        index == count - 1 -> GroupPosition.LAST
        else -> GroupPosition.MIDDLE
    }

private fun shareDiagnostics(
    context: Context,
    state: DevOptionsUiState,
) {
    runCatching {
        val diagnosticsFile = DiagnosticLog.createShareFile(context, state.preferences)
        val uri =
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                diagnosticsFile,
            )
        val sendIntent =
            Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.settings_share_diagnostics_subject))
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        context.startActivity(Intent.createChooser(sendIntent, context.getString(R.string.settings_share_diagnostics_chooser)))
    }.onFailure {
        Toast.makeText(context, context.getString(R.string.settings_share_diagnostics_failed), Toast.LENGTH_SHORT).show()
    }
}

private fun copyDiagnostics(
    context: Context,
    state: DevOptionsUiState,
) {
    runCatching {
        val diagnosticsFile = DiagnosticLog.createShareFile(context, state.preferences)
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(
            ClipData.newPlainText(
                context.getString(R.string.dev_options_diagnostics_clip_label),
                diagnosticsFile.readText(),
            ),
        )
        Toast.makeText(context, context.getString(R.string.dev_options_diagnostics_copied), Toast.LENGTH_SHORT).show()
    }.onFailure {
        Toast.makeText(context, context.getString(R.string.settings_share_diagnostics_failed), Toast.LENGTH_SHORT).show()
    }
}
