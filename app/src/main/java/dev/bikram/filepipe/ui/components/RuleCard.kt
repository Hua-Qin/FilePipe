package dev.bikram.filepipe.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bikram.filepipe.R
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RunProgress
import dev.bikram.filepipe.domain.model.ScheduleType
import java.io.File

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun RuleCard(
    rule: Rule,
    isSelected: Boolean,
    progress: RunProgress?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onRunClick: () -> Unit,
    isAnyRuleRunning: Boolean,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(12.dp)
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = rule.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .weight(1f)
                        .combinedClickable(onClick = onClick, onLongClick = onLongClick),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Switch(
                    checked = rule.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            ) {
                Spacer(Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.rule_card_types),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                if (rule.fileExtensions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.rule_card_types_none),
                        style = MaterialTheme.typography.bodySmall,
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
                                onClick = {},
                                label = { Text(extension) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimaryContainer
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
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(4.dp))
                if (rule.sourceFolderUris.isEmpty()) {
                    Text(
                        text = stringResource(R.string.rule_card_from_none),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        rule.sourceFolderUris.forEach { uriString ->
                            Text(
                                text = uriToDisplayName(uriString).ifEmpty { uriString },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                LabeledInfo(
                    label = stringResource(R.string.destination_label),
                    value = uriToDisplayName(rule.destinationFolderUri)
                        .ifEmpty { stringResource(R.string.rule_card_destination_not_set) }
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
                            if (!runProgress.isComplete) {
                                Text(
                                    text = "Running…",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (runProgress.isComplete) {
                                val summary = when {
                                    runProgress.error != null -> "Error: ${runProgress.error}"
                                    runProgress.totalFiles == 0 -> "No matching files found"
                                    else -> "${runProgress.filesMoved} / ${runProgress.totalFiles} files moved"
                                }
                                Text(
                                    text = summary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (runProgress.error != null) {
                                        MaterialTheme.colorScheme.error
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    }
                                )
                                LinearProgressIndicator(
                                    progress = { 1f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                if (runProgress.currentFileName.isNotBlank()) {
                                    Text(
                                        text = runProgress.currentFileName,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { runProgress.progress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                val runInProgressForThisRule = progress != null && !progress.isComplete
                val runBlockedByAnotherRule = isAnyRuleRunning && progress == null
                FilledTonalButton(
                    onClick = onRunClick,
                    enabled = rule.isEnabled && !runInProgressForThisRule && !runBlockedByAnotherRule,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(text = stringResource(R.string.run_now))
                }
            }
        }
    }
}

@Composable
private fun LabeledInfo(label: String, value: String) {
    Row {
        Text(
            text = "$label: ",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** Extract a readable path segment from a content URI string. */
fun uriToDisplayName(uriString: String): String {
    if (uriString.isBlank()) return ""
    return try {
        val decoded = java.net.URLDecoder.decode(uriString, "UTF-8")
        val colonIdx = decoded.lastIndexOf(':')
        if (colonIdx >= 0) {
            "/" + decoded.substring(colonIdx + 1)
        } else {
            File(uriString).name
        }
    } catch (_: Exception) {
        uriString.takeLast(40)
    }
}
