# Project State

Current status as of 2026-04-01.

## Current Focus
Issue #46 cleanup/summary language and prompt routing stabilization.

## Completed (this cycle)
- [x] Added language-aware detail post-processing routing with deterministic prompt selection.
- [x] Standardized cleanup-enabled prompt order to `prompt.cleanup` then `prompt.cleanup.<lang|null>`.
- [x] Kept detail cleanup-off actions on `prompt.translate.<en|de>`.
- [x] Removed duplicated system-wrapper prompt text in post-processing messages.
- [x] Removed duplicate summary generation in recording cleanup path.
- [x] Added sanitized prompt preview logging (`{{TEXT}}`) and switched detail card to summary prompt preview.
- [x] Kept summary generation prompt path on `prompt.summary` + `prompt.user.transcription` only.
- [x] Revalidated unit tests and debug build/install on physical device.

## Pending
- [ ] Run full instrumentation gate once pre-existing androidTest compile blockers are addressed.

## Blockers
- `compileDebugAndroidTestKotlin` still has pre-existing failures outside this issue scope.

## Next Session Suggestion
Open/fix dedicated instrumentation-test issue and restore `compileDebugAndroidTestKotlin` + `connectedAndroidTest` as reliable gate.
