# Current State (2026-03-29)

## Current Focus
Detail screen navigation fix and test infrastructure stability.

## Companion Project
- `../aitranscribe` (Python/TUI) is the **lead project**
- This Android app mirrors its prompts, modes, and pipeline
- Key files: `core.py` (LLM calls), `main.py` (prompts, modes), `tui.py` (UI)

## Completed This Session
- [x] Issue #27: Fixed detail screen navigation — removed broken overscroll auto-nav, restored prev/next toolbar buttons
- [x] Added tap-outside-to-save on TranscriptionDetailScreen (focus loss triggers save)
- [x] Fixed FakeTranscriptionRepository to implement new interface methods
- [x] Fixed PostProcessTextUseCaseTest assertion (originalText vs processedText)
- [x] Fixed SettingsViewModelTest constructor signature (added ValidateApiKeysUseCase)
- [x] Fixed ViewModel test timeouts — no more advanceUntilIdle() with infinite collectors

## Pending
- [ ] Fix setup screen flash on startup (check keys before navigating)
- [ ] Fix settings: API key validation on save, not on timeout
- [ ] Surface post-processing failures to user (currently silent)
- [ ] Port authoritative prompts from `../aitranscribe/main.py` to Android
- [ ] Issue #12: Emulator audio captures silence (use physical device)
- [ ] Issue #22: Compose UI tests

## Blockers
- None

## Next Session Suggestion
1. Make a recording with non-RAW mode, capture the post-processing HTTP error from logcat
2. Port prompts from `../aitranscribe/main.py` to `PostProcessTextUseCase.kt`
3. Surface post-processing failures in the UI (status indicator or notification)
