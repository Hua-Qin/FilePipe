package dev.bikram.filepipe.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import dev.bikram.filepipe.data.local.entity.RunHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RunHistoryDao {

    @Query("SELECT * FROM run_history ORDER BY startedAt DESC")
    fun getAllHistory(): Flow<List<RunHistoryEntity>>

    @Query("SELECT * FROM run_history WHERE id = :id")
    suspend fun getHistoryById(id: Long): RunHistoryEntity?

    @Query("SELECT * FROM run_history WHERE ruleId = :ruleId ORDER BY startedAt DESC")
    fun getHistoryForRule(ruleId: Long): Flow<List<RunHistoryEntity>>

    @Insert
    suspend fun insertHistory(history: RunHistoryEntity): Long

    @Update
    suspend fun updateHistory(history: RunHistoryEntity)

    @Query("DELETE FROM run_history WHERE startedAt < :olderThan")
    suspend fun deleteHistoryOlderThan(olderThan: Long)

    @Query("DELETE FROM run_history")
    suspend fun deleteAllHistory()
}
