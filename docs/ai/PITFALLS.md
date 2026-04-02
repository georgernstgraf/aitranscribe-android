# Pitfalls and Gotchas

## Creating new UiState instance resets all fields to defaults
When updating ViewModel state, using `_uiState.update { SettingsUiState(...) }` creates a completely new instance, resetting all fields not explicitly provided to their default values. This caused SettingsScreen to show "0 active / 0 total" languages because `allLanguages` was reset to emptyList(). **Fix:** Use `_uiState.update { it.copy(...) }` to preserve existing values.

## Room entity must declare foreign keys to match database schema
If migration creates a table with `FOREIGN KEY...` but the Entity class lacks `@Entity(foreignKeys = [...])`, Room throws `IllegalStateException: Migration didn't properly handle...` at runtime. The schema validation is strict. **Fix:** Add matching `ForeignKey` annotation AND `Index` on the column.

## Detail screen: never launch multiple Flow collectors for swipe paging
Launching `repository.getByIdFlow(id).collect {}` in a new coroutine on every swipe without cancelling the previous one causes collectors to race, overwriting `_uiState` with stale data. Use `flatMapLatest` on a StateFlow of the active ID instead.

## Hilt stale incremental build causes ClassNotFoundException
After changing Hilt dependency graph (e.g., adding `PostProcessTextUseCase` to `TranscriptionWorker`, adding `OpenRouterApiService` DI), incremental builds can produce APKs missing `AITranscribeApp_GeneratedInjector`. **Fix:** Always run `./gradlew clean assembleDebug` before deploying to device.

## Hilt + Compose: viewModel() vs hiltViewModel()
Using `viewModel()` instead of `hiltViewModel()` silently bypasses Hilt injection. Every `@HiltViewModel` must use `hiltViewModel()`.

## Android 13+ BroadcastReceiver requires export flag
Registering a BroadcastReceiver without `Context.RECEIVER_NOT_EXPORTED` throws SecurityException on API 33+.

## Navigation parameter name must match SavedStateHandle key
Route parameter names and SavedStateHandle keys must be identical (e.g., `"transcription_id"`).

## TranscriptionWorker: Don't use "get next queued" pattern
Always pass and use the specific transcription ID to avoid race conditions.

## GROQ API returns " ." for empty/silent audio
When audio contains no speech, GROQ Whisper returns `" ."` (not an error).

## .m4a files need audio/mp4 MIME type, not audio/mpeg
RecordingService outputs .m4a (AAC in MP4 container). Correct MIME type is `audio/mp4`.

## adb install -r to preserve app data
Always use `adb install -r` (replace). `adb uninstall` + `adb install` wipes API keys and preferences.

## Room DB migration with fallbackToDestructiveMigration
When `exportSchema = false`, `fallbackToDestructiveMigration()` will wipe all data on schema mismatch. Existing transcriptions will be lost after a migration. Use `adb install -r` to preserve data when possible.

## TranscriptionWorker: single try-catch causes infinite duplicate transcriptions
If post-processing throws inside the same try-catch as transcription, `Result.retry()` re-runs the whole worker — re-transcribing and inserting duplicate DB entries in a loop. **Fix:** Separate error handling so only transcription failure triggers retry.

## Room column names use Kotlin field name by default (camelCase)
Without explicit `@ColumnInfo(name = "...")`, Room uses the Kotlin property name as the SQL column name (e.g., `audioFilePath`, not `audio_file_path`). Writing SQL queries with snake_case will fail at KSP time.

## Device package name has .debug suffix
The debug build's package name is `com.georgernstgraf.aitranscribe.debug` (not `com.georgernstgraf.aitranscribe`). Use this for `run-as`, `adb shell pm`, etc.

## sqlite3 on device: run-as cannot execute /data/local/tmp binary directly
`run-as com.georgernstgraf.aitranscribe.debug /data/local/tmp/sqlite3 ...` fails with `Permission denied`. Copy the binary into the app sandbox first, then execute it there, e.g. `run-as <pkg> sh -c "cp /data/local/tmp/sqlite3 files/sqlite3 && chmod 700 files/sqlite3 && ./files/sqlite3 databases/aitranscribe.db ..."`.

## Post-processing failures are surfaced via COMPLETED_WITH_WARNING (IMPLEMENTED)
When LLM post-processing fails (wrong model, insufficient credits, network error), the transcription is saved with `COMPLETED_WITH_WARNING` status. The detail screen shows an amber warning banner with the error message. The raw transcription text is still displayed. Implemented in #32.

## Setup screen flashes on every app start
`startDestination = "setup"` always renders the setup screen first. `loadExistingKeys()` then validates and triggers navigation to main. This causes a visible flash. Should check keys before rendering navigation, or use a splash/loading state.

## ViewModel tests with infinite Flow collectors hang forever
ViewModels that call `flow.collect {}` in `init` create infinite coroutines. Using `advanceUntilIdle()` or `UnconfinedTestDispatcher` with `setMain` causes tests to hang. **Fix:** Use `StandardTestDispatcher` with `setMain`, avoid `advanceUntilIdle()`, use `runCurrent()` instead, read `.value` directly on StateFlow.

## runTest hangs with infinite viewModelScope coroutines
`runTest` calls `advanceUntilIdle()` internally at test end. If `viewModelScope` has an infinite `collect`, this never completes. Use `runBlocking` instead for ViewModels with infinite collectors.

## org.json Android stubs return null in unit tests
With `isReturnDefaultValues = true` in build.gradle.kts, `JSONObject.getJSONArray()` returns `null` instead of parsing JSON. **Fix:** Add `testImplementation("org.json:json:20231013")` for a real implementation in tests.

## Intent extras return null in JVM unit tests
`Intent.getStringExtra()` and `Intent.getParcelableExtra()` return null in JVM unit tests because Android framework classes are stubbed. Test share/compose logic on plain data classes or helper functions instead of asserting on Intent contents. For code that constructs Intents, trust the framework and test the logic separately.

## ZAI ASR does not accept .m4a files
ZAI's `glm-asr-2512` rejects `.m4a` with error code 1214 (`format not supported`). Only `.mp3` and `.wav` are listed as supported. GROQ accepts `.m4a` with `audio/mp4` MIME. Do not add ZAI as an STT provider without client-side audio conversion.

## ZAI has two base URLs — use api.z.ai for international
`open.bigmodel.cn/api/` is the Chinese endpoint. `api.z.ai/api/` is the international one. Both accept the same key format and return identical responses. Use `api.z.ai` as the base URL in `ZaiApiService`.

## ZAI API key format: hex string + dot + base64
ZAI keys look like `a116a2e0344312a8aa5f33bfbee3c9f7.V5OqBIoBBqm9yWj0`. Validate: `length >= 20 && contains(".")`. No prefix like GROQ's `gsk_` or OpenRouter's `sk-or-`.

## ZAI response includes reasoning_content field
ZAI's `glm-4.7-flash` returns `reasoning_content` in the message object alongside `content`. This is a thinking/reasoning field. The DTO (`OpenRouterMessage`) ignores it via Gson — no `@SerializedName` match. Don't add it unless needed.

## Material3 ExposedDropdownMenuBox uses Modifier.menuAnchor() without args
In the M3 version used by this project, `Modifier.menuAnchor()` takes no arguments (no `MenuAnchorPoint`). Passing `MenuAnchorPoint.PrimaryNotEditable` causes compilation error.

## return keyword not allowed inside withContext lambda
Using `return` inside `withContext(Dispatchers.IO) { ... }` causes "return is not allowed here" compilation error. Use bare expressions or `return@withContext` instead.

## Cross-test hangs from uncleared viewModelScope
ViewModel tests must cancel `viewModel.viewModelScope` in `@After` tearDown. Otherwise infinite collectors from one test class leak into the next, causing hangs.

## Infinite markAsViewed loop in TranscriptionDetailViewModel (FIXED)
`observeActiveTranscription` called `markAsViewed` on every flow emission without checking `playedCount`. This caused a cycle: markAsViewed → DB update → flow re-emit → markAsViewed again, incrementing playedCount forever. **Fix:** Guard with `entity.playedCount == 0`.

## Overscroll auto-navigation breaks detail screen
Using `NestedScrollConnection.onPostScroll` to auto-navigate prev/next on overscroll fires on initial load because `scrollState.value == 0` is immediately true. Use HorizontalPager with explicit prev/next buttons instead.

## Bundling sqlite3 binary in APK for debugging
Bundling `sqlite3` in `jniLibs` to inspect app databases on non-rooted devices is not viable. Android's security model (No-Exec partition) prevents executing binaries from the application's private data sandbox. **Fix:** Use the Android Studio Database Inspector for database inspection, or logcat for routine debugging.

## `prisma` schema check is strict text diff
`cd prisma && make check-schema` uses `diff -u` between `desired/schema.prisma` and `device/schema.prisma`. Any textual difference fails the check, including formatting/order differences. Keep both files intentionally aligned when asserting zero drift.

## `gh issue comment --body` with backticks can execute shell substitutions
Passing markdown with backticks via inline `--body "..."` in shell can trigger command substitution and mangle the comment. Use `gh issue comment --body-file - <<'EOF' ... EOF` for safe multiline comments.

## Updating `prisma/desired/schema.prisma` does not change runtime DB
Desired schema edits alone do not affect Room entities, migrations, or `prisma/device/schema.prisma`. Kotlin/Room implementation work is required before drift checks can pass.

## Room migration FK mismatch when referencing temporary table names
In migrations that rebuild tables (e.g. `models_new` -> `models`), creating dependent tables with foreign keys pointing to temporary names causes startup crash (`Migration didn't properly handle ...`). Create/link dependent tables after final rename, or seed into temporary non-FK table and copy into final FK table.

## Cleanup/post-processing errors are often provider-side, not app crashes
`TranscriptionWorker` logs like HTTP `429` (insufficient balance) or HTTP `400` unknown model during cleanup/LLM post-processing are handled as non-fatal warnings (`Worker result SUCCESS` with warning status path). Diagnose provider token/model/billing before suspecting crash bugs.

## ZAI coding-plan keys may fail on general endpoint
ZAI post-processing with coding-subscription keys can return HTTP `429` (`insufficient balance` / `no resource package`) on `https://api.z.ai/api/paas/v4/...` while succeeding on `https://api.z.ai/api/coding/paas/v4/...`. Keep provider pathing consistent with key/package type.

## Queued recordings stored in `cacheDir` can disappear between sessions
Android may evict app cache files, which causes queued STT rows to reference missing audio and repeatedly fail on startup. Store queued recordings in `filesDir/recordings` instead.

## Missing queued audio should be terminal, not retried forever
When `audio_file_path` points to a non-existent file, mark the row as warning and clear `audio_file_path`; returning worker failure causes repeated retries/toasts with no recovery path.

## Worker + detail summary triggers can silently double-request summaries
If `TranscriptionWorker` calls `generateSummary(...)` after a detail-style post-processing invoke that already generates summary, recording flow produces duplicate summary requests. Keep summary trigger in only one path per action.

## Prompt wrappers can be duplicated between builder and request assembly
Applying system wrapper text during prompt construction and again during message assembly duplicates instruction text and destabilizes output. Build raw request prompt first; apply `prompt.system.base`/`prompt.system.request` exactly once at send time.

## JUnit 5 assertNull(message, actual) is ambiguous in Kotlin coroutine lambdas
`assertNull("STT model error: ${result.sttModelError}", result.sttModelError)` can misinterpret the eager message string as the actual value inside Kotlin coroutine state machine bytecode. Use `assertEquals(null, result.sttModelError)` for unambiguous null assertions in coroutine test bodies.

## Gradle test results XML can show stale line numbers
After editing a test file, the compiled bytecode may cache old source-to-bytecode line mappings. Use `./gradlew --rerun-tasks` or `rm -rf app/build` to force clean recompilation when test line numbers in errors don't match source.

## MediaRecorder(Context) requires API 31, not 30
`MediaRecorder(Context)` constructor was added in API 31 (Android 12). The no-arg `MediaRecorder()` constructor exists since API 1 but is deprecated at API 31+. At minSdk 30, an API guard is required: use `MediaRecorder(context)` on API 31+, `MediaRecorder()` on API 30.

## window.insetsController does not expose isAppearanceLightStatusBars
The platform `WindowInsetsController` (API 30+) uses `setSystemBarsAppearance()` / `getSystemBarsAppearance()`, not a direct `isAppearanceLightStatusBars` property. That property exists only on `WindowInsetsControllerCompat` (via `WindowCompat.getInsetsController()`). Do not replace `WindowCompat` with `window.insetsController` for this use case.

## OpenRouter does not have a dedicated audio/transcriptions endpoint
OpenRouter has **no STT endpoint** like GROQ's `/audio/transcriptions`. The existing `OpenRouterApiService.transcribeAudio()` method was calling a non-existent endpoint. OpenRouter supports audio only through multimodal chat completions with base64-encoded audio, which requires a completely different integration approach.

## Whisper API language detection should be captured and stored
Don't leave `language` as NULL after STT transcription. The Whisper API returns a `language` field (e.g., "de", "en") that should be captured in `GroqTranscriptionResponse`, passed to `markSttSuccess()`, and stored in the database. This enables language-aware prompts for summary generation instead of letting the LLM guess.

## Summary prompts must explicitly specify output language
Without explicit language instruction (e.g., "Write summary in German"), LLMs will guess the language from text content. German text with English loan words often gets misclassified as English. Always include `{{language}}` placeholder in summary prompts and populate it from the stored Whisper language.

## ZAI key format: length >= 20 && contains(".")
ZAI API keys look like `a116a2e0344312a8aa5f33bfbee3c9f7.V5OqBIoBBqm9yWj0` (hex.base64 format). Validate with `key.length >= 20 && key.contains(".")`. Unlike GROQ (`gsk_`) or OpenRouter (`sk-or-`), ZAI keys have no prefix.

## Provider auth validation requires both format and online checks
Don't just check key format (regex/prefix). Always validate API keys with an actual API call to verify they work and have sufficient credits. Format validation catches typos; online validation catches expired/revoked keys.

## Settings screen must refresh on return from auth screen
After authenticating a provider in ProviderAuthScreen and navigating back, the SettingsScreen provider list will be stale. Use `currentBackStackEntryAsState()` + `LaunchedEffect` to detect navigation changes and trigger `loadSettings()` refresh.

## Prompt logging must use {{TEXT}} placeholder for privacy
When logging prompts for debugging, never log the full transcription text. Use a `{{TEXT}}` placeholder in the logged user prompt. The system prompt (instructions) can be logged fully, but user content should be redacted or truncated.

## MediaRecorder requires API version check at minSdk 30
`MediaRecorder(Context)` constructor was added in API 31. At minSdk 30, you must check `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S` and use `MediaRecorder(context)` on API 31+ or `MediaRecorder()` (no-arg, deprecated) on API 30.

## Android API 36+ has Espresso/Compose testing compatibility issues
Running instrumentation tests on API 36 (Android 16) causes `java.lang.NoSuchMethodException: android.hardware.input.InputManager.getInstance []` errors. This is a framework compatibility issue between Espresso and newer Android versions. **Solution**: Use API 35 emulator for instrumentation testing.

## Cannot call setContent twice in same Compose UI test
In Compose UI tests (`ComposeContentTestRule`), calling `setContent` more than once per test method throws `IllegalStateException: Cannot call setContent twice per test!`. Split into separate test methods or use state-based recomposition instead.

## ModalBottomSheet confirmValueChange={false} blocks showing the sheet
Setting `confirmValueChange = { false }` on `rememberModalBottomSheetState` blocks ALL sheet state transitions, including the initial `Hidden → Expanded`. The sheet never appears. To make a non-dismissible sheet, block only the hide transition: `confirmValueChange = { it != SheetValue.Hidden }`.

## AppSettingsStore.getActiveLanguages() returns empty when preference never set
`AppSettingsStore.getActiveLanguages()` reads from a `KEY_ACTIVE_LANGUAGES` preference that may never have been written. It returns an empty list even though the `languages` table has active rows. Use `languageRepository.getActiveLanguages()` (which queries `is_active = 1` directly) for reliable language loading.

## Instrumentation tests must use JUnit 4, not JUnit 5
Android instrumentation tests (`src/androidTest`) require JUnit 4 (`org.junit.Test`, `org.junit.Assert`). While JUnit 5 works for unit tests (`src/test`), the Android testing framework and Espresso have limited JUnit 5 support. Use JUnit 4 for all instrumentation tests to avoid compatibility issues.

## META-INF/LICENSE.md conflicts in test APK packaging
When both JUnit 4 and JUnit 5 dependencies are present, building the test APK may fail with "6 files found with path 'META-INF/LICENSE.md'". **Fix**: Add to `build.gradle.kts`:
```kotlin
packaging {
    resources {
        excludes += "/META-INF/LICENSE.md"
        excludes += "/META-INF/LICENSE-notice.md"
    }
}
```
