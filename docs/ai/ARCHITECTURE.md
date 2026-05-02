# Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────┐
│                    Jetpack Compose UI                    │
│  MainScreen · TranscriptionDetailScreen · SettingsScreen │
└──────────────────────────┬──────────────────────────────┘
                           │ hiltViewModel()
┌──────────────────────────▼──────────────────────────────┐
│                      ViewModels                          │
│  MainViewModel · TranscriptionDetailViewModel · Settings │
└──────────────────────────┬──────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────┐
│                    Repositories                          │
│  TranscriptionRepository · ProviderModelRepository      │
│  LanguageRepository · AppPreferencesRepository          │
└────────┬───────────────────────────────────┬────────────┘
         │                                   │
┌────────▼──────────┐               ┌────────▼────────────┐
│   Room (SQLite)    │               │  Retrofit + OkHttp   │
│  aitranscribe.db   │               │  GROQ · OpenRouter   │
│  v14               │               │  ZAI                 │
└───────────────────┘               └──────────────────────┘
```

### Background Services

- **RecordingService** (foreground) — captures audio via MediaRecorder to `.m4a`
- **TranscriptionWorker** (WorkManager) — uploads audio to STT, calls LLM for cleanup/summary

### Data Flow

```
User taps Record
  → RecordingService (foreground, notification)
  → MediaRecorder → .m4a in filesDir/recordings
  → Stop → LocalBroadcast → MainViewModel
  → Room insert (stt_text=null, audio_file_path=path)
  → Enqueue TranscriptionWorker
  → Worker: STT upload → atomically set stt_text + clear audio_file_path
  → Optional: LLM cleanup → set cleaned_text
  → Optional: LLM summary → set summary
  → UI observes Room Flow, updates automatically
```

### Dependency Injection

Hilt modules in `di/`:
- `DatabaseModule` — provides Room database, DAOs
- `NetworkModule` — provides Retrofit instances for GROQ, OpenRouter, ZAI
- `RepositoryModule` — binds repository interfaces to implementations

---

## Database Schema

**Engine:** Room (SQLite)  
**Version:** 17  
**File:** `aitranscribe.db`  
**Canonical schema:** `prisma/desired/schema.prisma`

### Entity-Relationship Diagram

```
providers ──1:N──> models

languages ──1:N──> transcriptions
                     │
                app_preferences (standalone key-value)
```

### Tables

#### `transcriptions`

| Column           | Type      | Nullable | Default | Notes                       |
|------------------|-----------|----------|---------|-----------------------------|
| `id`             | INTEGER   | no       | auto    | Primary key                 |
| `stt_text`       | TEXT      | yes      |         | Raw STT output              |
| `cleaned_text`   | TEXT      | yes      |         | LLM-processed text          |
| `audio_file_path`| TEXT      | yes      |         | Cleared after STT success   |
| `created_at`     | TEXT      | no       |         | ISO-8601 datetime string    |
| `error_message`  | TEXT      | yes      |         |                             |
| `seen`           | INTEGER   | no       | 0       | Boolean                     |
| `summary`        | TEXT      | yes      |         | LLM-generated summary       |
| `languagesId`    | TEXT      | yes      |         | FK → languages.id (SET NULL)|

**Invariants:**
- Unfinished STT: `stt_text IS NULL AND audio_file_path IS NOT NULL`
- STT success is atomic: set `stt_text`, clear `audio_file_path` in one statement

#### `providers`

| Column          | Type    | Nullable | Default | Notes           |
|-----------------|---------|----------|---------|-----------------|
| `id`            | TEXT    | no       |         | Primary key     |
| `name`          | TEXT    | no       |         | Display name    |
| `last_synced_at`| INTEGER | no       | 0       | Unix timestamp  |
| `api_token`     | TEXT    | yes      |         | Provider API key|

**Prepopulated:** `groq`, `openrouter`, `zai`

#### `models`

| Column        | Type    | Nullable | Default | Notes                        |
|---------------|---------|----------|---------|------------------------------|
| `id`          | INTEGER | no       | auto    | Primary key (surrogate)      |
| `external_id` | TEXT    | no       |         | Provider-facing model ID     |
| `provider_id` | TEXT    | no       |         | FK → providers.id (CASCADE)  |
| `model_name`  | TEXT    | no       |         | Human-readable name          |

**Unique constraint:** (`provider_id`, `external_id`)

#### `languages`

| Column       | Type    | Nullable | Default | Notes            |
|--------------|---------|----------|---------|------------------|
| `id`         | TEXT    | no       |         | Primary key (BCP) |
| `name`       | TEXT    | no       |         | English name      |
| `native_name`| TEXT    | yes      |         | Endonym           |
| `is_active`  | INTEGER | no       | 1       | Boolean           |

**Prepopulated:** 37 languages (de, en, fr, es, it, pt, nl, pl, ru, ja, zh, ko, ar, hi, tr, sv, da, no, fi, cs, hu, ro, el, he, th, vi, id, ms, uk, bg, hr, sr, sk, sl, lt, lv, et)

#### `app_preferences`

| Column      | Type    | Nullable | Default | Notes       |
|-------------|---------|----------|---------|-------------|
| `key`       | TEXT    | no       |         | Primary key |
| `value`     | TEXT    | no       |         |             |
| `updated_at`| TEXT    | no       |         | ISO-8601    |

---

## Entity-to-Table Mapping

| Entity class               | Table               | Source file            |
|----------------------------|---------------------|------------------------|
| `TranscriptionEntity`      | transcriptions      | data/local/            |
| `ProviderEntity`           | providers           | data/local/            |
| `ModelEntity`              | models              | data/local/            |
| `LanguageEntity`           | languages           | data/local/            |
| `AppPreferenceEntity`      | app_preferences     | data/local/            |

All entities in `app/src/main/java/com/georgernstgraf/aitranscribe/data/local/`.

---

## Migration History

| Version | Change | Key Details |
|---------|--------|-------------|
| 1→2     | Add retry_count, summary to transcriptions | |
| 2→3     | Create providers + models tables | Prepopulate groq, openrouter, zai |
| 3→4     | Add status, error_message to queued_transcriptions | |
| 4→5     | Merge queued_transcriptions into transcriptions | Add stt_model, llm_model columns; drop queued_transcriptions |
| 5→6     | Normalize models; add capabilities + model_capabilities | Models get surrogate PK; capabilities extracted from JSON |
| 6→7     | Rename providers.display_name → name; add api_token | Schema rebuild with FK off/on |
| 7→8     | Create app_preferences table | |
| 8→9     | Rebuild transcriptions: replace played_count with seen | Drop old columns |
| 9→10    | Remove stt_model, llm_model, post_processing_type, retry_count | Simplify schema |
| 10→11   | Make original_text nullable | Rows with audio but no text = pending |
| 11→12   | Merge original_text + processed_text → single text column | Add language column |
| 12→13   | Add language TEXT column to transcriptions | |
| 13→14   | Create languages table; rebuild transcriptions with FK to languages | Split text → stt_text + cleaned_text; drop status column |
| 16→17   | Remove capabilities + model_capabilities tables | Dead code — written but never read |

---

## Source Structure

```
app/src/main/java/com/georgernstgraf/aitranscribe/
├── data/
│   ├── local/          # Room entities, DAOs, TranscriptionDatabase, migrations
│   ├── remote/         # Retrofit API interfaces, DTOs (GROQ, OpenRouter, ZAI)
│   ├── repository/     # Repository interfaces + implementations
│   └── testing/        # FakeTranscriptionRepository for tests
├── di/                 # Hilt modules (Database, Network, Repository)
├── domain/model/       # Domain data classes (Transcription, etc.)
├── service/            # RecordingService, TranscriptionWorker
└── ui/
    ├── screen/         # Compose screens + MainActivity
    ├── viewmodel/      # ViewModels
    ├── navigation/     # NavHost, routes
    └── theme/          # Material3 theme
```

## API Endpoints

| Provider   | Purpose | Base URL                                        |
|------------|---------|-------------------------------------------------|
| GROQ       | STT     | `https://api.groq.com/openai/v1/audio/`        |
| OpenRouter | LLM     | `https://openrouter.ai/api/v1/chat/completions` |
| ZAI        | LLM     | `https://api.z.ai/api/paas/v4/chat/completions` |
