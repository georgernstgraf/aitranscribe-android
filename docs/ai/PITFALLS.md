# Pitfalls and Gotchas

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
