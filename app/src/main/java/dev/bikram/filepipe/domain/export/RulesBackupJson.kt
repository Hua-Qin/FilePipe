package dev.bikram.filepipe.domain.export

import dev.bikram.filepipe.domain.model.ConflictPolicy
import dev.bikram.filepipe.domain.model.OperationMode
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RuleIcon
import dev.bikram.filepipe.domain.model.RuleSchedule
import dev.bikram.filepipe.domain.model.ScheduleType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class RulesBackup(
    val version: Int = 3,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val rules: List<RuleBackupDto>
)

@Serializable
data class RuleBackupDto(
    val name: String,
    val sourceFolderPaths: List<String>,
    val destinationFolderPath: String,
    val fileExtensions: List<String>,
    val isEnabled: Boolean = true,
    val schedule: ScheduleBackupDto? = null,
    val conflictPolicy: String = ConflictPolicy.RENAME_SUFFIX.name,
    val operationMode: String = OperationMode.MOVE.name,
    val scanSubdirectories: Boolean = false,
    val iconKey: String = RuleIcon.DEFAULT.name
)

@Serializable
data class ScheduleBackupDto(
    val type: String,
    val dayOfWeek: Int? = null,
    val hour: Int,
    val minute: Int
)

private val jsonFormatter = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

fun Rule.toBackupDto(): RuleBackupDto = RuleBackupDto(
    name = name,
    sourceFolderPaths = sourceFolderPaths,
    destinationFolderPath = destinationFolderPath,
    fileExtensions = fileExtensions,
    isEnabled = isEnabled,
    schedule = schedule?.toBackupDto(),
    conflictPolicy = conflictPolicy.name,
    operationMode = operationMode.name,
    scanSubdirectories = scanSubdirectories,
    iconKey = icon.name
)

fun RuleSchedule.toBackupDto(): ScheduleBackupDto = ScheduleBackupDto(
    type = type.name,
    dayOfWeek = dayOfWeek,
    hour = hour,
    minute = minute
)

fun RuleBackupDto.toDomain(): Rule = Rule(
    id = 0L,
    name = name,
    sourceFolderPaths = sourceFolderPaths,
    destinationFolderPath = destinationFolderPath,
    fileExtensions = fileExtensions,
    isEnabled = isEnabled,
    schedule = schedule?.toDomain(),
    conflictPolicy = runCatching { ConflictPolicy.valueOf(conflictPolicy) }.getOrDefault(ConflictPolicy.RENAME_SUFFIX),
    operationMode = runCatching { OperationMode.valueOf(operationMode) }.getOrDefault(OperationMode.MOVE),
    scanSubdirectories = scanSubdirectories,
    icon = RuleIcon.fromStored(iconKey)
)

fun ScheduleBackupDto.toDomain(): RuleSchedule? {
    val scheduleType = runCatching { ScheduleType.valueOf(type) }.getOrNull() ?: return null
    return RuleSchedule(
        type = scheduleType,
        dayOfWeek = dayOfWeek,
        hour = hour,
        minute = minute
    )
}

fun buildRulesBackupJson(rules: List<Rule>): String {
    val backup = RulesBackup(
        exportedAtMillis = System.currentTimeMillis(),
        rules = rules.map { it.toBackupDto() }
    )
    return jsonFormatter.encodeToString(backup)
}

fun parseRulesBackupJson(text: String): Result<RulesBackup> = runCatching {
    jsonFormatter.decodeFromString<RulesBackup>(text)
}
