# Hand Off

No pending tasks. Last cleared: 2026-04-01.

## This Session
- **Issue #59 closed** — Minimum SDK raised from 26 to 30:
  - Removed dead `WRITE_EXTERNAL_STORAGE` permission, API 26 NotificationChannel guards, and 3 unused dependencies (`threetenbp`, `datastore-preferences`, `security-crypto`).
  - Added API 31 guard for `MediaRecorder(Context)` in `RecordingService.kt`.
  - Replaced `ContextCompat.checkSelfPermission` with direct `checkSelfPermission` in `MainActivity.kt`.
  - Discovered pre-existing bug: `MediaRecorder(Context)` requires API 31, meaning the app was already effectively minSdk 31 for the recording feature before this change.
  - `./gradlew test` passes all tests.
