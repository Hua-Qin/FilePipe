package dev.bikram.filepipe.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bikram.filepipe.R
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RunProgress
import dev.bikram.filepipe.domain.model.ScheduleType
import dev.bikram.filepipe.ui.feedback.rememberPlayTapSound
import dev.bikram.filepipe.ui.feedback.tapSoundCombinedClickable

private val CardShape = RoundedCornerShape(16.dp)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun RuleCard(
    rule: Rule,
    isSelected: Boolean,
    isExpanded: Boolean,
    progress: RunProgress?,
    onClick: () -> Unit,          // toggles expansion (or selection when in selection mode)
    onLongClick: () -> Unit,      // toggles selection
    cardActions: List<Pair<ImageVector, () -> Unit>>, // non-swipe action icons shown in card
    onToggleEnabled: (Boolean) -> Unit,
    onRunClick: () -> Unit,
    isAnyRuleRunning: Boolean,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CardShape)
                else Modifier
            ),
        shape = CardShape,
        colors = CardDefaults.elevatedCardColors()
    ) {
        AnimatedContent(
            targetState = isExpanded,
            transitionSpec = {
                (expandVertically() + fadeIn()) togetherWith (shrinkVertically() + fadeOut())
            },
            label = "card_expansion"
        ) { expanded ->
            if (expanded) {
                ExpandedContent(
                    rule = rule,
                    progress = progress,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    cardActions = cardActions,
                    onToggleEnabled = onToggleEnabled,
                    onRunClick = onRunClick,
                    isAnyRuleRunning = isAnyRuleRunning
                )
            } else {
                CompactContent(
                    rule = rule,
                    progress = progress,
                    onClick = onClick,
                    onLongClick = onLongClick,
                    onToggleEnabled = onToggleEnabled,
                    onRunClick = onRunClick,
                    isAnyRuleRunning = isAnyRuleRunning
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CompactContent(
    rule: Rule,
    progress: RunProgress?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onRunClick: () -> Unit,
    isAnyRuleRunning: Boolean
) {
    val playTap = rememberPlayTapSound()
    val runInProgress = progress != null && !progress.isComplete
    val runBlocked = isAnyRuleRunning && progress == null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .tapSoundCombinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Switch(
                checked = rule.isEnabled,
                onCheckedChange = { enabled ->
                    playTap()
                    onToggleEnabled(enabled)
                },
                modifier = Modifier.height(24.dp)
            )
            Icon(
                imageVector = rule.icon.toImageVector(),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = rule.name,
                style = MaterialTheme.typography.titleLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            FilledTonalIconButton(
                onClick = {
                    playTap()
                    onRunClick()
                },
                enabled = rule.isEnabled && !runInProgress && !runBlocked,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.run_now), modifier = Modifier.size(18.dp))
            }
        }
        if (rule.fileExtensions.isNotEmpty() || rule.sourceFolderPaths.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            val typesText = rule.fileExtensions.take(4).joinToString(" · ") +
                if (rule.fileExtensions.size > 4) " +${rule.fileExtensions.size - 4}" else ""
            val destText = displayPath(rule.destinationFolderPath).takeIf { it.isNotBlank() } ?: ""
            val infoText = listOf(typesText, destText).filter { it.isNotBlank() }.joinToString("  |  ")
            if (infoText.isNotBlank()) {
                Text(
                    text = infoText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (progress != null && !progress.isComplete) {
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { if (progress.totalFiles > 0) progress.progress else 0f },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ExpandedContent(
    rule: Rule,
    progress: RunProgress?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    cardActions: List<Pair<ImageVector, () -> Unit>>,
    onToggleEnabled: (Boolean) -> Unit,
    onRunClick: () -> Unit,
    isAnyRuleRunning: Boolean
) {
    val playTap = rememberPlayTapSound()
    Column(Modifier.padding(16.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .tapSoundCombinedClickable(onClick = onClick, onLongClick = onLongClick),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = rule.icon.toImageVector(),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = rule.name,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }
            Switch(
                checked = rule.isEnabled,
                onCheckedChange = { enabled ->
                    playTap()
                    onToggleEnabled(enabled)
                },
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .tapSoundCombinedClickable(onClick = onClick, onLongClick = onLongClick)
        ) {
            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.rule_card_types),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            if (rule.fileExtensions.isEmpty()) {
                Text(
                    text = stringResource(R.string.rule_card_types_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rule.fileExtensions.forEach { extension ->
                        FilterChip(
                            selected = true,
                            onClick = { playTap() },
                            label = { Text(extension, style = MaterialTheme.typography.bodyMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = true,
                                borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0f)
                            )
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.rule_card_from),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(4.dp))
            if (rule.sourceFolderPaths.isEmpty()) {
                Text(
                    text = stringResource(R.string.rule_card_from_none),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    rule.sourceFolderPaths.forEach { path ->
                        Text(
                            text = displayPath(path),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            val notSet = stringResource(R.string.rule_card_destination_not_set)
            LabeledInfo(
                label = stringResource(R.string.destination_label),
                value = if (rule.destinationFolderPath.isEmpty()) notSet
                        else displayPath(rule.destinationFolderPath)
            )

            rule.schedule?.let { schedule ->
                Spacer(Modifier.height(4.dp))
                val scheduleText = when (schedule.type) {
                    ScheduleType.DAILY -> "Daily at %02d:%02d".format(schedule.hour, schedule.minute)
                    ScheduleType.WEEKLY -> {
                        val days = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                        val dayName = schedule.dayOfWeek?.let { days.getOrNull(it - 2) } ?: "?"
                        "Weekly $dayName at %02d:%02d".format(schedule.hour, schedule.minute)
                    }
                }
                LabeledInfo(label = stringResource(R.string.schedule_label), value = scheduleText)
            }

            AnimatedVisibility(
                visible = progress != null,
                enter = expandVertically() + fadeIn()
            ) {
                progress?.let { runProgress ->
                    Column(
                        modifier = Modifier.padding(top = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (runProgress.isComplete) {
                            val summary = when {
                                runProgress.error != null -> "Error: ${runProgress.error}"
                                runProgress.totalFiles == 0 -> "No matching files found"
                                else -> "${runProgress.filesMoved} / ${runProgress.totalFiles} files moved"
                            }
                            Text(
                                text = summary,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (runProgress.error != null) MaterialTheme.colorScheme.error
                                        else MaterialTheme.colorScheme.primary
                            )
                            LinearProgressIndicator(progress = { 1f }, modifier = Modifier.fillMaxWidth())
                        } else if (runProgress.totalFiles > 0) {
                            val animatedProgress by animateFloatAsState(
                                targetValue = runProgress.progress,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                label = "progress"
                            )
                            Text(
                                text = "Moving ${runProgress.currentFileName.ifBlank { "…" }} (${runProgress.filesMoved + 1} / ${runProgress.totalFiles})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth())
                        } else {
                            Text("Scanning…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                cardActions.forEach { (icon, onActionClick) ->
                    IconButton(onClick = {
                        playTap()
                        onActionClick()
                    }) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            val runInProgress = progress != null && !progress.isComplete
            val runBlocked = isAnyRuleRunning && progress == null
            FilledTonalButton(
                onClick = {
                    playTap()
                    onRunClick()
                },
                enabled = rule.isEnabled && !runInProgress && !runBlocked,
                shape = RoundedCornerShape(50)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text(text = stringResource(R.string.run_now))
            }
        }
    }
}

@Composable
private fun LabeledInfo(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}
