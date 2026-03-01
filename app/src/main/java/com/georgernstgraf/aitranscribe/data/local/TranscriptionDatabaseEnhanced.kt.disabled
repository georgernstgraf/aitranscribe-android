package com.georgernstgraf.aitranscribe.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [TranscriptionEntity::class, QueuedTranscriptionEntity::class],
    version = 2,
    exportSchema = true
)
abstract class TranscriptionDatabaseEnhanced : RoomDatabase() {

    abstract fun transcriptionDao(): TranscriptionDaoEnhanced
    abstract fun queuedTranscriptionDao(): QueuedTranscriptionDao

    companion object {
        private const val DATABASE_NAME = "aitranscribe.db"

        fun getEnhancedDatabase(context: Context, useInMemory: Boolean = false): TranscriptionDatabaseEnhanced {
            val builder = if (useInMemory) {
                Room.inMemoryDatabaseBuilder(context)
            } else {
                Room.databaseBuilder(context, DATABASE_NAME)
            }
            
            return builder
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}