# Project State

Current status as of 2026-03-29.

## Current Focus
Issue #26 closed. Issue #29 (share with summary) has uncommitted implementation in working tree. End-to-end device test passed.

## Completed This Session
- [x] Issue #26: Unit tests, test infrastructure, markAsViewed bug fix — closed
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
- [x] End-to-end device testing on real hardware — passed

## Pending
- [ ] Issue #29: Share transcriptions with summary prepended (uncommitted changes in working tree)
- [ ] Issue #12: Emulator audio captures silence
- [ ] Issue #22: Compose UI tests
- [ ] Fix settings: API key validation on save, not on timeout
- [ ] Surface post-processing failures to user
- [ ] Port authoritative prompts from `../aitranscribe/main.py`

## Blockers
- None

## Next Session Suggestion
Commit and push Issue #29 changes (share with summary — already implemented in working tree).
