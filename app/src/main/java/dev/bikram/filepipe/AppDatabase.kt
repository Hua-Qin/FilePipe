package dev.bikram.filepipe

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import dev.bikram.filepipe.data.local.dao.FileMovedDao
import dev.bikram.filepipe.data.local.dao.RuleDao
import dev.bikram.filepipe.data.local.dao.RunHistoryDao
import dev.bikram.filepipe.data.local.database.Converters
import dev.bikram.filepipe.data.local.entity.FileMovedEntity
import dev.bikram.filepipe.data.local.entity.RuleEntity
import dev.bikram.filepipe.data.local.entity.RunHistoryEntity

@Database(
    entities = [RuleEntity::class, RunHistoryEntity::class, FileMovedEntity::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun ruleDao(): RuleDao
    abstract fun runHistoryDao(): RunHistoryDao
    abstract fun fileMovedDao(): FileMovedDao
}
