package dev.bikram.filepipe.ui.screens.rules

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bikram.filepipe.R
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.ui.components.RuleCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    contentPadding: PaddingValues,
    onCreateRule: () -> Unit,
    onEditRule: (Long) -> Unit,
    onNavigateToHistoryDetail: (Long) -> Unit,
    onNavigateToHistoryList: () -> Unit,
    viewModel: RulesViewModel = hiltViewModel()
) {
    val rules by viewModel.rules.collectAsStateWithLifecycle()
    val selectedRuleIds by viewModel.selectedRuleIds.collectAsStateWithLifecycle()
    val progressMap by viewModel.progressMap.collectAsStateWithLifecycle()
    val isRunning by viewModel.isRunning.collectAsStateWithLifecycle()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(topAppBarState)
    var pendingDeleteRule by remember { mutableStateOf<Rule?>(null) }

    val hasSelection = selectedRuleIds.isNotEmpty()

    BackHandler(enabled = hasSelection) {
        viewModel.clearSelection()
    }

    LaunchedEffect(viewModel) {
        viewModel.navigateAfterRun.collect { target ->
            when (target) {
                is RulesRunNavigation.HistoryDetail -> onNavigateToHistoryDetail(target.historyId)
                is RulesRunNavigation.HistoryList -> onNavigateToHistoryList()
            }
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Rules") },
                scrollBehavior = scrollBehavior,
                actions = {
                    if (hasSelection && !isRunning) {
                        TextButton(onClick = viewModel::clearSelection) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (!hasSelection) {
                ExtendedFloatingActionButton(
                    onClick = onCreateRule,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding())
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(
                        text = stringResource(R.string.rules_add_rule),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        },
        bottomBar = {
            if (hasSelection || isRunning) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 16.dp,
                            end = 16.dp,
                            bottom = contentPadding.calculateBottomPadding() + 8.dp
                        )
                ) {
                    if (isRunning) {
                        Text(
                            text = stringResource(R.string.run_in_progress),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        val enabledSelectedCount = rules.count { it.id in selectedRuleIds && it.isEnabled }
                        Button(
                            onClick = viewModel::runSelected,
                            enabled = enabledSelectedCount > 0,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Text(text = "  ${stringResource(R.string.run_button, enabledSelectedCount)}")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (rules.isEmpty()) {
            EmptyState(modifier = Modifier.padding(innerPadding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + contentPadding.calculateBottomPadding() + 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(rules, key = { it.id }) { rule ->
                    SwipeToDismissRuleCard(
                        rule = rule,
                        isSelected = rule.id in selectedRuleIds,
                        progress = progressMap[rule.id],
                        isAnyRuleRunning = isRunning,
                        onToggleEnabled = { enabled -> viewModel.toggleEnabled(rule, enabled) },
                        onToggleSelectOrEdit = {
                            if (hasSelection) {
                                viewModel.toggleSelection(rule.id)
                            } else {
                                onEditRule(rule.id)
                            }
                        },
                        onLongClick = { viewModel.toggleSelection(rule.id) },
                        onDelete = { pendingDeleteRule = rule },
                        onRunRule = { viewModel.runRule(rule) }
                    )
                }
            }
        }
    }

    pendingDeleteRule?.let { rule ->
        AlertDialog(
            onDismissRequest = { pendingDeleteRule = null },
            title = { Text("Delete rule?") },
            text = { Text("\"${rule.name}\" and its schedule will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteRule(rule)
                    pendingDeleteRule = null
                }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteRule = null }) { Text("Cancel") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToDismissRuleCard(
    rule: Rule,
    isSelected: Boolean,
    progress: dev.bikram.filepipe.domain.model.RunProgress?,
    isAnyRuleRunning: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleSelectOrEdit: () -> Unit,
    onLongClick: () -> Unit,
    onDelete: () -> Unit,
    onRunRule: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                false
            } else false
        }
    )

    val swipeBackground = MaterialTheme.colorScheme.error.copy(alpha = 0.32f)
    val iconTint = MaterialTheme.colorScheme.error

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                Modifier
                    .fillMaxHeight()
                    .weight(1f)
                    .background(swipeBackground, RoundedCornerShape(12.dp))
                    .padding(end = 24.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = iconTint,
                    modifier = Modifier.size(32.dp)
                )
            }
        },
        enableDismissFromStartToEnd = false,
        modifier = Modifier.animateContentSize()
    ) {
        RuleCard(
            rule = rule,
            isSelected = isSelected,
            progress = progress,
            onClick = onToggleSelectOrEdit,
            onLongClick = onLongClick,
            onToggleEnabled = onToggleEnabled,
            onRunClick = onRunRule,
            isAnyRuleRunning = isAnyRuleRunning
        )
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.Info,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.rules_empty_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.rules_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outlineVariant,
            textAlign = TextAlign.Center
        )
    }
}
