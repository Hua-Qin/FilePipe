package dev.bikram.filepipe.domain.model

enum class ConflictPolicy { SKIP, OVERWRITE, RENAME_SUFFIX }

enum class OperationMode { MOVE, COPY }

data class Rule(
    val id: Long = 0,
    val name: String,
    val sourceFolderPaths: List<String>,
    val destinationFolderPath: String,
    val fileExtensions: List<String>,
    val isEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val schedule: RuleSchedule? = null,
    val conflictPolicy: ConflictPolicy = ConflictPolicy.RENAME_SUFFIX,
    val operationMode: OperationMode = OperationMode.MOVE,
    val scanSubdirectories: Boolean = false,
    val icon: RuleIcon = RuleIcon.DEFAULT
)

enum class ScheduleType { DAILY, WEEKLY }

data class RuleSchedule(
    val type: ScheduleType,
    val dayOfWeek: Int? = null,  // Calendar.MONDAY (2) … Calendar.SUNDAY (1) — null for DAILY
    val hour: Int,
    val minute: Int
)

// ---

data class RunHistory(
    val id: Long = 0,
    val ruleId: Long?,
    val ruleName: String,
    val triggeredBy: TriggerType,
    val startedAt: Long,
    val completedAt: Long? = null,
    val status: RunStatus,
    val totalFilesFound: Int = 0,
    val totalFilesMoved: Int = 0,
    val totalFilesFailed: Int = 0,
    val errorMessage: String? = null,
    val isReversed: Boolean = false
)

enum class TriggerType { MANUAL, SCHEDULED }

enum class RunStatus { IN_PROGRESS, SUCCESS, PARTIAL_FAILURE, FAILED }

// ---

data class FileMoved(
    val id: Long = 0,
    val runHistoryId: Long = 0,
    val fileName: String,
    val sourceUri: String,
    val destinationUri: String,
    val fileSizeBytes: Long,
    val movedAt: Long = System.currentTimeMillis(),
    val success: Boolean,
    val skipped: Boolean = false,
    val errorMessage: String? = null
)

// ---

data class RunResult(
    val ruleId: Long,
    val ruleName: String,
    val historyId: Long,
    val filesMoved: List<FileMoved>,
    val startedAt: Long,
    val completedAt: Long
) {
    val totalMoved: Int get() = filesMoved.count { it.success && !it.skipped }
    val totalSkipped: Int get() = filesMoved.count { it.skipped }
    val totalFailed: Int get() = filesMoved.count { !it.success && !it.skipped }
    val status: RunStatus get() = when {
        filesMoved.isEmpty() -> RunStatus.SUCCESS
        totalFailed == 0 -> RunStatus.SUCCESS
        totalMoved == 0 && totalSkipped == 0 -> RunStatus.FAILED
        else -> RunStatus.PARTIAL_FAILURE
    }
}

// ---

data class RunProgress(
    val ruleId: Long,
    val ruleName: String,
    val progress: Float = 0f,
    val currentFileName: String = "",
    val filesMoved: Int = 0,
    val totalFiles: Int = 0,
    val isComplete: Boolean = false,
    val error: String? = null
)
