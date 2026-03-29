# Project State

Current status as of 2026-03-29.

## Current Focus
Unit tests for all ViewModels and UseCases passing (76 tests, 19 classes). Bug fix for infinite markAsViewed loop committed.

## Completed This Session
- [x] Issue #28: Swipe fix (flatMapLatest) committed, deployed, and closed
- [x] Issue #31: Defined policy for project-specific skills and tool-agnosticism
- [x] Rewrote AGENTS.md — positive project instructions, no anti-tool framing
- [x] Moved `.opencode/skills/orchestrator/` → `skills/orchestrator/` (project root)
- [x] Removed `.opencode/knowledge/` (superseded by `docs/ai/`)
- [x] Removed `memory/` (tool session artifact)
- [x] Updated `.gitignore` to ignore `.opencode/`, `memory/`, persona files
- [x] Added persistence triggers and content guide to `docs/ai/CONVENTIONS.md`
- [x] Cleaned all tool-specific references from tracked files
- [x] Unit tests: TranscriptionDetailViewModelTest, SetupViewModelTest, SettingsViewModelTest, SearchViewModelTest, ValidateApiKeysUseCaseTest, ShareTranscriptionUseCaseTest, ValidateApiKeysIntegrationTest
- [x] Bug fix: infinite markAsViewed loop in TranscriptionDetailViewModel (playedCount guard)
- [x] Test infrastructure: org.json dependency, .env loader, TestEnv helper, FakeTranscriptionRepository fixes

## Pending
- [ ] Fix setup screen flash on startup
- [ ] Fix settings: API key validation on save, not on timeout
- [ ] Surface post-processing failures to user
- [ ] Port authoritative prompts from `../aitranscribe/main.py`
- [ ] Issue #12: Emulator audio captures silence
- [ ] Issue #22: Compose UI tests
- [ ] Issue #29: Share transcriptions with summary prepended

## Blockers
- None

## Next Session Suggestion
Tackle setup screen flash or Issue #29 (share with summary).
