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
- CI builds successful; release APKs are unsigned as of 2026-03-29.
- On-device SQLite debugging works if `sqlite3` is copied into the app sandbox via `run-as ... sh -c "cp /data/local/tmp/sqlite3 files/sqlite3 && chmod 700 files/sqlite3 ..."`.
