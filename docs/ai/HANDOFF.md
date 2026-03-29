# Handoff

## Open Tasks

1. [ ] Issue #28 follow-up: Write TranscriptionDetailViewModel tests for `flatMapLatest` architecture
2. [ ] Fix startup: setup screen flashes briefly before main screen (`startDestination="setup"` always shows first)
3. [ ] Fix settings: move API key validation from timeout-based auto-check to explicit save-button action
4. [ ] Port post-processing prompts from companion project `../aitranscribe`
5. [ ] Surface post-processing failures to user (currently silent)
6. [ ] Issue #22: Compose UI tests

## Context
- Knowledge files in `docs/ai/` — canonical knowledge location
- Project-specific skill at `skills/orchestrator/`
- Companion project `../aitranscribe` is the authoritative source for prompts and pipeline logic
- `.opencode/` and `memory/` are gitignored — not part of the project
