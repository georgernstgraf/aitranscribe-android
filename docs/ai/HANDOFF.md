# Handoff

## Open Tasks

1. [ ] See #22 — Compose UI tests
2. [ ] See #12 — Emulator audio captures silence
3. [ ] Port authoritative prompts from `../aitranscribe/main.py`
4. [ ] Consider: retry button in post-processing warning banner
5. [ ] Consider: notification channel for post-processing failures

## Context
- Knowledge files in `docs/ai/` — canonical knowledge location
- Project-specific skill at `skills/orchestrator/`
- Companion project `../aitranscribe` is the authoritative source for prompts and pipeline logic
- `.opencode/` and `memory/` are gitignored — not part of the project
- ZAI (api.z.ai) is now an LLM provider alongside OpenRouter; GROQ remains sole STT provider
- `ProviderConfig.kt` is the single source of truth for model lists — update there when adding/removing models
- `TranscriptionStatus.COMPLETED_WITH_WARNING` used when post-processing fails — raw text still shown
