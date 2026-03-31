package dev.bikram.filepipe.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.bikram.filepipe.domain.model.RunHistory
import dev.bikram.filepipe.domain.model.RunStatus
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
    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = history.ruleName,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                StatusChip(status = history.status)
            }

            Spacer(Modifier.height(6.dp))

            val triggerLabel = when (history.triggeredBy) {
                TriggerType.MANUAL -> "Manual"
                TriggerType.SCHEDULED -> "Scheduled"
            }
            val timeLabel = formatTime(history.startedAt)
            Text(
                text = "$triggerLabel · $timeLabel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (history.totalFilesMoved > 0 || history.totalFilesFailed > 0) {
                Spacer(Modifier.height(4.dp))
                val summary = buildString {
                    if (history.totalFilesMoved > 0) append("${history.totalFilesMoved} moved")
                    if (history.totalFilesFailed > 0) {
                        if (isNotEmpty()) append(", ")
                        append("${history.totalFilesFailed} failed")
                    }
                }
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun StatusChip(status: RunStatus, modifier: Modifier = Modifier) {
    val (label, containerColor) = when (status) {
        RunStatus.SUCCESS -> "Success" to MaterialTheme.colorScheme.primaryContainer
        RunStatus.PARTIAL_FAILURE -> "Partial" to MaterialTheme.colorScheme.tertiaryContainer
        RunStatus.FAILED -> "Failed" to MaterialTheme.colorScheme.errorContainer
        RunStatus.IN_PROGRESS -> "Running" to MaterialTheme.colorScheme.secondaryContainer
    }
    AssistChip(
        onClick = {},
        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(containerColor = containerColor)
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
