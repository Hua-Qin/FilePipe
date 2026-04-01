package dev.bikram.filepipe.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.bikram.filepipe.domain.model.RuleSchedule
import dev.bikram.filepipe.domain.model.ScheduleType
import dev.bikram.filepipe.ui.feedback.rememberPlayTapSound
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDialog(
    initialSchedule: RuleSchedule?,
    onDismiss: () -> Unit,
    onSave: (RuleSchedule?) -> Unit
) {
    val playTap = rememberPlayTapSound()
    var scheduleType by remember { mutableStateOf(initialSchedule?.type ?: ScheduleType.DAILY) }
    var hour by remember { mutableIntStateOf(initialSchedule?.hour ?: 2) }
    var minute by remember { mutableIntStateOf(initialSchedule?.minute ?: 0) }
    var dayOfWeek by remember { mutableIntStateOf(initialSchedule?.dayOfWeek ?: Calendar.MONDAY) }

    var typeExpanded by remember { mutableStateOf(false) }
    var dayExpanded by remember { mutableStateOf(false) }

    val days = listOf(
        Calendar.MONDAY to "Monday",
        Calendar.TUESDAY to "Tuesday",
        Calendar.WEDNESDAY to "Wednesday",
        Calendar.THURSDAY to "Thursday",
        Calendar.FRIDAY to "Friday",
        Calendar.SATURDAY to "Saturday",
        Calendar.SUNDAY to "Sunday"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Schedule") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Frequency picker
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = if (scheduleType == ScheduleType.DAILY) "Daily" else "Weekly",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Frequency") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Daily") },
                            onClick = { scheduleType = ScheduleType.DAILY; typeExpanded = false }
                        )
                        DropdownMenuItem(
                            text = { Text("Weekly") },
                            onClick = { scheduleType = ScheduleType.WEEKLY; typeExpanded = false }
                        )
                    }
                }

                // Day of week (only for weekly)
                if (scheduleType == ScheduleType.WEEKLY) {
                    val selectedDayName = days.find { it.first == dayOfWeek }?.second ?: "Monday"
                    ExposedDropdownMenuBox(
                        expanded = dayExpanded,
                        onExpandedChange = { dayExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedDayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Day") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(dayExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = dayExpanded,
                            onDismissRequest = { dayExpanded = false }
                        ) {
                            days.forEach { (calDay, name) ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = { dayOfWeek = calDay; dayExpanded = false }
                                )
                            }
                        }
                    }
                }

                // Time picker (simple text fields)
                Text("Time", style = MaterialTheme.typography.labelMedium)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = "%02d".format(hour),
                        onValueChange = { v -> v.toIntOrNull()?.let { if (it in 0..23) hour = it } },
                        label = { Text("HH") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Text(" : ", modifier = Modifier.padding(horizontal = 8.dp))
                    OutlinedTextField(
                        value = "%02d".format(minute),
                        onValueChange = { v -> v.toIntOrNull()?.let { if (it in 0..59) minute = it } },
                        label = { Text("MM") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    playTap()
                    onSave(
                        RuleSchedule(
                            type = scheduleType,
                            dayOfWeek = if (scheduleType == ScheduleType.WEEKLY) dayOfWeek else null,
                            hour = hour,
                            minute = minute
                        )
                    )
                },
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            Row {
                if (initialSchedule != null) {
                    OutlinedButton(
                        onClick = {
                            playTap()
                            onSave(null)
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Remove", color = MaterialTheme.colorScheme.error)
                    }
                    Spacer(Modifier.width(8.dp))
                }
                OutlinedButton(
                    onClick = {
                        playTap()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }
            }
        }
    )
}
