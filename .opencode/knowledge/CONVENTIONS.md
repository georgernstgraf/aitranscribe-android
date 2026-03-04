# Coding Conventions

## Architecture
- **MVVM pattern:** Compose Screen -> ViewModel -> Repository -> (Room DB | Retrofit API)
- **Dependency Injection:** Hilt throughout; all ViewModels annotated `@HiltViewModel`
- **Background work:** WorkManager for API calls (TranscriptionWorker), foreground Service for recording
- **Communication:** RecordingService -> LocalBroadcast -> MainViewModel -> WorkManager enqueue

## Kotlin / Android
- ViewModels obtained via `hiltViewModel()` in Compose (never `viewModel()`)
- Room DAOs return `Flow<List<T>>` for observable queries, suspend functions for writes
- Coroutines for async work in ViewModels (viewModelScope)
- Repository pattern wraps both local (Room) and remote (Retrofit) data sources

## File Layout
```
app/src/main/java/com/georgernstgraf/aitranscribe/
  data/
    local/         # Room entities, DAOs, database
    remote/        # Retrofit API interfaces, DTOs
    repository/    # Repository interfaces + implementations
  di/              # Hilt modules
  service/         # RecordingService, TranscriptionWorker
  ui/
    screen/        # Compose screens, MainActivity
    viewmodel/     # ViewModels
```

## Naming
- Screens: `*Screen.kt` (e.g., `MainScreen.kt`, `TranscriptionDetailScreen.kt`)
- ViewModels: `*ViewModel.kt`
- DAOs: `*Dao.kt`
- Workers: `*Worker.kt`
- Services: `*Service.kt`
- DTOs: grouped in `ApiDtos.kt`

## API Integration
- GROQ Whisper API for transcription (multipart file upload)
- API key stored in app preferences, validated on setup screen
- Audio uploaded as multipart form data with model, language, and response_format parameters

## Logging
- Use `android.util.Log` with class-name tags (e.g., `Log.d("TranscriptionWorker", ...)`)
- Add logging at key pipeline stages: recording start/stop, file creation, API request/response, DB writes

## Build & Deploy
- Build: `./gradlew assembleDebug`
- Install preserving data: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- Never use `adb uninstall` + `adb install` (loses API keys and preferences)
