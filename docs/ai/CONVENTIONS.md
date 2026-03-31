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
- **Auto-deploy:** After every change, always run `./gradlew assembleDebug` followed by `adb devices`. If a device is present, immediately run `adb install -r app/build/outputs/apk/debug/app-debug.apk`. Do not wait for the user to ask.
- Install preserving data: `adb install -r app/build/outputs/apk/debug/app-debug.apk`
- Never use `adb uninstall` + `adb install` (loses API keys and preferences)
- Device package name for `run-as`: `com.georgernstgraf.aitranscribe.debug` (debug build has `.debug` suffix)
## Database Inspection
- If debugging is required on a physical device, a statically linked `sqlite3` binary can be pushed to `/data/local/tmp/`.
- Accessing the app's internal database requires root access or specific `run-as` permissions.
- Direct file access via `adb` is usually restricted. Use log-based debugging (`adb logcat`) for routine inspections.
- If direct inspection is necessary, copy the database to a readable directory (e.g., `/data/local/tmp/`) using `run-as` if possible, then query it. Ensure cleanup of sensitive data afterwards.
- Canonical schema is `prisma/desired/schema.prisma`; observed runtime schema is `prisma/device/schema.prisma`.
- Refresh runtime schema with `cd prisma && make`.
- Check schema drift with `cd prisma && make check-schema`.

## Companion Project
- `../aitranscribe` (Python/TUI) is the **lead and authoritative project** for pipeline logic, prompts, and feature design
- This Android app is the companion/follower — prompts, modes, and workflow must mirror the Python project
- Key file in companion: `core.py` (LLM call function), `main.py` (prompts, modes, pipeline), `tui.py` (UI behavior)
- When adding or changing post-processing behavior, always check `../aitranscribe/main.py` first

## Knowledge Persistence
- Use the persistent workflow in `docs/ai/` — this is the canonical knowledge location
- Use the `knowledge-persistence` skill when available

### Persistence Triggers
1. **End of productive session** — always update STATE.md and HANDOFF.md
2. **After an architectural or technical decision** — add to DECISIONS.md immediately
3. **After discovering a bug, constraint, or non-obvious behavior** — add to PITFALLS.md
4. **After establishing a coding pattern or naming rule** — add to CONVENTIONS.md
5. **When the user asks to "save context" or "persist knowledge"** — full persistence run

### Knowledge File Content Guide

| File | Contains | Disambiguation Test |
|------|----------|---------------------|
| DECISIONS.md | One-time choices with rationale | "Is this a past choice I made?" |
| CONVENTIONS.md | Ongoing rules to follow every time | "Must I follow this on every change?" |
| PITFALLS.md | Things that don't work, subtle bugs | "Would a new agent repeat this mistake?" |
| STATE.md | Current project status (overwritten entirely) | "What's happening right now?" |
| HANDOFF.md | Pending tasks for next agent | "What's unfinished?" |
| DOMAIN.md | Business rules not obvious from code | "Would a developer miss this from code alone?" |

### Fallback Protocol
If the `knowledge-persistence` skill is not available:
1. Read all existing `docs/ai/` files
2. Identify new facts, decisions, patterns from this session not yet recorded
3. Append to the correct file using the content guide above (do not duplicate)
4. Overwrite STATE.md entirely with current status
5. Update HANDOFF.md: clear if done, or list pending tasks with context
6. Report which files were changed and how many entries were added

Keep each knowledge file under 200 lines. Split by topic if needed.

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
