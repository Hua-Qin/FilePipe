package dev.bikram.filepipe.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.bikram.filepipe.R
import dev.bikram.filepipe.domain.model.OperationMode
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RuleIcon
import dev.bikram.filepipe.domain.model.RunHistory
import dev.bikram.filepipe.domain.model.RunStatus
import dev.bikram.filepipe.domain.model.TriggerType
import dev.bikram.filepipe.domain.model.isEffectivelyUndone
import dev.bikram.filepipe.domain.model.isNoChangesRun
import dev.bikram.filepipe.ui.common.FilePipeMaterialRoundedSymbol
import dev.bikram.filepipe.ui.theme.elevatedCardColors
import dev.bikram.filepipe.ui.theme.reducedMotionAwareSpec
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryCard(
    history: RunHistory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isActiveInDetailPane: Boolean = false,
    rule: Rule? = null,
) {
    val cardColors = elevatedCardColors()
    val historyCardShape = MaterialTheme.shapes.medium
    val activePaneModifier =
        if (isActiveInDetailPane) {
            Modifier.border(1.dp, MaterialTheme.colorScheme.secondary, historyCardShape)
        } else {
            Modifier
        }
    FilePipeSurface(
        onClick = onClick,
        modifier =
            modifier
                .fillMaxWidth()
                .then(activePaneModifier)
                .semantics(mergeDescendants = true) {
                    role = Role.Button
                },
        shape = historyCardShape,
        color = cardColors.containerColor,
        contentColor = cardColors.contentColor,
        tonalElevation = 1.dp,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                RuleIconOrEmoji(
                    iconEmoji = rule?.iconEmoji,
                    icon = rule?.icon ?: RuleIcon.DEFAULT,
                    vectorSize = 22.dp,
                    emojiFontSize = 18.sp,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = history.ruleName,
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        val operationIconName =
                            when (history.operationMode) {
                                OperationMode.COPY -> "file_copy"
                                OperationMode.MOVE -> "move_item"
                                OperationMode.DELETE -> "delete"
                            }
                        val modeLabel =
                            when (history.operationMode) {
                                OperationMode.COPY -> stringResource(R.string.operation_copy)
                                OperationMode.MOVE -> stringResource(R.string.operation_move)
                                OperationMode.DELETE -> stringResource(R.string.operation_delete)
                            }
                        FilePipeMaterialRoundedSymbol(
                            name = operationIconName,
                            contentDescription = modeLabel,
                            size = 14.dp,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        )
                    }

                    val noChanges = history.isNoChangesRun()
                    val chipStatus = if (history.isEffectivelyUndone()) RunStatus.UNDONE else history.status
                    StatusChip(status = chipStatus, noChanges = noChanges)
                }

                Spacer(Modifier.height(4.dp))

                val triggerLabel =
                    when (history.triggeredBy) {
                        TriggerType.MANUAL -> stringResource(R.string.history_triggered_manual)
                        TriggerType.SCHEDULED -> stringResource(R.string.history_triggered_scheduled)
                    }
                val cardContext = androidx.compose.ui.platform.LocalContext.current
                val timeLabel = formatTime(cardContext, history.startedAt)
                val successPart =
                    if (history.totalFilesMoved > 0) {
                        when (history.operationMode) {
                            OperationMode.COPY -> {
                                pluralStringResource(
                                    R.plurals.history_files_copied,
                                    history.totalFilesMoved,
                                    history.totalFilesMoved,
                                )
                            }

                            OperationMode.MOVE -> {
                                pluralStringResource(
                                    R.plurals.history_files_moved,
                                    history.totalFilesMoved,
                                    history.totalFilesMoved,
                                )
                            }

                            OperationMode.DELETE -> {
                                pluralStringResource(
                                    R.plurals.history_files_deleted,
                                    history.totalFilesMoved,
                                    history.totalFilesMoved,
                                )
                            }
                        }
                    } else {
                        ""
                    }
                val failedPart =
                    if (history.totalFilesFailed > 0) {
                        pluralStringResource(
                            R.plurals.history_files_failed,
                            history.totalFilesFailed,
                            history.totalFilesFailed,
                        )
                    } else {
                        ""
                    }
                val cancelledPart =
                    if (history.cancelledUnprocessedCount > 0) {
                        pluralStringResource(
                            R.plurals.history_files_cancelled_remaining,
                            history.cancelledUnprocessedCount,
                            history.cancelledUnprocessedCount,
                        )
                    } else {
                        ""
                    }
                val fileSummary =
                    when {
                        history.status == RunStatus.CANCELLED &&
                            history.totalFilesMoved == 0 &&
                            history.totalFilesFailed == 0 &&
                            history.cancelledUnprocessedCount == 0 -> {
                            stringResource(R.string.status_cancelled)
                        }

                        history.totalFilesMoved == 0 && history.totalFilesFailed == 0 && cancelledPart.isEmpty() -> {
                            stringResource(R.string.history_no_files_affected)
                        }

                        else -> {
                            buildString {
                                if (successPart.isNotEmpty()) append(successPart)
                                if (failedPart.isNotEmpty()) {
                                    if (isNotEmpty()) append(", ")
                                    append(failedPart)
                                }
                                if (cancelledPart.isNotEmpty()) {
                                    if (isNotEmpty()) append(", ")
                                    append(cancelledPart)
                                }
                            }
                        }
                    }
                Text(
                    text = "$triggerLabel · $timeLabel · $fileSummary",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun StatusChip(
    status: RunStatus,
    modifier: Modifier = Modifier,
    noChanges: Boolean = false,
) {
    val (label, targetContainerColor, targetContentColor) =
        when {
            noChanges -> {
                Triple(
                    stringResource(R.string.status_no_changes),
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            else -> {
                when (status) {
                    RunStatus.SUCCESS -> {
                        Triple(
                            stringResource(R.string.status_success),
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }

                    RunStatus.PARTIAL_FAILURE -> {
                        Triple(
                            stringResource(R.string.status_partial),
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }

                    RunStatus.FAILED -> {
                        Triple(
                            stringResource(R.string.status_failed),
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }

                    RunStatus.IN_PROGRESS -> {
                        Triple(
                            stringResource(R.string.status_in_progress),
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }

                    RunStatus.CANCELLED -> {
                        Triple(
                            stringResource(R.string.status_cancelled),
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    RunStatus.UNDONE -> {
                        Triple(
                            stringResource(R.string.status_undone),
                            MaterialTheme.colorScheme.surfaceVariant,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    RunStatus.PARTIAL_UNDONE -> {
                        Triple(
                            stringResource(R.string.status_partially_undone),
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
        }
    val containerColor by animateColorAsState(
        targetValue = targetContainerColor,
        animationSpec = reducedMotionAwareSpec(MaterialTheme.motionScheme.defaultEffectsSpec()),
        label = "chipContainerColor",
    )
    val contentColor by animateColorAsState(
        targetValue = targetContentColor,
        animationSpec = reducedMotionAwareSpec(MaterialTheme.motionScheme.defaultEffectsSpec()),
        label = "chipContentColor",
    )
    Surface(
        modifier = modifier,
        shape = SuggestionChipDefaults.shape,
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
        )
    }
}

fun formatTime(
    context: android.content.Context,
    millis: Long,
): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    val locale = Locale.getDefault()
    val isSystem24Hour =
        android.text.format.DateFormat
            .is24HourFormat(context)
    val timePattern = if (isSystem24Hour) "HH:mm" else "h:mm a"
    val dateTimePattern = if (isSystem24Hour) "MMM d, HH:mm" else "MMM d, h:mm a"
    val pattern = if (diff < 24 * 60 * 60 * 1000L) timePattern else dateTimePattern
    return SimpleDateFormat(pattern, locale).format(Date(millis))
}
