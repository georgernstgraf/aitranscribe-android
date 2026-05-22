# AITranscribe Android

Native Android app for speech-to-text transcription with AI-powered post-processing. Designed for F-Droid distribution — all dependencies are FOSS, no Google Play Services required.

## Features

- **Push-to-Talk Recording** — hold to record, release to transcribe; runs as a foreground service
- **Multi-Provider STT** — choose between [Groq](https://groq.com) (whisper-large-v3-turbo, whisper-large-v3) and [ZAI](https://z.ai) (GLM-ASR) for speech recognition
- **LLM Post-Processing** — automatically clean up or translate transcriptions via OpenRouter, Groq, or ZAI LLMs
- **Background Transcription** — recordings are transcribed via WorkManager; you can leave the app immediately
- **Offline Queue** — recordings are queued when offline and transcribed automatically when connectivity returns
- **View Status Tracking** — unviewed transcriptions are marked with a blue dot; auto-marked on open
- **Search & Filter** — filter by date range, text content, and view status (All / Only Unviewed)
- **Export / Import** — export transcription history to JSON or CSV; import from those formats
- **Model Sync** — model catalog is fetched periodically from each provider so you always see the latest available models
- **Language Detection** — detected language is stored per transcription; configure active languages per transcription
- **Dark / Light Theme** — follows system theme via Material 3

## Screens

| Screen | Purpose |
|---|---|
| **Setup** | First-launch wizard to enter Groq and OpenRouter API keys |
| **Main** | Recording button + list of recent transcriptions with search/settings toolbar |
| **Detail** | Full transcription text, post-processing actions (cleanup, translate), summary, share, delete |
| **Search** | Full-text search with date range and view filter |
| **Settings** | Provider management (auth/disconnect), STT/LLM model selection, language config, delete old transcriptions |
| **Connect Provider** | Add additional providers (Groq, OpenRouter, ZAI) |

## Architecture

Clean Architecture with three layers:

```
app/
├── data/           # Room database, Retrofit API clients, Repository implementations
│   ├── local/      # Room entities, DAOs, database, preferences
│   ├── remote/     # GroqApiService, OpenRouterApiService, ZaiApiService, NetworkModule
│   └── repository/ # TranscriptionRepository, AppPreferencesRepository
├── domain/         # Business logic (use cases), domain models, repository interfaces
│   ├── model/      # Transcription, ProviderConfig, ViewFilter, PostProcessingType, etc.
│   └── usecase/    # TranscribeAudioUseCase, PostProcessTextUseCase, SearchTranscriptionsUseCase, etc.
├── service/        # RecordingService (foreground), TranscriptionWorker (WorkManager), ModelSyncWorker
├── ui/             # Jetpack Compose screens, components, ViewModels, theme
│   ├── screen/     # MainActivity, MainScreen, SettingsScreen, SearchScreen, etc.
│   ├── components/ # BottomControlPanel, AudioRecordingButton, TranscriptionItem, etc.
│   ├── viewmodel/  # MainViewModel, SettingsViewModel, SearchViewModel, SetupViewModel
│   └── theme/      # Material 3 theme (Color, Theme, Type, Icon)
└── util/           # AudioChunker, NetworkMonitor, AppLogger, ToastManager
```

**Key libraries:** Jetpack Compose (Material 3), Room, Hilt, Retrofit + OkHttp, WorkManager, JUnit 5 + MockK + Turbine (testing).

## Build

### Prerequisites

- JDK 17+
- Android SDK 34
- Gradle (via wrapper)

### Commands

```bash
# Debug build (faster, no minification)
./gradlew assembleDebug

# Release build (minified, obfuscated)
./gradlew assembleRelease

# Release AAB (for distribution)
./gradlew bundleRelease

# Install on connected device
./gradlew installDebug
```

APK output: `app/build/outputs/apk/debug/app-debug.apk`

### Environment Variables for Tests

Copy `.env.example` to `.env` and fill in API keys for integration tests:

```bash
cp .env.example .env
# Edit .env with your keys
```

## Configuration

### API Keys

The app requires API keys for speech-to-text and (optionally) LLM post-processing:

| Purpose | Provider | Key Format | Get One |
|---|---|---|---|
| STT | Groq | `gsk_...` | [console.groq.com](https://console.groq.com) |
| STT | ZAI | API token | [z.ai](https://z.ai) |
| LLM | OpenRouter | `sk-or-...` | [openrouter.ai/keys](https://openrouter.ai/keys) |
| LLM | Groq | `gsk_...` | [console.groq.com](https://console.groq.com) |
| LLM | ZAI | API token | [z.ai](https://z.ai) |

Enter keys in the Setup screen on first launch, or later in Settings → Connect Provider. Keys are stored encrypted on-device via AndroidX Security (EncryptedSharedPreferences).

### Model Selection

In Settings you can choose separate providers and models for STT and LLM. Available models are synced from each provider's API and updated every 12 hours via `ModelSyncWorker`.

### STT Models

| Provider | Models |
|---|---|
| Groq | `whisper-large-v3-turbo`, `whisper-large-v3` |
| ZAI | `glm-asr-2512` |

### LLM Models (partial list)

| Provider | Models |
|---|---|
| OpenRouter | `inception/mercury`, `google/gemini-2.5-flash-lite`, `anthropic/claude-3-haiku`, `mistralai/mistral-small-3.1-24b-instruct`, ... |
| Groq | `llama-3.3-70b-versatile`, `llama-3.1-8b-instant`, `mixtral-8x7b-32768`, `gemma2-9b-it` |
| ZAI | `glm-4.7-flash`, `glm-4.5-flash`, `glm-4-32b-0414-128k`, ... |

The full model list is dynamic — synced from provider APIs on first connect and refreshed every 12 hours.

### Build Config

- **Min SDK:** 30 (Android 11)
- **Target SDK:** 34 (Android 14)
- **Debug build ID:** `com.georgernstgraf.aitranscribe.debug`
- **Release build ID:** `com.georgernstgraf.aitranscribe`

## Data Flow

```
Push & hold record button
    ↓
RecordingService (foreground) → MediaRecorder → .m4a file
    ↓  (release)
TranscriptionWorker (WorkManager)
    ├─ Groq / ZAI STT API → raw text + detected language
    └─ OpenRouter / Groq / ZAI LLM → cleaned text + summary
    ↓
Room database → transcription stored with status
    ↓
Notification: transcription complete
```

## Testing

```bash
# Unit tests (JUnit 5 + MockK + Turbine)
./gradlew test

# Instrumented tests (requires device/emulator)
./gradlew connectedAndroidTest
```

Integration tests against live APIs are gated by the `RUN_API_INTEGRATION_TESTS` env flag.

## License

FOSS — all dependencies are Apache 2.0, LGPL, or compatible. See individual library licenses.
