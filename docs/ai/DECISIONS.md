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

## 2026-03-29: Share text logic lives in Transcription domain model (#29)
- **Reason:** `MainViewModel.shareTranscription()` and `ShareTranscriptionUseCase` both construct share text. Putting `getShareText()` on the `Transcription` data class avoids duplication and is trivially testable without Android stubs (Intent extras return null in JVM unit tests).
- **Changed:** `Transcription.getShareText()` prepends `summary: ` when summary is non-blank; `MainViewModel` and `ShareTranscriptionUseCase` call it directly.

## 2026-03-29: ZAI added as LLM provider, not STT (#32)
- **Choice:** ZAI (ZhipuAI / api.z.ai) as LLM provider only; GROQ remains sole STT provider
- **Reason:** ZAI ASR rejects `.m4a` format (`format not supported`). Adding `.m4a`→`.mp3` conversion was deemed too complex for initial integration.
- **Tested:** `.mp3` accepted by ZAI ASR (billing error, not format), `.m4a` rejected. `glm-4.7-flash` chat completions confirmed working.
- **Changed:** `ZaiApiService` (reuses OpenRouter DTOs), `ProviderConfig` hardcoded registry, settings UI with provider+model dropdowns

## 2026-03-29: Post-processing failures surfaced via COMPLETED_WITH_WARNING (#32)
- **Choice:** New `TranscriptionStatus.COMPLETED_WITH_WARNING` status + amber warning banner in detail screen
- **Reason:** LLM failures (wrong model, insufficient credits) were silently swallowed. User saw raw transcription with no feedback that cleanup/translation failed.
- **Changed:** `TranscriptionWorker` sets `COMPLETED_WITH_WARNING` + error message on post-processing failure. `TranscriptionDetailScreen` shows amber card with error detail.

## 2026-03-29: Provider dropdown + model dropdown pattern in settings (#32)
- **Choice:** Two dropdowns per section — pick provider first, then filtered model list
- **Changed:** `ExposedDropdownMenuBox` for STT model (GROQ only), LLM provider (OpenRouter/ZAI), LLM model (filtered by provider). Free-text model fields replaced entirely.
- **Model lists:** OpenRouter 8 models (mercury, gemini-2.5-flash-lite, gemini-2.0-flash, claude-3-haiku, mistral-small-3.1-24b, gemma-3-12b, llama-3.3-70b, llama-4-scout). ZAI 6 models (glm-4.7-flash free, glm-4.5-flash free, glm-4-32b-0414-128k, glm-4.7-flashx, glm-4.5-air, glm-4.7).

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

## 2026-03-30: Enhance Sharing Intent with Standard Metadata
- **Choice**: Utilize `Intent.EXTRA_SUBJECT` for sharing title.
- **Reason**: Standardizes metadata delivery for compatible Android apps (e.g., Mail, Obsidian).
- **Considered**: No changes, or manual text prepending.
- **Tradeoff**: Inconsistent third-party support, but compliant with Android standards.
Refactored Transcription.getShareText() to remove summary concatenation, as summary is now handled via Intent.EXTRA_SUBJECT. Updated tests accordingly.
Replaced copy icon with share icon on TranscriptionDetailScreen. Added shareTranscription() to TranscriptionDetailViewModel.

## 2026-03-30: Provider-Centric Authentication System
- **Choice**: Migrated from flat `groqApiKey`/`openRouterApiKey` structure to a dynamic `ProviderAuthToken` mapping in `SecurePreferences`.
- **Reason**: To support an arbitrary number of future providers gracefully without cluttering the Settings UI with empty text fields.
- **Tradeoff**: Required a mapping migration in SecurePreferences, temporarily maintaining backward compatibility for legacy flat keys.

## 2026-03-30: Database-Driven Dynamic Model Selection
- **Choice**: Storing AI models (`ModelEntity`) and providers (`ProviderEntity`) in a local Room database, populated via API sync.
- **Reason**: Hardcoding model lists is unsustainable. APIs rapidly deprecate old models and release new ones. Database storage enables an "Active Search" UI and scales effortlessly.
- **Tradeoff**: Introduces a new background worker (`ModelSyncWorker`) running on app startup.

## 2026-03-30: Robust Offline Audio Queueing
- **Choice**: `TranscriptionWorker` retains the recorded `.m4a` file in the device cache (by not nulling `audioFilePath`) until *both* STT and LLM post-processing steps succeed.
- **Reason**: Enables true "offline queueing" where users can record endless notes while offline or before entering API keys. The app will catch up on processing once configured.
- **Tradeoff**: Requires cautious cleanup sweeps to avoid filling the user's storage with orphaned audio files.
