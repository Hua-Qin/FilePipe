package dev.bikram.filepipe.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bikram.filepipe.domain.model.RunHistory
import dev.bikram.filepipe.domain.model.RunStatus
import dev.bikram.filepipe.domain.model.isNoChangesRun
import dev.bikram.filepipe.domain.model.TriggerType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryCard(
    history: RunHistory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hoverInteraction = remember { MutableInteractionSource() }
    val isHovered by hoverInteraction.collectIsHoveredAsState()
    val elevation by animateDpAsState(
        targetValue = if (isHovered) 8.dp else 2.dp,
        label = "cardElevation"
    )
    ElevatedCard(
        modifier = modifier.fillMaxWidth().hoverable(hoverInteraction),
        onClick = onClick,
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = elevation),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = history.ruleName,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val noChanges = history.isNoChangesRun()
                StatusChip(status = history.status, noChanges = noChanges)
            }

            Spacer(Modifier.height(6.dp))

            val triggerLabel = when (history.triggeredBy) {
                TriggerType.MANUAL -> "Manual"
                TriggerType.SCHEDULED -> "Scheduled"
            }
            val timeLabel = formatTime(history.startedAt)
            val fileSummary = when {
                history.totalFilesMoved == 0 && history.totalFilesFailed == 0 -> "No files affected"
                else -> buildString {
                    if (history.totalFilesMoved > 0) append("${history.totalFilesMoved} moved")
                    if (history.totalFilesFailed > 0) {
                        if (isNotEmpty()) append(", ")
                        append("${history.totalFilesFailed} failed")
                    }
                }
            }
            Text(
                text = "$triggerLabel · $timeLabel · $fileSummary",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatusChip(status: RunStatus, noChanges: Boolean = false, modifier: Modifier = Modifier) {
    val (label, targetColor) = when {
        noChanges -> "No changes" to MaterialTheme.colorScheme.surfaceVariant
        else -> when (status) {
            RunStatus.SUCCESS -> "Success" to MaterialTheme.colorScheme.primaryContainer
            RunStatus.PARTIAL_FAILURE -> "Partial" to MaterialTheme.colorScheme.tertiaryContainer
            RunStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.errorContainer
            RunStatus.IN_PROGRESS -> "Running" to MaterialTheme.colorScheme.secondaryContainer
        }
    }
    val containerColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(300),
        label = "chipColor"
    )
    SuggestionChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        modifier = modifier,
        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = containerColor)
    )
}

private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
private val dateTimeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())

fun formatTime(millis: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - millis
    return if (diff < 24 * 60 * 60 * 1000L) {
        timeFormat.format(Date(millis))
    } else {
        dateTimeFormat.format(Date(millis))
    }
}
