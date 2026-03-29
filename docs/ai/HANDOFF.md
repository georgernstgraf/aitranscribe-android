# Handoff

## Open Tasks

1. [ ] Issue #28: Verify swipe fix on device, then write new TranscriptionDetailViewModel tests for `flatMapLatest` architecture
2. [ ] Fix startup: setup screen flashes briefly before main screen (`startDestination="setup"` always shows first)
3. [ ] Fix settings: move API key validation from timeout-based auto-check to explicit save-button action
4. [ ] Port post-processing prompts from companion project `../aitranscribe`
5. [ ] Surface post-processing failures to user (currently silent)
6. [ ] Issue #22: Compose UI tests

## Context
- Issue #28: `flatMapLatest` fix committed and deployed. Detail screen swipe bug root cause was uncancelled Flow collectors racing to update `_uiState`. Fix deployed to physical device.
- Knowledge migrated from `.opencode/knowledge/` to `docs/ai/` — this is now the canonical knowledge location (AGENTS.md updated)
- Companion project `../aitranscribe` is the authoritative source for prompts and pipeline logic
