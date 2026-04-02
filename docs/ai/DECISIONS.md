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

## 2026-03-31: Dynamic Provider LLM Routing
- **Choice**: Configured `GroqApiService` to handle `chat/completions` directly and updated `PostProcessTextUseCase` to route requests dynamically based on the selected provider.
- **Reason**: The previous implementation forced all LLM requests through `OpenRouterApiService` if they weren't explicitly ZAI, which caused HTTP 401 errors when attempting to use a Groq API key on an OpenRouter endpoint.
- **Tradeoff**: Required duplicating the OpenAI-compatible DTO structures across Retrofit API interfaces, but maintains clean separation of base URLs.

## 2026-03-31: Prisma desired schema is canonical for DB governance (#54)
- **Choice**: Treat `prisma/desired/schema.prisma` as the canonical DB contract and `prisma/device/schema.prisma` as observed runtime state.
- **Reason**: The team wants DB evolution to be desired-first and explicitly detect runtime drift.
- **Changed**: Added `prisma/GOVERNANCE.md` and Make targets `refresh-device`/`check-schema` to support the workflow.

## 2026-03-31: Desired provider-model-capability schema normalized (#52)
- **Choice**: Keep normalized provider-model-capability representation in `prisma/desired/schema.prisma` using explicit `model_capability` join mapping.
- **Reason**: Current runtime storage is denormalized (`models.capabilities` string), and desired-first governance requires target schema to be explicit before Kotlin migration work.
- **Changed**: `provider_model` now has surrogate `id` plus `@@unique([providerId, externalId])`; `capability` and `model_capability` represent capabilities relationally.

## 2026-03-31: Provider API token modeled as scalar field on provider (#52)
- **Choice**: Store provider token as nullable `provider.api_token` in desired schema, not in a separate token table.
- **Reason**: Simpler desired model and straightforward provider-centric reads.
- **Tradeoff**: Requires auth storage migration from `SecurePreferences` to Room-aligned schema in Kotlin implementation.

## 2026-03-31: Desired schema hardening pass for indexes and constraints (#51)
- **Choice**: Hardened `prisma/desired/schema.prisma` with targeted defaults, FK update cascades, and query-oriented indexes without changing domain structure.
- **Reason**: Improve data integrity and lookup performance while keeping schema redesign work isolated to #52/#53.
- **Changed**: Added defaults/indexes on `provider` and `transcriptions`, added `onUpdate: Cascade` on provider/capability relations, and removed redundant `model_capability.modelId` index covered by PK prefix.

## 2026-03-31: Runtime provider-model-capability storage normalized in Room (#52)
- **Choice**: Migrated Room schema from denormalized `models.capabilities` JSON text to normalized `capabilities` + `model_capabilities` tables, with `models.id` as surrogate PK and `external_id` retained for provider-facing model IDs.
- **Reason**: Align runtime DB with desired relational model and enable durable many-to-many capability mapping.
- **Changed**: Added DB `version = 6` migration (`MIGRATION_5_6`), updated DAO replacement flow, updated sync pipeline to write capability rows and joins, and switched UI model selection to use `externalId`.

## 2026-03-31: Provider auth moved to DB-first storage with compatibility fallback (#55)
- **Choice**: Store provider auth token in `providers.api_token` (Room) as canonical runtime location, while retaining SecurePreferences fallback/backfill for compatibility.
- **Reason**: Align runtime implementation with desired schema (`provider.api_token`) without breaking existing installs that only have preference-based tokens.
- **Changed**: Added DB `version = 7` migration (`MIGRATION_6_7`) converting `display_name` to `name` and adding `api_token`; updated worker/settings auth reads to DB-first and writes to sync DB + SecurePreferences.

## 2026-03-31: ZAI coding endpoint fallback for post-processing (#57)
- **Choice**: For ZAI LLM post-processing, retry via coding endpoint (`/api/coding/paas/v4/chat/completions`) when general endpoint returns package/balance-style HTTP 429.
- **Reason**: Coding-subscription keys can fail on the general endpoint with `insufficient balance/no resource package` despite being valid for coding endpoint usage.
- **Changed**: Added `ZaiCodingApiService` and fallback logic in `PostProcessTextUseCase.callLlmApi`.

## 2026-03-31: Auth-token storage no longer uses SecurePreferences keys (#57)
- **Choice**: Treat `providers.api_token` as the only runtime auth-token source; keep `SecurePreferences` for non-auth settings only.
- **Reason**: Reduce token-source drift and align provider auth behavior with DB governance.
- **Changed**: Removed legacy auth-key helper usage paths and switched app auth lookups to DB-backed methods.

## 2026-03-31: Device introspection is the source of current DB truth during migration work
- **Choice**: Use freshly generated `prisma/device/schema.prisma` from `cd prisma && make` as the current implementation truth reference.
- **Reason**: Runtime schema must be validated against what actually exists on device/emulator, not assumed from planned schema changes.
- **Changed**: Re-ran `cd prisma && make` and persisted the convention to always refresh device truth before drift evaluation.

## 2026-03-31: Removed SecurePreferences from production and moved settings to Room-backed store (#56)
- **Choice**: Replaced `SecurePreferences` with `AppSettingsStore` backed by `app_preferences` + `providers.api_token`.
- **Reason**: Single DB-backed persistence path reduces settings/token drift and aligns runtime behavior with device-introspected schema truth.
- **Changed**: Added `MIGRATION_7_8` for `app_preferences`, migrated production callers (viewmodels/workers/setup), and deleted `SecurePreferences` from production code.

## 2026-03-31: Transcription view state migrated from `played_count` to `seen` (#53)
- **Choice**: Use `seen` as the canonical read/unread state, with `playedCount` derived only in domain mapping for compatibility.
- **Reason**: Desired model uses `seen`; `played_count` caused coupling and extra query complexity.
- **Changed**: Added `MIGRATION_8_9` to map `seen = played_count > 0`; updated DAO filters and view-state toggles to `seen` semantics.

## 2026-03-31: Removed transcription execution-context columns from runtime table (#53)
- **Choice**: Removed `stt_model`, `llm_model`, `post_processing_type`, and `retry_count` from `transcriptions` runtime schema.
- **Reason**: Desired schema excludes these fields; they represent operational context better sourced from current settings and worker flow.
- **Changed**: Added `MIGRATION_9_10` to rebuild `transcriptions` without those columns; updated repositories/use-cases/workers/tests accordingly.

## 2026-03-31: Transcription text unified to a single nullable `text` field (#44)
- **Choice**: Replaced dual text columns (`original_text`, `processed_text`) with one nullable runtime field (`text`) and updated app models to `text`.
- **Reason**: STT and cleanup pipeline only need one current text value; pending STT is represented by `text = NULL` with retained `audio_file_path`.
- **Changed**: Added `MIGRATION_11_12` with `text = COALESCE(processed_text, original_text)` backfill; removed `processedText` from Kotlin models and query paths.

## 2026-03-31: Retry queue inferred from data invariants, not status enum (#44)
- **Choice**: Unfinished STT items are selected by `text IS NULL AND audio_file_path IS NOT NULL`.
- **Reason**: This invariant is simpler and aligns with offline/airplane-mode queue semantics.
- **Changed**: Added DAO/repository `getUnfinishedSttTranscriptions()` and switched main/settings retry triggers to it.

## 2026-03-31: STT success writes text and clears audio path atomically (#44)
- **Choice**: On STT success, perform a single SQL update setting `text`, clearing `audio_file_path`, and resetting error state.
- **Reason**: Prevent intermediate inconsistent states and duplicate retry behavior.
- **Changed**: Added DAO/repository `markSttSuccess(...)` and used it in `TranscriptionWorker` before post-processing.

## 2026-03-31: Recording files moved from cache to persistent app files dir (#44)
- **Choice**: Store recordings in `filesDir/recordings` using `createNewFile()` uniqueness checks.
- **Reason**: `cacheDir` is not reliable for queued audio across app restarts and system cache cleanup.
- **Changed**: `RecordingService.createTempAudioFile()` now writes to `filesDir/recordings`; worker orphan cleanup scans same directory with a grace period.

## 2026-03-31: Missing-audio rows become terminal warning, not retry loops (#44)
- **Choice**: If worker cannot find referenced audio file, mark row warning and clear `audio_file_path` instead of failing/retrying forever.
- **Reason**: Prevent repeated startup failure toasts and stuck work for irrecoverable missing files.
- **Changed**: Added `markAudioMissing(...)` DAO/repository path; worker handles `AudioFileMissingException` with success + warning status.

## 2026-03-31: Post-STT summary generation is always requested when LLM is configured (#44)
- **Choice**: After STT success, summary generation runs regardless of cleanup mode; cleanup itself runs only in CLEANUP mode.
- **Reason**: Matches product rule: cleanup is optional, summary is requested when text exists.
- **Changed**: `TranscriptionWorker.performPostProcessing()` now gates cleanup by mode and calls `generateSummary(...)` in all modes.

## 2026-04-01: Cleanup/translation prompt routing normalized to explicit prompt keys (#46)
- **Choice**: Prompt composition now uses explicit key families: `prompt.translate.*` for detail actions with cleanup off, and `prompt.cleanup` + `prompt.cleanup.<lang|null>` for cleanup-enabled actions.
- **Reason**: German cleanup flow produced unexpected English output and prompt behavior was hard to reason about.
- **Changed**: `PostProcessTextUseCase` now resolves prompts by flow/context and language state, with deterministic cleanup-first ordering.

## 2026-04-01: Summary prompt is generated once per action path (#46)
- **Choice**: Removed duplicate summary trigger in worker cleanup path.
- **Reason**: Recording flow produced duplicate summary requests and unstable outcomes.
- **Changed**: Worker no longer calls a second summary after cleanup/detail use-case path that already generates summary.

## 2026-04-01: Prompt preview logging is sanitized and summary-focused in UI (#46)
- **Choice**: Persist/show only summary prompt preview in detail screen while logging all prompt previews with `{{TEXT}}` placeholder.
- **Reason**: Needed inspectable prompt debugging without exposing full transcription text in debug preview state.
- **Changed**: Added summary-preview preference key and UI card switched from generic prompt preview to summary prompt preview.

## 2026-04-01: Removed in-app prompt preview cards; keep logcat-only prompt previews (#46)
- **Choice**: Prompt previews are now logcat-only (`PromptDebug`) and no longer rendered in detail screen UI.
- **Reason**: Prompt preview popups/cards were intrusive during normal use.
- **Changed**: Removed summary preview state from `TranscriptionDetailViewModel` and preview card from `TranscriptionDetailScreen`; removed preview persistence keys from `AppSettingsStore`.

## 2026-04-01: Non-fatal post-processing failures now surface toast feedback (#46)
- **Choice**: Show warning toast when work succeeds but transcription status is `COMPLETED_WITH_WARNING`.
- **Reason**: Timeout/provider post-processing failures were too silent in main flow.
- **Changed**: Added `WorkInfo.State.SUCCEEDED` warning branch in `MainViewModel.observeWorkResult()`.

## 2026-04-01: Issue #48 closed — Android infrastructure already implemented (#48)
- **Choice**: Closed Issue #48 as completed after assessment revealed 5/6 sub-items were already done.
- **Reason**: NetworkCallback (API 26+), Kotlin Flow (`callbackFlow`), `ACCESS_NETWORK_STATE` permission, minSdk 26, and Java 17 were all already in place.
- **Changed**: Opened Issue #58 for the one genuinely open item: JUnit 4 → JUnit 5 migration.

## 2026-04-01: JUnit 4 → JUnit 5 migration completed (#58)
- **Choice**: Migrated entire test suite from `junit:junit:4.13.2` to JUnit 5 (`org.junit.jupiter:junit-jupiter:5.10.1`).
- **Reason**: Modern testing framework with `@BeforeEach`/`@AfterEach`, `assertThrows`, better extension model.
- **Changed**: 
  - `build.gradle.kts`: added `junit-jupiter:5.10.1`, `junit-platform-launcher`, removed `junit:junit:4.13.2`, added `useJUnitPlatform()`.
  - `MainDispatcherRule`: migrated from `org.junit.rules.TestWatcher` to `BeforeEachCallback`/`AfterEachCallback`.
  - All 27 test files migrated (`org.junit.Assert.*` → `org.junit.jupiter.api.Assertions`, `@Before` → `@BeforeEach`, `@After` → `@AfterEach`).
  - `@Test(expected=...)` replaced with `assertThrows { }` in JUnit 5.
  - Android instrumentation tests retain `@RunWith(AndroidJUnit4::class)` for Android component support.
- **Test result**: 111/112 tests pass; `ValidateApiKeysIntegrationTest` fails without live API keys (expected).

## 2026-04-01: Minimum SDK raised to 30 (#59)
- **Choice**: Set `minSdk = 30` (Android 11) and removed all API < 30 compatibility code.
- **Reason**: No requirement to support older devices; `MediaRecorder(Context)` at API 31 was already the practical floor since the app's core recording feature uses it.
- **Changed**: Removed `WRITE_EXTERNAL_STORAGE` permission (dead at minSdk 30), removed `if (Build.VERSION.SDK_INT >= O)` guards around NotificationChannel creation in `AITranscribeApp` and `RecordingService`, added API 31 guard for `MediaRecorder(Context)` (uses no-arg constructor on API 30), replaced `ContextCompat.checkSelfPermission` with direct `checkSelfPermission`, removed 3 dead dependencies (`threetenbp`, `datastore-preferences`, `security-crypto`).
- **Kept**: `registerReceiver` Tiramisu branch (API 33), dynamic color check (API 31), `READ_EXTERNAL_STORAGE` with `maxSdkVersion=32`, `WindowCompat.getInsetsController` (platform `WindowInsetsController` lacks `isAppearanceLightStatusBars` property).

## 2026-04-01: Settings screen save button moved to TopAppBar with online validation (#60)
- **Choice**: Replaced "Save Settings" button with floppy disk icon in TopAppBar, added online API key validation.
- **Reason**: Better UX - settings save is a primary action that belongs in the navigation bar; validation ensures keys work before saving.
- **Changed**: Added `Icons.Default.Save` to TopAppBar actions with loading state, `saveSettings()` now validates keys online via `ValidateApiKeysUseCase.validateProviderKey()` before saving.
- **Centralized validation**: Created provider-agnostic validation methods (`isValidKeyFormat`, `validateProviderKey`) supporting Groq, OpenRouter, and ZAI.

## 2026-04-01: Provider management UI patterns (#60)
- **Choice**: Active Providers section moved to bottom above Delete button; hide "Connect Provider" when all connected; trash icon for disconnect; "Authenticate" label for auth action.
- **Reason**: Clearer visual hierarchy - active providers are status display, not primary action; disconnect needs confirmation affordance.
- **Changed**: `ProviderStatusItem` shows trash icon for connected providers, "Authenticate" button for disconnected; conditional rendering of "Connect Provider" button.

## 2026-04-01: Provider authentication with format + online validation (#60)
- **Choice**: ProviderAuthScreen validates API keys before saving (format check + online verification).
- **Reason**: Prevents saving invalid keys that would fail later; gives immediate feedback with provider-specific hints.
- **Changed**: `validateAndSaveProviderAuth()` returns `ProviderAuthResult` sealed class; format validation shows hints ("Groq keys start with 'gsk_'", etc.); online verification calls provider API before saving.

## 2026-04-01: Settings screen refreshes provider list on navigation return (#60)
- **Choice**: Use `currentBackStackEntryAsState()` + `LaunchedEffect` to trigger `loadSettings()` when returning to SettingsScreen.
- **Reason**: Provider list was stale after authenticating a new provider in ProviderAuthScreen.
- **Changed**: Made `loadSettings()` public in SettingsViewModel; added navigation-aware refresh trigger in SettingsScreen.

## 2026-04-01: Main screen Raw/Cleanup toggle removed, always RAW mode (#63)
- **Choice**: Removed Raw/Cleanup toggle from BottomControlPanel; recording always produces RAW output.
- **Reason**: Simpler UX; cleanup available via detail screen when needed; matches product workflow.
- **Changed**: Removed `processingMode` from MainUiState and MainViewModel; removed `setProcessingMode()` and `loadProcessingMode()`; changed TranscriptionWorker to always use `PostProcessingType.RAW`; removed processing mode storage from AppSettingsStore.

## 2026-04-01: BottomControlPanel single row layout (#63)
- **Choice**: Filter pills (Unread/All/Read) and record button on same row, removed processing mode switch.
- **Reason**: Cleaner layout with fewer UI elements; record button is primary action.
- **Changed**: Reorganized composable to single Row with filter pills taking weight space and record button fixed size.

## 2026-04-01: Whisper API language detection captured for summary generation (#61)
- **Choice**: Capture `language` field from Whisper API response, store in database, include in summary prompt.
- **Reason**: Fixes intermittent wrong-language summaries when LLM guessed language from text content.
- **Changed**: Added `language` field to `GroqTranscriptionResponse`; `transcribeAudio()` returns `TranscriptionResult(text, language)`; `markSttSuccess()` stores language; `generateSummary()` builds language-aware prompt; added `getLanguageDisplayName()` for 30+ language codes.
- **Prompt update**: Summary prompt now includes "The summary MUST be written in {{language}}. Do not translate or change the language."

## 2026-04-01: OpenRouter STT support removed (#62)
- **Choice**: Removed OpenRouter from STT providers; GROQ and ZAI remain as working STT options.
- **Reason**: OpenRouter has no dedicated STT endpoint; would require complex base64 chat API integration with higher costs and lower accuracy.
- **Changed**: Removed OpenRouter from `sttProviders` in ProviderConfig; removed `transcribeAudio()` from OpenRouterApiService; removed OpenRouter case from TranscriptionWorker; updated tests; tagged issue #62 with "not now" label.
- **OpenRouter remains**: As LLM provider for post-processing (works correctly via chat completions).

## 2026-04-01: Instrumentation tests use JUnit 4, unit tests use JUnit 5
- **Choice**: Keep instrumentation tests (`src/androidTest`) on JUnit 4 while unit tests (`src/test`) use JUnit 5.
- **Reason**: Android instrumentation testing has limited JUnit 5 support; Espresso and Compose testing work best with JUnit 4. The android-junit5 plugin exists but has compatibility issues with API 36+.
- **Changed**: Converted 6 instrumentation test files from JUnit 5 to JUnit 4 imports; added `packagingOptions` excludes for META-INF/LICENSE.md files; created API 35 emulator for testing.
- **Results**: All 31 instrumentation tests pass on API 35 emulator; 115 unit tests pass with JUnit 5.

## 2026-04-01: API 35 emulator for instrumentation testing
- **Choice**: Use API 35 (Android 15) emulator instead of API 36 for instrumentation tests.
- **Reason**: API 36 has Espresso/Compose compatibility issues (`InputManager.getInstance` method not found); API 35 provides stable testing environment.
- **Changed**: Installed API 35 system image; created `Medium_Phone_API_35` AVD; verified all 31 instrumentation tests pass.
- **Note**: Production app targets API 36 for compilation, but tests run on API 35.
