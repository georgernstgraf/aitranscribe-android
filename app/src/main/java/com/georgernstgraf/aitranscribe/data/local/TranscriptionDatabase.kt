package com.georgernstgraf.aitranscribe.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TranscriptionEntity::class,
        ProviderEntity::class,
        ModelEntity::class,
        CapabilityEntity::class,
        ModelCapabilityEntity::class,
        AppPreferenceEntity::class,
        LanguageEntity::class
    ],
    version = 16,
    exportSchema = false
)
abstract class TranscriptionDatabase : RoomDatabase() {

    abstract fun transcriptionDao(): TranscriptionDao
    abstract fun providerModelDao(): ProviderModelDao
    abstract fun appPreferencesDao(): AppPreferencesDao
    abstract fun languageDao(): LanguageDao

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
                    .addMigrations(
                        MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5,
                        MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9,
                        MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13,
                        MIGRATION_13_14, MIGRATION_14_15, MIGRATION_15_16
                    )
                    .addCallback(ProviderPrepopulateCallback())
                    .addCallback(LanguagePrepopulateCallback())
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class ProviderPrepopulateCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("INSERT INTO providers (id, name, last_synced_at, api_token) VALUES ('groq', 'Groq', 0, NULL)")
                db.execSQL("INSERT INTO providers (id, name, last_synced_at, api_token) VALUES ('openrouter', 'OpenRouter', 0, NULL)")
                db.execSQL("INSERT INTO providers (id, name, last_synced_at, api_token) VALUES ('zai', 'ZAI', 0, NULL)")
            }
        }

        private class LanguagePrepopulateCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedLanguages(db)
            }
        }

        private fun seedLanguages(db: SupportSQLiteDatabase) {
            val active = listOf(
                Triple("de", "German", "Deutsch"),
                Triple("en", "English", "English"),
                Triple("fr", "French", "Français"),
                Triple("es", "Spanish", "Español"),
                Triple("it", "Italian", "Italiano"),
                Triple("pt", "Portuguese", "Português"),
                Triple("nl", "Dutch", "Nederlands"),
                Triple("pl", "Polish", "Polski"),
                Triple("ru", "Russian", "Русский"),
                Triple("ja", "Japanese", "日本語"),
                Triple("zh", "Chinese", "中文"),
                Triple("ko", "Korean", "한국어"),
                Triple("ar", "Arabic", "العربية"),
                Triple("hi", "Hindi", "हिन्दी"),
                Triple("tr", "Turkish", "Türkçe"),
                Triple("sv", "Swedish", "Svenska"),
                Triple("da", "Danish", "Dansk"),
                Triple("no", "Norwegian", "Norsk"),
                Triple("fi", "Finnish", "Suomi"),
                Triple("cs", "Czech", "Čeština"),
                Triple("hu", "Hungarian", "Magyar"),
                Triple("ro", "Romanian", "Română"),
                Triple("el", "Greek", "Ελληνικά"),
                Triple("he", "Hebrew", "עברית"),
                Triple("th", "Thai", "ไทย"),
                Triple("vi", "Vietnamese", "Tiếng Việt"),
                Triple("id", "Indonesian", "Bahasa Indonesia"),
                Triple("ms", "Malay", "Bahasa Melayu"),
                Triple("uk", "Ukrainian", "Українська"),
                Triple("bg", "Bulgarian", "Български"),
                Triple("hr", "Croatian", "Hrvatski"),
                Triple("sr", "Serbian", "Српски"),
                Triple("sk", "Slovak", "Slovenčina"),
                Triple("sl", "Slovenian", "Slovenščina"),
                Triple("lt", "Lithuanian", "Lietuvių"),
                Triple("lv", "Latvian", "Latviešu"),
                Triple("et", "Estonian", "Eesti")
            )
            val inactive = listOf(
                Triple("ca", "Catalan", "Català"),
                Triple("ta", "Tamil", "தமிழ்"),
                Triple("ur", "Urdu", "اردو"),
                Triple("la", "Latin", "Latina"),
                Triple("mi", "Maori", "Te Reo Māori"),
                Triple("ml", "Malayalam", "മലയാളം"),
                Triple("cy", "Welsh", "Cymraeg"),
                Triple("te", "Telugu", "తెలుగు"),
                Triple("fa", "Persian", "فارسی"),
                Triple("bn", "Bengali", "বাংলা"),
                Triple("az", "Azerbaijani", "Azərbaycan"),
                Triple("kn", "Kannada", "ಕನ್ನಡ"),
                Triple("mk", "Macedonian", "Македонски"),
                Triple("br", "Breton", "Brezhoneg"),
                Triple("eu", "Basque", "Euskara"),
                Triple("is", "Icelandic", "Íslenska"),
                Triple("hy", "Armenian", "Հայերեն"),
                Triple("ne", "Nepali", "नेपाली"),
                Triple("mn", "Mongolian", "Монгол"),
                Triple("bs", "Bosnian", "Bosanski"),
                Triple("kk", "Kazakh", "Қазақ"),
                Triple("sq", "Albanian", "Shqip"),
                Triple("sw", "Swahili", "Kiswahili"),
                Triple("gl", "Galician", "Galego"),
                Triple("mr", "Marathi", "मराठी"),
                Triple("pa", "Punjabi", "ਪੰਜਾਬੀ"),
                Triple("si", "Sinhala", "සිංහල"),
                Triple("km", "Khmer", "ខ្មែរ"),
                Triple("sn", "Shona", "chiShona"),
                Triple("yo", "Yoruba", "Yorùbá"),
                Triple("so", "Somali", "Soomaali"),
                Triple("af", "Afrikaans", "Afrikaans"),
                Triple("oc", "Occitan", "Occitan"),
                Triple("ka", "Georgian", "ქართული"),
                Triple("be", "Belarusian", "Беларуская"),
                Triple("tg", "Tajik", "Тоҷикӣ"),
                Triple("sd", "Sindhi", "سنڌي"),
                Triple("gu", "Gujarati", "ગુજરાતી"),
                Triple("am", "Amharic", "አማርኛ"),
                Triple("yi", "Yiddish", "ייִדיש"),
                Triple("lo", "Lao", "ລາວ"),
                Triple("uz", "Uzbek", "Oʻzbek"),
                Triple("fo", "Faroese", "Føroyskt"),
                Triple("ht", "Haitian Creole", "Kreyòl Ayisyen"),
                Triple("ps", "Pashto", "پښتو"),
                Triple("tk", "Turkmen", "Türkmen"),
                Triple("nn", "Nynorsk", "Nynorsk"),
                Triple("mt", "Maltese", "Malti"),
                Triple("sa", "Sanskrit", "संस्कृतम्"),
                Triple("lb", "Luxembourgish", "Lëtzebuergesch"),
                Triple("my", "Myanmar", "မြန်မာ"),
                Triple("bo", "Tibetan", "བོད་སྐད"),
                Triple("tl", "Tagalog", "Tagalog"),
                Triple("mg", "Malagasy", "Malagasy"),
                Triple("as", "Assamese", "অসমীয়া"),
                Triple("tt", "Tatar", "Татар"),
                Triple("haw", "Hawaiian", "ʻŌlelo Hawaiʻi"),
                Triple("ln", "Lingala", "Lingála"),
                Triple("ha", "Hausa", "Hausa"),
                Triple("ba", "Bashkir", "Башҡорт"),
                Triple("jw", "Javanese", "Basa Jawa"),
                Triple("su", "Sundanese", "Basa Sunda"),
                Triple("yue", "Cantonese", "粵語")
            )
            active.forEach { (id, name, nativeName) ->
                db.execSQL(
                    "INSERT INTO languages (id, name, native_name, is_active) VALUES (?, ?, ?, 1)",
                    arrayOf(id, name, nativeName)
                )
            }
            inactive.forEach { (id, name, nativeName) ->
                db.execSQL(
                    "INSERT INTO languages (id, name, native_name, is_active) VALUES (?, ?, ?, 0)",
                    arrayOf(id, name, nativeName)
                )
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
                db.execSQL("INSERT OR IGNORE INTO providers (id, display_name, last_synced_at) VALUES ('groq', 'Groq', 0)")
                db.execSQL("INSERT OR IGNORE INTO providers (id, display_name, last_synced_at) VALUES ('openrouter', 'OpenRouter', 0)")
                db.execSQL("INSERT OR IGNORE INTO providers (id, display_name, last_synced_at) VALUES ('zai', 'ZAI', 0)")
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE queued_transcriptions ADD COLUMN status TEXT NOT NULL DEFAULT 'PENDING'")
                db.execSQL("ALTER TABLE queued_transcriptions ADD COLUMN error_message TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transcriptions ADD COLUMN stt_model TEXT")
                db.execSQL("ALTER TABLE transcriptions ADD COLUMN llm_model TEXT")
                db.execSQL(
                    """
                    INSERT INTO transcriptions (
                        original_text, processed_text, audio_file_path, stt_model, llm_model,
                        created_at, post_processing_type, status, error_message, played_count, retry_count, summary
                    )
                    SELECT '', NULL, audioFilePath, sttModel, llmModel, created_at,
                        postProcessingType, status, error_message, 0, 0, NULL
                    FROM queued_transcriptions
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS queued_transcriptions")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `models_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `provider_id` TEXT NOT NULL,
                        `external_id` TEXT NOT NULL,
                        `model_name` TEXT NOT NULL,
                        FOREIGN KEY(`provider_id`) REFERENCES `providers`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `models_new` (`provider_id`, `external_id`, `model_name`)
                    SELECT `provider_id`, `id`, `model_name` FROM `models`
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `capabilities` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `model_capabilities_seed` (
                        `model_id` INTEGER NOT NULL,
                        `capability_id` TEXT NOT NULL,
                        `source` TEXT,
                        PRIMARY KEY(`model_id`, `capability_id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `capabilities` (`id`, `name`)
                    SELECT DISTINCT
                        'modality:' || TRIM(SUBSTR(
                            SUBSTR(`m`.`capabilities`, INSTR(`m`.`capabilities`, '"modality":"') + LENGTH('"modality":"')),
                            1,
                            INSTR(SUBSTR(`m`.`capabilities`, INSTR(`m`.`capabilities`, '"modality":"') + LENGTH('"modality":"')), '"') - 1
                        )),
                        'Modality: ' || TRIM(SUBSTR(
                            SUBSTR(`m`.`capabilities`, INSTR(`m`.`capabilities`, '"modality":"') + LENGTH('"modality":"')),
                            1,
                            INSTR(SUBSTR(`m`.`capabilities`, INSTR(`m`.`capabilities`, '"modality":"') + LENGTH('"modality":"')), '"') - 1
                        ))
                    FROM `models` `m`
                    WHERE `m`.`capabilities` IS NOT NULL
                      AND INSTR(`m`.`capabilities`, '"modality":"') > 0
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `capabilities` (`id`, `name`)
                    SELECT DISTINCT
                        'instruct_type:' || TRIM(SUBSTR(
                            SUBSTR(`m`.`capabilities`, INSTR(`m`.`capabilities`, '"instruct_type":"') + LENGTH('"instruct_type":"')),
                            1,
                            INSTR(SUBSTR(`m`.`capabilities`, INSTR(`m`.`capabilities`, '"instruct_type":"') + LENGTH('"instruct_type":"')), '"') - 1
                        )),
                        'Instruct Type: ' || TRIM(SUBSTR(
                            SUBSTR(`m`.`capabilities`, INSTR(`m`.`capabilities`, '"instruct_type":"') + LENGTH('"instruct_type":"')),
                            1,
                            INSTR(SUBSTR(`m`.`capabilities`, INSTR(`m`.`capabilities`, '"instruct_type":"') + LENGTH('"instruct_type":"')), '"') - 1
                        ))
                    FROM `models` `m`
                    WHERE `m`.`capabilities` IS NOT NULL
                      AND INSTR(`m`.`capabilities`, '"instruct_type":"') > 0
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `model_capabilities_seed` (`model_id`, `capability_id`, `source`)
                    SELECT `mn`.`id`, 'modality:' || TRIM(SUBSTR(
                            SUBSTR(`m`.`capabilities`, INSTR(`m`.`capabilities`, '"modality":"') + LENGTH('"modality":"')),
                            1,
                            INSTR(SUBSTR(`m`.`capabilities`, INSTR(`m`.`capabilities`, '"modality":"') + LENGTH('"modality":"')), '"') - 1
                        )), 'legacy_json'
                    FROM `models` `m`
                    JOIN `models_new` `mn` ON `mn`.`provider_id` = `m`.`provider_id` AND `mn`.`external_id` = `m`.`id`
                    WHERE `m`.`capabilities` IS NOT NULL AND INSTR(`m`.`capabilities`, '"modality":"') > 0
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `model_capabilities_seed` (`model_id`, `capability_id`, `source`)
                    SELECT `mn`.`id`, 'instruct_type:' || TRIM(SUBSTR(
                            SUBSTR(`m`.`capabilities`, INSTR(`m`.`capabilities`, '"instruct_type":"') + LENGTH('"instruct_type":"')),
                            1,
                            INSTR(SUBSTR(`m`.`capabilities`, INSTR(`m`.`capabilities`, '"instruct_type":"') + LENGTH('"instruct_type":"')), '"') - 1
                        )), 'legacy_json'
                    FROM `models` `m`
                    JOIN `models_new` `mn` ON `mn`.`provider_id` = `m`.`provider_id` AND `mn`.`external_id` = `m`.`id`
                    WHERE `m`.`capabilities` IS NOT NULL AND INSTR(`m`.`capabilities`, '"instruct_type":"') > 0
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS `models`")
                db.execSQL("ALTER TABLE `models_new` RENAME TO `models`")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `model_capabilities` (
                        `model_id` INTEGER NOT NULL,
                        `capability_id` TEXT NOT NULL,
                        `source` TEXT,
                        PRIMARY KEY(`model_id`, `capability_id`),
                        FOREIGN KEY(`model_id`) REFERENCES `models`(`id`) ON UPDATE CASCADE ON DELETE CASCADE,
                        FOREIGN KEY(`capability_id`) REFERENCES `capabilities`(`id`) ON UPDATE CASCADE ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR IGNORE INTO `model_capabilities` (`model_id`, `capability_id`, `source`)
                    SELECT `model_id`, `capability_id`, `source` FROM `model_capabilities_seed`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS `model_capabilities_seed`")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_models_provider_id_external_id` ON `models` (`provider_id`, `external_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_models_provider_id` ON `models` (`provider_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_models_external_id` ON `models` (`external_id`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_models_provider_id_model_name` ON `models` (`provider_id`, `model_name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_models_model_name` ON `models` (`model_name`)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_capabilities_name` ON `capabilities` (`name`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_model_capabilities_capability_id` ON `model_capabilities` (`capability_id`)")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("PRAGMA foreign_keys=OFF")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `providers_new` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `last_synced_at` INTEGER NOT NULL,
                        `api_token` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `providers_new` (`id`, `name`, `last_synced_at`, `api_token`)
                    SELECT `id`, `display_name`, `last_synced_at`, NULL FROM `providers`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS `providers`")
                db.execSQL("ALTER TABLE `providers_new` RENAME TO `providers`")
                db.execSQL("PRAGMA foreign_keys=ON")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `app_preferences` (
                        `key` TEXT NOT NULL,
                        `value` TEXT NOT NULL,
                        `updated_at` TEXT NOT NULL,
                        PRIMARY KEY(`key`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transcriptions_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `original_text` TEXT NOT NULL,
                        `processed_text` TEXT,
                        `audio_file_path` TEXT,
                        `stt_model` TEXT,
                        `llm_model` TEXT,
                        `created_at` TEXT NOT NULL,
                        `post_processing_type` TEXT,
                        `status` TEXT NOT NULL,
                        `error_message` TEXT,
                        `seen` INTEGER NOT NULL DEFAULT 0,
                        `retry_count` INTEGER NOT NULL DEFAULT 0,
                        `summary` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `transcriptions_new` (
                        `id`, `original_text`, `processed_text`, `audio_file_path`, `stt_model`, `llm_model`,
                        `created_at`, `post_processing_type`, `status`, `error_message`, `seen`, `retry_count`, `summary`
                    )
                    SELECT `id`, `original_text`, `processed_text`, `audio_file_path`, `stt_model`, `llm_model`,
                        `created_at`, `post_processing_type`, `status`, `error_message`,
                        CASE WHEN `played_count` > 0 THEN 1 ELSE 0 END, `retry_count`, `summary`
                    FROM `transcriptions`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS `transcriptions`")
                db.execSQL("ALTER TABLE `transcriptions_new` RENAME TO `transcriptions`")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transcriptions_new2` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `original_text` TEXT NOT NULL,
                        `processed_text` TEXT,
                        `audio_file_path` TEXT,
                        `created_at` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `error_message` TEXT,
                        `seen` INTEGER NOT NULL DEFAULT 0,
                        `summary` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `transcriptions_new2` (
                        `id`, `original_text`, `processed_text`, `audio_file_path`,
                        `created_at`, `status`, `error_message`, `seen`, `summary`
                    )
                    SELECT `id`, `original_text`, `processed_text`, `audio_file_path`,
                        `created_at`, `status`, `error_message`, `seen`, `summary`
                    FROM `transcriptions`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS `transcriptions`")
                db.execSQL("ALTER TABLE `transcriptions_new2` RENAME TO `transcriptions`")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transcriptions_new3` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `original_text` TEXT,
                        `processed_text` TEXT,
                        `audio_file_path` TEXT,
                        `created_at` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `error_message` TEXT,
                        `seen` INTEGER NOT NULL DEFAULT 0,
                        `summary` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `transcriptions_new3` (
                        `id`, `original_text`, `processed_text`, `audio_file_path`,
                        `created_at`, `status`, `error_message`, `seen`, `summary`
                    )
                    SELECT `id`,
                        CASE WHEN `audio_file_path` IS NOT NULL AND TRIM(COALESCE(`original_text`, '')) = '' THEN NULL
                            ELSE `original_text` END,
                        `processed_text`, `audio_file_path`, `created_at`, `status`, `error_message`, `seen`, `summary`
                    FROM `transcriptions`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS `transcriptions`")
                db.execSQL("ALTER TABLE `transcriptions_new3` RENAME TO `transcriptions`")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transcriptions_new4` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `text` TEXT,
                        `audio_file_path` TEXT,
                        `created_at` TEXT NOT NULL,
                        `status` TEXT NOT NULL,
                        `error_message` TEXT,
                        `seen` INTEGER NOT NULL DEFAULT 0,
                        `summary` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `transcriptions_new4` (
                        `id`, `text`, `audio_file_path`, `created_at`, `status`, `error_message`, `seen`, `summary`
                    )
                    SELECT `id`, COALESCE(`processed_text`, `original_text`) AS `text`,
                        `audio_file_path`, `created_at`, `status`, `error_message`, `seen`, `summary`
                    FROM `transcriptions`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE IF EXISTS `transcriptions`")
                db.execSQL("ALTER TABLE `transcriptions_new4` RENAME TO `transcriptions`")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE transcriptions ADD COLUMN language TEXT")
            }
        }

        val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create languages table
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS languages (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        native_name TEXT,
                        is_active INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )

                // 2. Seed 34 languages
                seedLanguages(db)

                // 3. Create new transcriptions table with split columns and FK
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS transcriptions_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        stt_text TEXT,
                        cleaned_text TEXT,
                        audio_file_path TEXT,
                        created_at TEXT NOT NULL,
                        error_message TEXT,
                        seen INTEGER NOT NULL DEFAULT 0,
                        summary TEXT,
                        languagesId TEXT,
                        FOREIGN KEY(languagesId) REFERENCES languages(id) ON UPDATE CASCADE ON DELETE SET NULL
                    )
                    """.trimIndent()
                )

                // 4. Migrate data: text -> stt_text, cleaned_text = NULL, drop status
                db.execSQL(
                    """
                    INSERT INTO transcriptions_new (id, stt_text, cleaned_text, audio_file_path, created_at, error_message, seen, summary, languagesId)
                    SELECT id, text, NULL, audio_file_path, created_at, error_message, seen, summary, language
                    FROM transcriptions
                    """.trimIndent()
                )

                // 5. Drop old table, rename new
                db.execSQL("DROP TABLE IF EXISTS transcriptions")
                db.execSQL("ALTER TABLE transcriptions_new RENAME TO transcriptions")

                // 6. Create index for FK
                db.execSQL("CREATE INDEX IF NOT EXISTS index_transcriptions_languagesId ON transcriptions (languagesId)")
            }
        }

        val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val mappings = listOf(
                    "english" to "en", "chinese" to "zh", "german" to "de",
                    "spanish" to "es", "russian" to "ru", "korean" to "ko",
                    "french" to "fr", "japanese" to "ja", "portuguese" to "pt",
                    "turkish" to "tr", "polish" to "pl", "catalan" to "ca",
                    "dutch" to "nl", "arabic" to "ar", "swedish" to "sv",
                    "italian" to "it", "indonesian" to "id", "hindi" to "hi",
                    "finnish" to "fi", "vietnamese" to "vi", "hebrew" to "he",
                    "ukrainian" to "uk", "greek" to "el", "malay" to "ms",
                    "czech" to "cs", "romanian" to "ro", "danish" to "da",
                    "hungarian" to "hu", "tamil" to "ta", "norwegian" to "no",
                    "thai" to "th", "urdu" to "ur", "croatian" to "hr",
                    "bulgarian" to "bg", "lithuanian" to "lt", "latin" to "la",
                    "maori" to "mi", "malayalam" to "ml", "welsh" to "cy",
                    "slovak" to "sk", "telugu" to "te", "persian" to "fa",
                    "latvian" to "lv", "bengali" to "bn", "serbian" to "sr",
                    "azerbaijani" to "az", "slovenian" to "sl", "kannada" to "kn",
                    "estonian" to "et", "macedonian" to "mk", "breton" to "br",
                    "basque" to "eu", "icelandic" to "is", "armenian" to "hy",
                    "nepali" to "ne", "mongolian" to "mn", "bosnian" to "bs",
                    "kazakh" to "kk", "albanian" to "sq", "swahili" to "sw",
                    "galician" to "gl", "marathi" to "mr", "punjabi" to "pa",
                    "sinhala" to "si", "khmer" to "km", "shona" to "sn",
                    "yoruba" to "yo", "somali" to "so", "afrikaans" to "af",
                    "occitan" to "oc", "georgian" to "ka", "belarusian" to "be",
                    "tajik" to "tg", "sindhi" to "sd", "gujarati" to "gu",
                    "amharic" to "am", "yiddish" to "yi", "lao" to "lo",
                    "uzbek" to "uz", "faroese" to "fo", "haitian creole" to "ht",
                    "pashto" to "ps", "turkmen" to "tk", "nynorsk" to "nn",
                    "maltese" to "mt", "sanskrit" to "sa", "luxembourgish" to "lb",
                    "myanmar" to "my", "tibetan" to "bo", "tagalog" to "tl",
                    "malagasy" to "mg", "assamese" to "as", "tatar" to "tt",
                    "hawaiian" to "haw", "lingala" to "ln", "hausa" to "ha",
                    "bashkir" to "ba", "javanese" to "jw", "sundanese" to "su",
                    "cantonese" to "yue"
                )
                mappings.forEach { (name, code) ->
                    db.execSQL("UPDATE transcriptions SET languagesId = ? WHERE languagesId = ?", arrayOf(code, name))
                }
                db.execSQL(
                    """
                    DELETE FROM languages
                    WHERE native_name IS NULL
                      AND id NOT IN (SELECT DISTINCT languagesId FROM transcriptions WHERE languagesId IS NOT NULL)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_15_16 = object : Migration(15, 16) {
            override fun migrate(db: SupportSQLiteDatabase) {
                val inactiveLanguages = listOf(
                    Triple("ca", "Catalan", "Català"),
                    Triple("ta", "Tamil", "தமிழ்"),
                    Triple("ur", "Urdu", "اردو"),
                    Triple("la", "Latin", "Latina"),
                    Triple("mi", "Maori", "Te Reo Māori"),
                    Triple("ml", "Malayalam", "മലയാളം"),
                    Triple("cy", "Welsh", "Cymraeg"),
                    Triple("te", "Telugu", "తెలుగు"),
                    Triple("fa", "Persian", "فارسی"),
                    Triple("bn", "Bengali", "বাংলা"),
                    Triple("az", "Azerbaijani", "Azərbaycan"),
                    Triple("kn", "Kannada", "ಕನ್ನಡ"),
                    Triple("mk", "Macedonian", "Македонски"),
                    Triple("br", "Breton", "Brezhoneg"),
                    Triple("eu", "Basque", "Euskara"),
                    Triple("is", "Icelandic", "Íslenska"),
                    Triple("hy", "Armenian", "Հայերեն"),
                    Triple("ne", "Nepali", "नेपाली"),
                    Triple("mn", "Mongolian", "Монгол"),
                    Triple("bs", "Bosnian", "Bosanski"),
                    Triple("kk", "Kazakh", "Қазақ"),
                    Triple("sq", "Albanian", "Shqip"),
                    Triple("sw", "Swahili", "Kiswahili"),
                    Triple("gl", "Galician", "Galego"),
                    Triple("mr", "Marathi", "मराठी"),
                    Triple("pa", "Punjabi", "ਪੰਜਾਬੀ"),
                    Triple("si", "Sinhala", "සිංහල"),
                    Triple("km", "Khmer", "ខ្មែរ"),
                    Triple("sn", "Shona", "chiShona"),
                    Triple("yo", "Yoruba", "Yorùbá"),
                    Triple("so", "Somali", "Soomaali"),
                    Triple("af", "Afrikaans", "Afrikaans"),
                    Triple("oc", "Occitan", "Occitan"),
                    Triple("ka", "Georgian", "ქართული"),
                    Triple("be", "Belarusian", "Беларуская"),
                    Triple("tg", "Tajik", "Тоҷикӣ"),
                    Triple("sd", "Sindhi", "سنڌي"),
                    Triple("gu", "Gujarati", "ગુજરાતી"),
                    Triple("am", "Amharic", "አማርኛ"),
                    Triple("yi", "Yiddish", "ייִדיש"),
                    Triple("lo", "Lao", "ລາວ"),
                    Triple("uz", "Uzbek", "Oʻzbek"),
                    Triple("fo", "Faroese", "Føroyskt"),
                    Triple("ht", "Haitian Creole", "Kreyòl Ayisyen"),
                    Triple("ps", "Pashto", "پښتو"),
                    Triple("tk", "Turkmen", "Türkmen"),
                    Triple("nn", "Nynorsk", "Nynorsk"),
                    Triple("mt", "Maltese", "Malti"),
                    Triple("sa", "Sanskrit", "संस्कृतम्"),
                    Triple("lb", "Luxembourgish", "Lëtzebuergesch"),
                    Triple("my", "Myanmar", "မြန်မာ"),
                    Triple("bo", "Tibetan", "བོད་སྐད"),
                    Triple("tl", "Tagalog", "Tagalog"),
                    Triple("mg", "Malagasy", "Malagasy"),
                    Triple("as", "Assamese", "অসমীয়া"),
                    Triple("tt", "Tatar", "Татар"),
                    Triple("haw", "Hawaiian", "ʻŌlelo Hawaiʻi"),
                    Triple("ln", "Lingala", "Lingála"),
                    Triple("ha", "Hausa", "Hausa"),
                    Triple("ba", "Bashkir", "Башҡорт"),
                    Triple("jw", "Javanese", "Basa Jawa"),
                    Triple("su", "Sundanese", "Basa Sunda"),
                    Triple("yue", "Cantonese", "粵語")
                )
                inactiveLanguages.forEach { (id, name, nativeName) ->
                    db.execSQL(
                        "INSERT OR IGNORE INTO languages (id, name, native_name, is_active) VALUES (?, ?, ?, 0)",
                        arrayOf(id, name, nativeName)
                    )
                }
            }
        }
    }
}