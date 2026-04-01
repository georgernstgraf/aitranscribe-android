# Project State

Current status as of 2026-04-01.

## Current Focus
No active focus. All open issues addressed.

## Completed (this cycle)
- [x] Issue #59 — Set minSdk to 30, remove old API code:
  - `minSdk` changed from 26 to 30 in `build.gradle.kts`.
  - Removed `WRITE_EXTERNAL_STORAGE` permission from `AndroidManifest.xml` (dead at minSdk 30).
  - Removed `if (Build.VERSION.SDK_INT >= O)` guards in `AITranscribeApp.kt` and `RecordingService.kt`.
  - Added API 31 guard for `MediaRecorder(Context)` in `RecordingService.kt`.
  - Replaced `ContextCompat.checkSelfPermission` with direct `checkSelfPermission` in `MainActivity.kt`.
  - Removed 3 dead dependencies: `threetenbp`, `datastore-preferences`, `security-crypto`.
  - `./gradlew test` passes all tests.

- [x] Issue #60 — Settings screen UI/UX improvements:
  - Moved "Active Providers" section to bottom (above Delete button).
  - Replaced "Save Settings" button with floppy disk icon in TopAppBar.
  - Added online API key validation in `saveSettings()` (format + HTTP check).
  - Centralized key validation in `ValidateApiKeysUseCase` with provider-agnostic methods.
  - Added ZAI key format and online validation support.
  - Hide "Connect Provider" button when all providers connected.
  - Added trash icon to disconnect providers.
  - Renamed "Manage" button to "Authenticate".
  - Added format + online validation to ProviderAuthScreen.
  - Fixed stale provider list refresh when returning from auth screen.
  - Removed redundant "Connected" text from active provider cards.
  - All tests pass, APK deployed to device.

- [x] Issue #63 — Main screen redesign and transcription process streamlining:
  - Removed Raw/Cleanup toggle from main screen.
  - Reorganized BottomControlPanel to single row layout (filter pills + record button).
  - Removed `processingMode` state and related methods from MainViewModel.
  - Removed processing mode storage from AppSettingsStore.
  - Changed TranscriptionWorker to always use RAW mode (no cleanup by default).
  - Cleanup now available via transcription details screen.
  - All tests pass, APK deployed to device.

- [x] Issue #61 — Fixed summary language issues during recording:
  - Added `language` field to `GroqTranscriptionResponse` to capture Whisper API detected language.
  - Modified `transcribeAudio()` to return `TranscriptionResult` with text and language.
  - Updated `markSttSuccess()` to store language in database instead of NULL.
  - Modified `generateSummary()` to include detected language in LLM prompt.
  - Added `buildSummaryPrompt()` with language-specific instructions.
  - Updated summary prompt to explicitly require output in detected language.
  - All tests pass, APK deployed to device.

## Pending
- None.

## Blockers
- `compileDebugAndroidTestKotlin` still has pre-existing failures outside current issue scope.

## Next Session Suggestion
Issue #47 (Database delays require item caching) or Issue #61 (Summary language issues during recording with RAW enabled).
