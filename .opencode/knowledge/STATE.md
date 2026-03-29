# Current State (2026-03-29)

## Current Focus
Post-processing pipeline fixes — silent failures need surfacing, prompts need porting from companion project.

## Companion Project
- `../aitranscribe` (Python/TUI) is the **lead project**
- This Android app mirrors its prompts, modes, and pipeline
- Key files: `core.py` (LLM calls), `main.py` (prompts, modes), `tui.py` (UI)

## Completed This Session
- [x] Issue #25: Filter pills integrated into BottomControlPanel with SpaceBetween layout
- [x] Audio file cleanup: deleted on success, preserved on retry, orphans swept
- [x] TranscriptionWorker split: transcription retry separate from non-fatal post-processing
- [x] Better error logging in PostProcessTextUseCase (HTTP code + error body)
- [x] Orphaned audio files manually cleaned from device cache

## Pending
- [ ] Fix setup screen flash on startup (check keys before navigating)
- [ ] Fix settings: API key validation on save, not on timeout
- [ ] Surface post-processing failures to user (currently silent)
- [ ] Port authoritative prompts from `../aitranscribe/main.py` to Android
- [ ] Issue #12: Emulator audio captures silence (use physical device)
- [ ] Issue #22: Compose UI tests

## Blockers
- Post-processing always fails on device — likely wrong model name or API key issue (need to capture HTTP code from logs after next recording)

## Next Session Suggestion
1. Make a recording with non-RAW mode, capture the post-processing HTTP error from logcat
2. Port prompts from `../aitranscribe/main.py` to `PostProcessTextUseCase.kt`
3. Surface post-processing failures in the UI (status indicator or notification)
