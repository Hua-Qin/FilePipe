package dev.bikram.filepipe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimeInput
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.bikram.filepipe.R
import dev.bikram.filepipe.domain.model.RuleSchedule
import dev.bikram.filepipe.domain.model.ScheduleType
import dev.bikram.filepipe.ui.common.FilePipeMaterialRoundedSymbol
import dev.bikram.filepipe.ui.theme.compactControlShape
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleDialog(
    initialSchedule: RuleSchedule?,
    onDismiss: () -> Unit,
    onSave: (RuleSchedule?) -> Unit,
) {
    var scheduleType by remember { mutableStateOf(initialSchedule?.type ?: ScheduleType.DAILY) }
    var hour by remember { mutableIntStateOf(initialSchedule?.hour ?: 9) }
    var minute by remember { mutableIntStateOf(initialSchedule?.minute ?: 0) }

    // selectedDays bitmask initialized from model helper
    var selectedDays by remember {
        mutableStateOf(
            RuleSchedule.bitmaskToDaysOfWeek(initialSchedule?.dayOfWeek).toSet()
        )
    }

    var intervalText by remember {
        mutableStateOf(
            (initialSchedule?.intervalHours ?: 1).toString()
        )
    }
    var intervalFieldError by remember { mutableStateOf(false) }

    var typeExpanded by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    fun validateIntervalText(): Boolean {
        val parsed = intervalText.toIntOrNull()
        val valid = parsed != null && parsed >= 1
        intervalFieldError = !valid
        return valid
    }

    if (showTimePicker) {
        ScheduleTimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { pickedHour, pickedMinute ->
                hour = pickedHour
                minute = pickedMinute
                showTimePicker = false
            },
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp,
            modifier =
                Modifier
                    .widthIn(max = 400.dp)
                    .padding(24.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = stringResource(R.string.schedule_dialog_title),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = intervalText,
                            onValueChange = { newValue ->
                                intervalText = newValue.filter { character -> character.isDigit() }.take(3)
                                intervalFieldError = false
                            },
                            isError = intervalFieldError,
                            label = { Text(stringResource(R.string.schedule_every)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )

                        ExposedDropdownMenuBox(
                            expanded = typeExpanded,
                            onExpandedChange = { typeExpanded = it },
                            modifier = Modifier.weight(1.2f),
                        ) {
                            val selectedLabel = stringResource(
                                when (scheduleType) {
                                    ScheduleType.EVERY_N_HOURS -> R.string.schedule_unit_hours
                                    ScheduleType.DAILY -> R.string.schedule_unit_days
                                    ScheduleType.WEEKLY -> R.string.schedule_unit_weeks
                                }
                            )
                            OutlinedTextField(
                                value = selectedLabel,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.schedule_frequency)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(typeExpanded) },
                                modifier = Modifier
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true)
                                    .fillMaxWidth(),
                            )
                            ExposedDropdownMenu(
                                expanded = typeExpanded,
                                onDismissRequest = { typeExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ) {
                                FilePipeDropdownMenuItem(
                                    text = { Text(stringResource(R.string.schedule_unit_hours)) },
                                    onClick = {
                                        scheduleType = ScheduleType.EVERY_N_HOURS
                                        typeExpanded = false
                                    },
                                )
                                FilePipeDropdownMenuItem(
                                    text = { Text(stringResource(R.string.schedule_unit_days)) },
                                    onClick = {
                                        scheduleType = ScheduleType.DAILY
                                        typeExpanded = false
                                    },
                                )
                                FilePipeDropdownMenuItem(
                                    text = { Text(stringResource(R.string.schedule_unit_weeks)) },
                                    onClick = {
                                        scheduleType = ScheduleType.WEEKLY
                                        typeExpanded = false
                                    },
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.schedule_start_time),
                            style = MaterialTheme.typography.labelMedium,
                        )
                        val dialogContext = androidx.compose.ui.platform.LocalContext.current
                        val isSystem24Hour = android.text.format.DateFormat.is24HourFormat(dialogContext)
                        val timeStr = if (isSystem24Hour) {
                            "%02d:%02d".format(hour, minute)
                        } else {
                            val hour12 = when (val hourMod = hour % 12) {
                                0 -> 12
                                else -> hourMod
                            }
                            val amPm = if (hour < 12) "AM" else "PM"
                            "%d:%02d %s".format(hour12, minute, amPm)
                        }
                        FilePipeOutlinedButton(
                            onClick = { showTimePicker = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = compactControlShape,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilePipeMaterialRoundedSymbol(
                                    name = "schedule",
                                    contentDescription = null,
                                    size = 18.dp,
                                )
                                Text(text = timeStr)
                            }
                        }
                    }

                    if (scheduleType == ScheduleType.WEEKLY) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.schedule_day),
                                style = MaterialTheme.typography.labelMedium,
                            )
                            val weekdays = listOf(
                                Calendar.SUNDAY to stringResource(R.string.day_sun).first().toString(),
                                Calendar.MONDAY to stringResource(R.string.day_mon).first().toString(),
                                Calendar.TUESDAY to stringResource(R.string.day_tue).first().toString(),
                                Calendar.WEDNESDAY to stringResource(R.string.day_wed).first().toString(),
                                Calendar.THURSDAY to stringResource(R.string.day_thu).first().toString(),
                                Calendar.FRIDAY to stringResource(R.string.day_fri).first().toString(),
                                Calendar.SATURDAY to stringResource(R.string.day_sat).first().toString(),
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                weekdays.forEach { (calDay, shortLabel) ->
                                    val isSelected = selectedDays.contains(calDay)
                                    val dayBoxModifier = Modifier
                                        .size(38.dp)
                                        .background(
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer,
                                            shape = CircleShape
                                        )
                                        .clip(CircleShape)
                                        .clickable {
                                            selectedDays = if (isSelected) {
                                                selectedDays - calDay
                                            } else {
                                                selectedDays + calDay
                                            }
                                        }
                                    Box(
                                        modifier = dayBoxModifier,
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = shortLabel,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (initialSchedule != null) {
                        FilePipeTextButton(
                            onClick = {
                                onSave(null)
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = stringResource(R.string.schedule_remove_short),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    FilePipeOutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = compactControlShape,
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                    FilePipeButton(
                        onClick = {
                            val validInterval = validateIntervalText()
                            val validDays = scheduleType != ScheduleType.WEEKLY || selectedDays.isNotEmpty()
                            if (!validInterval || !validDays) {
                                return@FilePipeButton
                            }
                            val intervalParsed = intervalText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                            onSave(
                                RuleSchedule(
                                    type = scheduleType,
                                    dayOfWeek = if (scheduleType == ScheduleType.WEEKLY) {
                                        RuleSchedule.daysOfWeekToBitmask(selectedDays.toList())
                                    } else {
                                        null
                                    },
                                    hour = hour,
                                    minute = minute,
                                    intervalHours = intervalParsed,
                                ),
                            )
                        },
                        modifier = Modifier.weight(1f),
                        shape = compactControlShape,
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ScheduleTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    var showDial by remember { mutableStateOf(true) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        key(initialHour, initialMinute) {
            val pickerContext = androidx.compose.ui.platform.LocalContext.current
            val timePickerState =
                rememberTimePickerState(
                    initialHour = initialHour,
                    initialMinute = initialMinute,
                    is24Hour =
                        android.text.format.DateFormat
                            .is24HourFormat(pickerContext),
                )
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 6.dp,
                modifier =
                    Modifier
                        .widthIn(max = 560.dp)
                        .padding(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = stringResource(R.string.schedule_time_picker_title),
                        style = MaterialTheme.typography.titleMedium,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                    )
                    if (showDial) {
                        TimePicker(state = timePickerState)
                    } else {
                        TimeInput(state = timePickerState)
                    }
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TooltipBox(
                            positionProvider =
                                TooltipDefaults.rememberTooltipPositionProvider(
                                    TooltipAnchorPosition.Above,
                                ),
                            tooltip = {
                                PlainTooltip {
                                    CenteredTooltipText(
                                        text =
                                            if (showDial) {
                                                stringResource(R.string.schedule_time_input_mode_cd)
                                            } else {
                                                stringResource(R.string.schedule_time_dial_mode_cd)
                                            },
                                    )
                                }
                            },
                            state = rememberTooltipState(),
                        ) {
                            FilePipeIconButton(
                                onClick = { showDial = !showDial },
                            ) {
                                FilePipeMaterialRoundedSymbol(
                                    name = if (showDial) "keyboard" else "schedule",
                                    contentDescription =
                                        if (showDial) {
                                            stringResource(R.string.schedule_time_input_mode_cd)
                                        } else {
                                            stringResource(R.string.schedule_time_dial_mode_cd)
                                        },
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        FilePipeTextButton(onClick = onDismiss) {
                            Text(stringResource(R.string.cancel))
                        }
                        FilePipeTextButton(
                            onClick = {
                                onConfirm(timePickerState.hour, timePickerState.minute)
                            },
                        ) {
                            Text(stringResource(R.string.save))
                        }
                    }
                }
            }
        }
    }
}
