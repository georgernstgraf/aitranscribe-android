# Project State

Current status as of 2026-03-29.

## Current Focus
Issue #28 closed. Knowledge migration to `docs/ai/` complete.

## Completed This Session
- [x] Issue #28: Root cause analysis and fix — replaced uncancelled Flow collectors with `flatMapLatest`
- [x] Issue #28: Fix committed, pushed, and deployed to physical device
- [x] Knowledge migrated from `.opencode/knowledge/` to `docs/ai/` (persistent workflow)
- [x] AGENTS.md updated to reference `docs/ai/` bootstrap sequence
- [x] Added auto-deploy convention (adb install after build if device present)
- [x] Added trunk-based workflow convention (git pull before work)
- [x] Assessed companion AGENTS.md knowledge persistence — identified gaps, documented in our conventions

## Pending
- [ ] Issue #28 follow-up: Write new TranscriptionDetailViewModel tests for flatMapLatest architecture
- [ ] Fix setup screen flash on startup
- [ ] Fix settings: API key validation on save, not on timeout
- [ ] Surface post-processing failures to user
- [ ] Port authoritative prompts from `../aitranscribe/main.py`
- [ ] Issue #12: Emulator audio captures silence
- [ ] Issue #22: Compose UI tests

## Blockers
- None

## Next Session Suggestion
Write TranscriptionDetailViewModel unit tests for the new `flatMapLatest` architecture. Then tackle setup screen flash.
