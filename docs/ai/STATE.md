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

## Pending
- None.

## Blockers
- `compileDebugAndroidTestKotlin` still has pre-existing failures outside current issue scope.

## Next Session Suggestion
Issue #47 (Database delays require item caching) or Issue #61 (Summary language issues during recording with RAW enabled).
