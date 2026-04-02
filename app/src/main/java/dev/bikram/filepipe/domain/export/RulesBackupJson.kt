package dev.bikram.filepipe.domain.export

import dev.bikram.filepipe.data.preferences.AppPreferences
import dev.bikram.filepipe.domain.model.ConflictPolicy
import dev.bikram.filepipe.domain.model.FileMoved
import dev.bikram.filepipe.domain.model.OperationMode
import dev.bikram.filepipe.domain.model.Rule
import dev.bikram.filepipe.domain.model.RuleIcon
import dev.bikram.filepipe.domain.model.RuleSchedule
import dev.bikram.filepipe.domain.model.RunHistory
import dev.bikram.filepipe.domain.model.ScheduleType
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class AppBackup(
    val version: Int = 4,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val rules: List<RuleBackupDto>,
    val history: List<RunHistoryBackupDto> = emptyList(),
    val settings: SettingsBackupDto? = null
)

/** Kept for backward compatibility — parsed the same as AppBackup */
typealias RulesBackup = AppBackup

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
    val iconKey: String = RuleIcon.DEFAULT.name,
    val filenamePattern: String? = null,
    val minFileSizeBytes: Long? = null,
    val maxFileSizeBytes: Long? = null,
    val minAgeDays: Int? = null,
    val maxAgeDays: Int? = null,
    val excludePatterns: List<String> = emptyList()
)

@Serializable
data class ScheduleBackupDto(
    val type: String,
    val dayOfWeek: Int? = null,
    val hour: Int,
    val minute: Int
)

@Serializable
data class RunHistoryBackupDto(
    val ruleName: String,
    val triggeredBy: String,
    val startedAt: Long,
    val completedAt: Long? = null,
    val status: String,
    val totalFilesMoved: Int,
    val totalFilesFailed: Int,
    val errorMessage: String? = null,
    val isReversed: Boolean = false,
    val files: List<FileMovedBackupDto> = emptyList()
)

@Serializable
data class FileMovedBackupDto(
    val fileName: String,
    val sourceUri: String,
    val destinationUri: String,
    val fileSizeBytes: Long,
    val movedAt: Long,
    val success: Boolean,
    val skipped: Boolean = false,
    val errorMessage: String? = null
)

@Serializable
data class SettingsBackupDto(
    val themeMode: String,
    val useMaterialYou: Boolean,
    val exportFolderUri: String,
    val autoExportOnRuleChange: Boolean,
    val scheduledExportEnabled: Boolean,
    val logRetentionDays: Int,
    val swipeStartToEnd: String,
    val swipeEndToStart: String,
    val bookmarkedFolders: List<String>
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
    iconKey = icon.name,
    filenamePattern = filenamePattern,
    minFileSizeBytes = minFileSizeBytes,
    maxFileSizeBytes = maxFileSizeBytes,
    minAgeDays = minAgeDays,
    maxAgeDays = maxAgeDays,
    excludePatterns = excludePatterns
)

fun RuleSchedule.toBackupDto(): ScheduleBackupDto = ScheduleBackupDto(
    type = type.name,
    dayOfWeek = dayOfWeek,
    hour = hour,
    minute = minute
)

fun RunHistory.toBackupDto(files: List<FileMoved> = emptyList()): RunHistoryBackupDto = RunHistoryBackupDto(
    ruleName = ruleName,
    triggeredBy = triggeredBy.name,
    startedAt = startedAt,
    completedAt = completedAt,
    status = status.name,
    totalFilesMoved = totalFilesMoved,
    totalFilesFailed = totalFilesFailed,
    errorMessage = errorMessage,
    isReversed = isReversed,
    files = files.map { it.toBackupDto() }
)

fun FileMoved.toBackupDto(): FileMovedBackupDto = FileMovedBackupDto(
    fileName = fileName,
    sourceUri = sourceUri,
    destinationUri = destinationUri,
    fileSizeBytes = fileSizeBytes,
    movedAt = movedAt,
    success = success,
    skipped = skipped,
    errorMessage = errorMessage
)

fun AppPreferences.toBackupDto(): SettingsBackupDto = SettingsBackupDto(
    themeMode = themeMode.name,
    useMaterialYou = useMaterialYou,
    exportFolderUri = exportFolderUri,
    autoExportOnRuleChange = autoExportOnRuleChange,
    scheduledExportEnabled = scheduledExportEnabled,
    logRetentionDays = logRetentionDays,
    swipeStartToEnd = swipeStartToEnd.name,
    swipeEndToStart = swipeEndToStart.name,
    bookmarkedFolders = bookmarkedFolders
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
    icon = RuleIcon.fromStored(iconKey),
    filenamePattern = filenamePattern,
    minFileSizeBytes = minFileSizeBytes,
    maxFileSizeBytes = maxFileSizeBytes,
    minAgeDays = minAgeDays,
    maxAgeDays = maxAgeDays,
    excludePatterns = excludePatterns
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

fun buildAppBackupJson(
    rules: List<Rule>,
    history: List<Pair<RunHistory, List<FileMoved>>> = emptyList(),
    settings: AppPreferences? = null
): String {
    val backup = AppBackup(
        exportedAtMillis = System.currentTimeMillis(),
        rules = rules.map { it.toBackupDto() },
        history = history.map { (run, files) -> run.toBackupDto(files) },
        settings = settings?.toBackupDto()
    )
    return jsonFormatter.encodeToString(backup)
}

/** Kept for backward compatibility */
fun buildRulesBackupJson(rules: List<Rule>): String = buildAppBackupJson(rules)

fun parseRulesBackupJson(text: String): Result<AppBackup> = runCatching {
    jsonFormatter.decodeFromString<AppBackup>(text)
}
