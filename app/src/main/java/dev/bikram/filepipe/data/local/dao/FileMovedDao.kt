package dev.bikram.filepipe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.bikram.filepipe.data.local.entity.FileMovedEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FileMovedDao {
    @Query("SELECT * FROM files_moved WHERE runHistoryId = :runHistoryId ORDER BY movedAt ASC")
    fun getFilesForRun(runHistoryId: Long): Flow<List<FileMovedEntity>>

    @Query("SELECT * FROM files_moved WHERE runHistoryId = :runHistoryId ORDER BY movedAt ASC")
    suspend fun getFilesForRunOnce(runHistoryId: Long): List<FileMovedEntity>

    @Insert
    suspend fun insertFileMoved(file: FileMovedEntity)

    @Insert
    suspend fun insertFilesMoved(files: List<FileMovedEntity>)
}
