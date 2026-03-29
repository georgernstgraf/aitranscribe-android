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
- Use `flatMapLatest` for reactive chains that switch between data sources (e.g., paging through transcriptions)

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

## Testing
- Use `StandardTestDispatcher` with `setMain` for ViewModel tests that have infinite Flow collectors
- Never use `advanceUntilIdle()` with ViewModels that collect Flows in `init` — it hangs forever
- Use `runBlocking` + `.value` (not `first()`) to read StateFlow in ViewModel tests
- Repository-level tests use `runBlocking` with `FakeTranscriptionRepository`
- Run tests in background with `nohup` + `tail` to avoid CLI timeouts

## Trunk-Based Workflow
- Always run `git pull` before starting any new work to avoid conflicts on main.
- Do not create feature branches; commit directly to main.

## Build & Deploy
- Build: `./gradlew assembleDebug`
- **Auto-deploy:** After every successful build, always run `adb devices` to check for a connected physical device. If one is present, immediately run `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Do not wait for the user to ask.
- Install preserving data: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- Never use `adb uninstall` + `adb install` (loses API keys and preferences)
- Device package name for `run-as`: `com.georgernstgraf.aitranscribe.debug` (debug build has `.debug` suffix)
- No `sqlite3` binary on device — use `run-as` + file ops or log-based debugging for DB inspection

## Companion Project
- `../aitranscribe` (Python/TUI) is the **lead and authoritative project** for pipeline logic, prompts, and feature design
- This Android app is the companion/follower — prompts, modes, and workflow must mirror the Python project
- Key file in companion: `core.py` (LLM call function), `main.py` (prompts, modes, pipeline), `tui.py` (UI behavior)
- When adding or changing post-processing behavior, always check `../aitranscribe/main.py` first

## Testing Strategy

### Test Pyramid
```
     /  UI Tests  \         ← Critical flows only (record→transcribe, settings, search)
    /  Integration \        ← DAOs, Services, Workers (androidTest)
   /    Unit Tests   \      ← ViewModels, UseCases, Repositories (test/)
  /____________________\
```

### ViewModel Testing Rules
- Use `StandardTestDispatcher` + `setMain()` — never `UnconfinedTestDispatcher`
- Never call `advanceUntilIdle()` — infinite Flow collectors will hang
- Use `runBlocking` instead of `runTest` for ViewModels with infinite collectors
- Read `.value` directly on `StateFlow` for assertions
- Use `FakeTranscriptionRepository` from `data/testing/`

### Known Testing Pitfalls
- See `docs/ai/PITFALLS.md` for the full list
- Key: infinite `collect {}` in ViewModel init hangs `runTest`/`advanceUntilIdle()`
- Key: Hilt stale builds require `./gradlew clean` before instrumentation tests
