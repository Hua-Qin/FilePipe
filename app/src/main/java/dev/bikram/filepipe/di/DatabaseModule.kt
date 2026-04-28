package dev.bikram.filepipe.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.bikram.filepipe.AppDatabase
import dev.bikram.filepipe.data.local.dao.FileMovedDao
import dev.bikram.filepipe.data.local.dao.RuleDao
import dev.bikram.filepipe.data.local.dao.RunHistoryDao
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    private val migration2To3 =
        object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE run_history ADD COLUMN cancelledUnprocessedCount INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "ALTER TABLE run_history ADD COLUMN operationMode TEXT NOT NULL DEFAULT 'MOVE'",
                )
                db.execSQL(
                    "ALTER TABLE run_history ADD COLUMN copyCreatedDestFolderUris TEXT NOT NULL DEFAULT '[]'",
                )
                db.execSQL("ALTER TABLE rules ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE rules SET sortOrder = id")
            }
        }

    private val migration5To6 =
        object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE rules ADD COLUMN recreateDestinationSubfolders INTEGER NOT NULL DEFAULT 0",
                )
                db.execSQL(
                    "UPDATE rules SET recreateDestinationSubfolders = CASE WHEN scanSubdirectories = 1 THEN 1 ELSE 0 END",
                )
            }
        }

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): AppDatabase =
        Room
            .databaseBuilder(context, AppDatabase::class.java, "media_organizer.db")
            .addMigrations(migration2To3, migration5To6)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .build()

    @Provides
    fun provideRuleDao(db: AppDatabase): RuleDao = db.ruleDao()

    @Provides
    fun provideRunHistoryDao(db: AppDatabase): RunHistoryDao = db.runHistoryDao()

    @Provides
    fun provideFileMovedDao(db: AppDatabase): FileMovedDao = db.fileMovedDao()
}
