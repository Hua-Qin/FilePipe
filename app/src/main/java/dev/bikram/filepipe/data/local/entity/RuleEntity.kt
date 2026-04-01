package dev.bikram.filepipe.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import dev.bikram.filepipe.domain.model.ConflictPolicy
import dev.bikram.filepipe.domain.model.OperationMode
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RuleIcon
import dev.bikram.filepipe.domain.model.RuleSchedule
import dev.bikram.filepipe.domain.model.ScheduleType

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sourceFolderPaths: List<String>,
    val destinationFolderPath: String,
    val fileExtensions: List<String>,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val scheduleType: ScheduleType? = null,
    val scheduleDayOfWeek: Int? = null,
    val scheduleHour: Int? = null,
    val scheduleMinute: Int? = null,
    val workManagerTag: String? = null,
    val conflictPolicy: String = ConflictPolicy.RENAME_SUFFIX.name,
    val operationMode: String = OperationMode.MOVE.name,
    val scanSubdirectories: Boolean = false,
    val iconKey: String = RuleIcon.DEFAULT.name
)

fun RuleEntity.toDomain(): Rule = Rule(
    id = id,
    name = name,
    sourceFolderPaths = sourceFolderPaths,
    destinationFolderPath = destinationFolderPath,
    fileExtensions = fileExtensions,
    isEnabled = isEnabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
    schedule = if (scheduleType != null && scheduleHour != null && scheduleMinute != null) {
        RuleSchedule(
            type = scheduleType,
            dayOfWeek = scheduleDayOfWeek,
            hour = scheduleHour,
            minute = scheduleMinute
        )
    } else null,
    conflictPolicy = runCatching { ConflictPolicy.valueOf(conflictPolicy) }.getOrDefault(ConflictPolicy.RENAME_SUFFIX),
    operationMode = runCatching { OperationMode.valueOf(operationMode) }.getOrDefault(OperationMode.MOVE),
    scanSubdirectories = scanSubdirectories,
    icon = RuleIcon.fromStored(iconKey)
)

fun Rule.toEntity(): RuleEntity = RuleEntity(
    id = id,
    name = name,
    sourceFolderPaths = sourceFolderPaths,
    destinationFolderPath = destinationFolderPath,
    fileExtensions = fileExtensions,
    isEnabled = isEnabled,
    createdAt = createdAt,
    updatedAt = updatedAt,
    scheduleType = schedule?.type,
    scheduleDayOfWeek = schedule?.dayOfWeek,
    scheduleHour = schedule?.hour,
    scheduleMinute = schedule?.minute,
    workManagerTag = if (id != 0L) "rule_$id" else null,
    conflictPolicy = conflictPolicy.name,
    operationMode = operationMode.name,
    scanSubdirectories = scanSubdirectories,
    iconKey = icon.name
)
