# Architectural Decisions

## 2026-03-29: Detail screen swipe uses flatMapLatest (Issue #28)
- **Reason:** `loadTranscription()` launched a new Flow collector on every swipe without cancelling the previous one. Multiple collectors raced to update `_uiState`, causing old data to overwrite new data.
- **Fix:** Replaced with `_activeTranscriptionId.flatMapLatest { id -> repository.getByIdFlow(id) }` — previous Flow auto-cancelled when ID changes
- **Changed:** `TranscriptionDetailViewModel` no longer has `loadTranscription()`; `onPageChanged()` just updates `_activeTranscriptionId.value`

## 2026-03-29: Filter pills integrated into BottomControlPanel
- **Reason:** Issue #25 — unified bottom panel instead of separate QuickFilters row above the panel
- **Layout:** Column: filter pill row (SpaceBetween) → Row: radio buttons (weight 1f) + record button
- **Changed:** `QuickFilters` composable no longer called from `MainScreen`; pills inlined into `BottomControlPanel` with `currentFilter`/`onFilterChanged` params

## 2026-03-29: Audio files deleted immediately after successful transcription
- **Reason:** No audio files shall be stored on the device per user requirement
- **Flow:** TranscriptionWorker saves entity with `audioFilePath = null`, deletes audio, sweeps orphans
- **Safety:** `cleanupOrphanedAudioFiles()` excludes files still referenced by queued (pending retry) items

## 2026-03-29: Post-processing failures are non-fatal (no retry)
- **Reason:** PostProcessingException was caught by same try-catch as transcription, causing `Result.retry()` which re-ran the entire worker, creating infinite duplicate transcriptions in DB
- **Fix:** Transcription and post-processing have separate error handling. Only transcription failure triggers retry. Post-processing failure is logged but worker returns `Result.success()`

## 2026-03-29: Companion project `../aitranscribe` is the authoritative source
- **Reason:** Python project is the lead; Android follows its prompts, modes, and pipeline
- **Implication:** Always check `../aitranscribe/main.py` and `core.py` when changing post-processing behavior

## 2026-03-29: PostProcessingType changed from GRAMMAR/ENGLISH to RAW/CLEANUP/ENGLISH
- **Reason:** Match original aitranscribe Python project's modes (`raw`, `cleanup`, `english`)
- **Changed:** `GRAMMAR` renamed to `CLEANUP`, `RAW` added as default (no LLM post-processing)

## 2026-03-29: Summary field added to TranscriptionEntity
- **Reason:** Original aitranscribe uses LLM to generate 70-80 char summaries as display titles
- **Changed:** Added `summary TEXT DEFAULT NULL` column, DB version bumped to 2 with migration
- **Generation:** After transcription, `PostProcessTextUseCase.generateSummary()` calls LLM via OpenRouter

## 2026-03-29: ProcessingMode persisted in SecurePreferences
- **Reason:** User selects RAW/CLEANUP/ENGLISH on main screen; must persist across restarts
- **Flow:** MainViewModel → QueuedTranscriptionEntity.postProcessingType → TranscriptionWorker → PostProcessTextUseCase

## 2026-03-29: Bottom control panel layout (radio buttons + mic button)
- **Reason:** Issue #24 SVG mockup shows processing mode radio buttons and record button in one bottom panel
- **Layout:** TopAppBar → transcription list → filter pills → bottom control panel

## 2026-03-29: OpenRouterApiService via separate Retrofit instance
- **Reason:** GROQ and OpenRouter have different base URLs
- **Changed:** NetworkModule provides both `GroqApiService` and `OpenRouterApiService`

## 2026-03-29: ViewModel test pattern: runBlocking + runCurrent + viewModelScope.cancel
- **Reason:** `runTest` calls `advanceUntilIdle()` at teardown which hangs with infinite collectors. `advanceUntilIdle()` burns CPU polling pending work that never ends.
- **Fix:** Use `runBlocking` + `StandardTestDispatcher` + `testDispatcher.scheduler.runCurrent()` to flush work once. Cancel `viewModel.viewModelScope` in `@After` tearDown.

## 2026-03-29: org.json:json added as test dependency
- **Reason:** Android's stub `org.json` returns null for all methods in unit tests (with `isReturnDefaultValues = true`). Integration tests that parse JSON API responses need a real implementation.
- **Changed:** `testImplementation("org.json:json:20231013")`

## 2026-03-29: Guard markAsViewed with playedCount == 0 check
- **Reason:** Without the guard, `observeActiveTranscription` called `markAsViewed` on every flow emission, creating an infinite loop that incremented `playedCount` forever.
- **Changed:** `if (!suppressAutoMark)` → `if (!suppressAutoMark && entity.playedCount == 0)` in TranscriptionDetailViewModel

## 2026-03-04: hiltViewModel() over viewModel() for all Compose screens
- **Reason:** Plain `viewModel()` bypasses Hilt DI, causing runtime crashes
- **Rule:** Every `@HiltViewModel` must use `hiltViewModel()` in Compose

## 2026-03-04: TranscriptionWorker fetches queued item by ID, not "next in queue"
- **Reason:** Prevents race conditions when multiple recordings queued concurrently

## 2026-03-04: Navigation route parameter "transcription_id" (not "id")
- **Reason:** Must match SavedStateHandle key used by TranscriptionDetailViewModel

## 2026-03-04: RECEIVER_NOT_EXPORTED for internal BroadcastReceivers
- **Reason:** Android 13+ (API 33) requires explicit export flag

## 2026-03-04: audio/mp4 MIME type for .m4a uploads to GROQ
- **Reason:** RecordingService outputs .m4a (AAC in MP4 container), not audio/mpeg
