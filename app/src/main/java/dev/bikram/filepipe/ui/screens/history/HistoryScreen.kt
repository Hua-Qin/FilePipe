package dev.bikram.filepipe.ui.screens.history

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.bikram.filepipe.R
import dev.bikram.filepipe.domain.model.RunHistory
import dev.bikram.filepipe.ui.components.HistoryCard
import dev.bikram.filepipe.ui.feedback.LocalHapticEnabled
import dev.bikram.filepipe.ui.feedback.rememberPlayTapSound
import dev.bikram.filepipe.ui.components.DeliberateSwipeRevealCard
import dev.bikram.filepipe.ui.components.SwipeDismissCardDefaults
import dev.bikram.filepipe.ui.modifiers.applyToScrollableList
import dev.bikram.filepipe.ui.theme.LocalProgressiveBlurStyle
import dev.bikram.filepipe.ui.theme.LocalUseGradientBackground

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    contentPadding: PaddingValues = PaddingValues(),
    onHistoryClick: (Long) -> Unit,
    onNavigateBack: (() -> Unit)? = null,
    viewModel: HistoryViewModel = hiltViewModel()
) {
    val playTap = rememberPlayTapSound()
    val groups by viewModel.historyGroups.collectAsStateWithLifecycle()
    val userMessage by viewModel.userMessage.collectAsStateWithLifecycle()
    val topAppBarState = rememberTopAppBarState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(topAppBarState)
    var showClearConfirm by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBlurModifier = LocalProgressiveBlurStyle.current?.applyToScrollableList() ?: Modifier
    val isFiltered = viewModel.filterRuleId != null

    LaunchedEffect(userMessage) {
        val msg = userMessage ?: return@LaunchedEffect
        viewModel.clearUserMessage()
        snackbarHostState.showSnackbar(msg)
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text(stringResource(R.string.history_clear_confirm_title)) },
            text = { Text(stringResource(R.string.history_clear_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        playTap()
                        showClearConfirm = false
                        viewModel.clearAllHistory()
                    }
                ) {
                    Text(stringResource(R.string.history_clear))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    playTap()
                    showClearConfirm = false
                }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = if (LocalUseGradientBackground.current) Color.Transparent else MaterialTheme.colorScheme.background,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.history_title)) },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                ),
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = {
                            playTap()
                            onNavigateBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        if (groups.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .then(scrollBlurModifier)
                    .padding(innerPadding)
                    .padding(bottom = contentPadding.calculateBottomPadding())
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = null,
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.outlineVariant
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.history_empty_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.history_empty_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center
                )
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(scrollBlurModifier),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = innerPadding.calculateTopPadding() + 8.dp,
                bottom = innerPadding.calculateBottomPadding() + contentPadding.calculateBottomPadding()
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isFiltered) {
                item(key = "filter_header") {
                    Text(
                        text = "Showing runs for this rule only",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            groups.forEach { group ->
                item(key = "header_${group.dateLabel}") {
                    Text(
                        text = group.dateLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(group.entries, key = { "history_${it.id}" }) { history ->
                    SwipeToDismissHistoryCard(
                        history = history,
                        onClick = {
                            playTap()
                            onHistoryClick(history.id)
                        },
                        onDelete = { viewModel.deleteHistoryEntry(history.id) },
                        modifier = Modifier.animateItem()
                    )
                }
            }
        }
    }
}

@Composable
private fun SwipeToDismissHistoryCard(
    history: RunHistory,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticEnabled = LocalHapticEnabled.current
    val cardShape = RoundedCornerShape(12.dp)
    DeliberateSwipeRevealCard(
        commitThresholdFraction = SwipeDismissCardDefaults.CommitThresholdFraction,
        cardShape = cardShape,
        onSwipeStartToEnd = { },
        onSwipeEndToStart = onDelete,
        hapticEnabled = hapticEnabled,
        allowSwipeStartToEnd = false,
        allowSwipeEndToStart = true,
        backgroundContent = { fromStart ->
            if (!fromStart) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.32f),
                            cardShape
                        )
                        .padding(end = 24.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        modifier = modifier
    ) {
        HistoryCard(history = history, onClick = onClick)
    }
}
