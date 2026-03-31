# Project State

Current status as of 2026-03-31.

## Current Focus
Finish #44 retry/queue hardening and persistence alignment.

## Completed (this cycle)
- [x] Made transcription text model single-field (`text`) and removed runtime `processedText` usage.
- [x] Added migration `11->12` backfilling `text = COALESCE(processed_text, original_text)`.
- [x] Changed retry queue selection to invariant predicate (`text IS NULL AND audio_file_path IS NOT NULL`).
- [x] Implemented atomic STT success update to set `text` and clear `audio_file_path` together.
- [x] Moved recording storage from `cacheDir` to `filesDir/recordings` and enforced no-overwrite creation.
- [x] Added terminal missing-audio handling (`markAudioMissing`) to avoid infinite retry/toast loops.
- [x] Added startup diagnostics log (`startup_audio_diagnostics`) for recordings/unfinished/missing refs.
- [x] Updated workflow so cleanup runs only when requested; summary request runs after STT success.
- [x] Updated unit tests and relevant androidTest files to the renamed `text` model.

## Pending
- [ ] Run full instrumentation test suite after resolving pre-existing androidTest compile blockers outside #44 scope.

## Blockers
- `compileDebugAndroidTestKotlin` currently fails due to pre-existing issues in `RecordingServiceTest` and `AudioRecordingButtonTest`.

## Next Session Suggestion
Close #44 after final push/issue close, then open a dedicated issue to fix androidTest compile blockers and restore full instrumentation gate.
