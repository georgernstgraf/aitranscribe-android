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

## Post-processing failures must be surfaced to user
Wrong LLM model names, invalid OpenRouter keys, and network errors during post-processing fail silently. The transcription is saved but the user never knows cleanup/translation/summary failed. Must show feedback (e.g., status field, notification, or UI indicator).

## Setup screen flashes on every app start
`startDestination = "setup"` always renders the setup screen first. `loadExistingKeys()` then validates and triggers navigation to main. This causes a visible flash. Should check keys before rendering navigation, or use a splash/loading state.

## ViewModel tests with infinite Flow collectors hang forever
ViewModels that call `flow.collect {}` in `init` create infinite coroutines. Using `advanceUntilIdle()` or `UnconfinedTestDispatcher` with `setMain` causes tests to hang. **Fix:** Use `StandardTestDispatcher` with `setMain`, avoid `advanceUntilIdle()`, use `runCurrent()` instead, read `.value` directly on StateFlow.

## runTest hangs with infinite viewModelScope coroutines
`runTest` calls `advanceUntilIdle()` internally at test end. If `viewModelScope` has an infinite `collect`, this never completes. Use `runBlocking` instead for ViewModels with infinite collectors.

## org.json Android stubs return null in unit tests
With `isReturnDefaultValues = true` in build.gradle.kts, `JSONObject.getJSONArray()` returns `null` instead of parsing JSON. **Fix:** Add `testImplementation("org.json:json:20231013")` for a real implementation in tests.

## Cross-test hangs from uncleared viewModelScope
ViewModel tests must cancel `viewModel.viewModelScope` in `@After` tearDown. Otherwise infinite collectors from one test class leak into the next, causing hangs.

## Infinite markAsViewed loop in TranscriptionDetailViewModel (FIXED)
`observeActiveTranscription` called `markAsViewed` on every flow emission without checking `playedCount`. This caused a cycle: markAsViewed → DB update → flow re-emit → markAsViewed again, incrementing playedCount forever. **Fix:** Guard with `entity.playedCount == 0`.

## Overscroll auto-navigation breaks detail screen
Using `NestedScrollConnection.onPostScroll` to auto-navigate prev/next on overscroll fires on initial load because `scrollState.value == 0` is immediately true. Use HorizontalPager with explicit prev/next buttons instead.
