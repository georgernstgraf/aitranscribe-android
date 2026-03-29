# Project State

Current status as of 2026-03-29.

## Current Focus
Issue #31 closed. Tool-agnostic cleanup and knowledge persistence policy established.

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

## Pending
- [ ] Issue #28 follow-up: Write TranscriptionDetailViewModel tests for flatMapLatest architecture
- [ ] Fix setup screen flash on startup
- [ ] Fix settings: API key validation on save, not on timeout
- [ ] Surface post-processing failures to user
- [ ] Port authoritative prompts from `../aitranscribe/main.py`
- [ ] Issue #12: Emulator audio captures silence
- [ ] Issue #22: Compose UI tests

## Blockers
- None

## Next Session Suggestion
Write TranscriptionDetailViewModel unit tests for the `flatMapLatest` architecture. Then tackle setup screen flash.
