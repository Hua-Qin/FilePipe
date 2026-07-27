package dev.bikram.filepipe.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import dev.bikram.filepipe.domain.model.FileMoved
import dev.bikram.filepipe.domain.model.FileUndoStatus

@Entity(
    tableName = "files_moved",
    foreignKeys = [
        ForeignKey(
            entity = RunHistoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["runHistoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("runHistoryId")],
)
data class FileMovedEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val runHistoryId: Long,
    val fileName: String,
    val sourceUri: String,
    val destinationUri: String,
    val fileSizeBytes: Long,
    @ColumnInfo(defaultValue = "[]")
    val relativeParentSegments: List<String> = emptyList(),
    val movedAt: Long,
    val success: Boolean,
    val skipped: Boolean = false,
    val errorMessage: String? = null,
    @ColumnInfo(defaultValue = "'PENDING'")
    val undoStatus: FileUndoStatus = FileUndoStatus.PENDING,
)

fun FileMovedEntity.toDomain(): FileMoved =
    FileMoved(
        id = id,
        runHistoryId = runHistoryId,
        fileName = fileName,
        sourceUri = sourceUri,
        destinationUri = destinationUri,
        fileSizeBytes = fileSizeBytes,
        relativeParentSegments = relativeParentSegments,
        movedAt = movedAt,
        success = success,
        skipped = skipped,
        errorMessage = errorMessage,
        undoStatus = undoStatus,
    )

fun FileMoved.toEntity(runHistoryId: Long): FileMovedEntity =
    FileMovedEntity(
        id = id,
        runHistoryId = runHistoryId,
        fileName = fileName,
        sourceUri = sourceUri,
        destinationUri = destinationUri,
        fileSizeBytes = fileSizeBytes,
        relativeParentSegments = relativeParentSegments,
        movedAt = movedAt,
        success = success,
        skipped = skipped,
        errorMessage = errorMessage,
        undoStatus = undoStatus,
    )
