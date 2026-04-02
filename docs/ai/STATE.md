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

- [x] Issue #62 — Removed broken OpenRouter STT code:
  - Removed OpenRouter from STT providers list (no dedicated STT endpoint exists).
  - Removed broken `transcribeAudio()` method from OpenRouterApiService.
  - Removed OpenRouter case from TranscriptionWorker STT switch.
  - Updated tests to reflect changes.
  - Tagged issue with "not now" label for potential future reconsideration.
  - OpenRouter remains as LLM provider (works correctly via chat completions).

## Pending
- None.

## Blockers
- None. Instrumentation tests now compile and run successfully on API 35.

## Testing Infrastructure
- **Unit tests**: JUnit 5, all passing (115 tests)
- **Instrumentation tests**: JUnit 4, all passing (31 tests) on API 35 emulator
- **Emulator**: API 35 (Android 15) - `Medium_Phone_API_35` AVD configured and working
- **API 36 compatibility**: Known Espresso/Compose testing issues on API 36+, use API 35 for UI tests

## Next Session Suggestion
Issue #47 (Database delays require item caching) — Performance issue causing lag when swiping through transcriptions. Medium complexity, high user impact.

## Current Provider Support Status
- **STT Providers**: GROQ (working), ZAI (working), OpenRouter (removed - no STT endpoint)
- **LLM Providers**: OpenRouter (working), ZAI (working), GROQ (working)
- **Audio Formats**: .m4a (GROQ), .mp3/.wav (ZAI)

## Recent Architecture Decisions
1. Always capture Whisper language detection for accurate summaries
2. Remove non-functional code paths (OpenRouter STT) rather than leaving broken
3. Use navigation-aware refresh for settings screens
4. Prompt logging uses {{TEXT}} placeholder for privacy
5. Language-aware prompts prevent LLM guessing
