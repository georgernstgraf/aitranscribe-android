package com.georgernstgraf.aitranscribe.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TranscriptionEntity::class,
        QueuedTranscriptionEntity::class,
        ProviderEntity::class,
        ModelEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class TranscriptionDatabase : RoomDatabase() {

    abstract fun transcriptionDao(): TranscriptionDao
    abstract fun queuedTranscriptionDao(): QueuedTranscriptionDao
    abstract fun providerModelDao(): ProviderModelDao

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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .addCallback(ProviderPrepopulateCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class ProviderPrepopulateCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                // Pre-populate providers on creation
                db.execSQL("INSERT INTO providers (id, display_name, last_synced_at) VALUES ('groq', 'Groq', 0)")
                db.execSQL("INSERT INTO providers (id, display_name, last_synced_at) VALUES ('openrouter', 'OpenRouter', 0)")
                db.execSQL("INSERT INTO providers (id, display_name, last_synced_at) VALUES ('zai', 'ZAI', 0)")
            }
        }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transcriptions ADD COLUMN retry_count INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE transcriptions ADD COLUMN summary TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `providers` (
                        `id` TEXT NOT NULL, 
                        `display_name` TEXT NOT NULL, 
                        `last_synced_at` INTEGER NOT NULL, 
                        PRIMARY KEY(`id`)
                    )
                """)
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `models` (
                        `id` TEXT NOT NULL, 
                        `provider_id` TEXT NOT NULL, 
                        `model_name` TEXT NOT NULL, 
                        `capabilities` TEXT, 
                        PRIMARY KEY(`id`, `provider_id`), 
                        FOREIGN KEY(`provider_id`) REFERENCES `providers`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                """)
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_models_provider_id` ON `models` (`provider_id`)")
                
                // Also insert providers during migration if migrating from 2
                db.execSQL("INSERT OR IGNORE INTO providers (id, display_name, last_synced_at) VALUES ('groq', 'Groq', 0)")
                db.execSQL("INSERT OR IGNORE INTO providers (id, display_name, last_synced_at) VALUES ('openrouter', 'OpenRouter', 0)")
                db.execSQL("INSERT OR IGNORE INTO providers (id, display_name, last_synced_at) VALUES ('zai', 'ZAI', 0)")
            }
        }
    }
}