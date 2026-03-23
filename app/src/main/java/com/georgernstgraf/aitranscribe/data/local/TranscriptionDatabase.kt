package com.georgernstgraf.aitranscribe.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [TranscriptionEntity::class, QueuedTranscriptionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class TranscriptionDatabase : RoomDatabase() {

    abstract fun transcriptionDao(): TranscriptionDao
    abstract fun queuedTranscriptionDao(): QueuedTranscriptionDao

    companion object {
        private const val DATABASE_NAME = "aitranscribe.db"

        @Volatile
        private var INSTANCE: TranscriptionDatabase? = null

        fun getDatabase(context: Context): TranscriptionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    TranscriptionDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_1_2)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transcriptions ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}