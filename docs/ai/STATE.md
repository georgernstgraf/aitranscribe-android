# Project State

Current status as of 2026-03-31.

## Current Focus
Final runtime-to-desired alignment on transcription schema simplification (#53).

## Completed (this cycle)
- [x] Closed #54 (DB governance) and codified desired-first workflow
- [x] Closed #51 (desired schema hardening for indexes/constraints)
- [x] Closed #52 (runtime provider-model-capability normalization in Room)
- [x] Implemented #55 provider auth alignment:
  - Room providers table now uses `name` + nullable `api_token`
  - added migration `MIGRATION_6_7` (`display_name` -> `name`, add `api_token`)
  - auth reads are DB-first with SecurePreferences fallback/backfill
  - worker/settings writes now sync provider token into DB
- [x] Refreshed runtime schema snapshot via `cd prisma && make`

## Pending
- [ ] #53 — Simplify transcription model to match desired schema contract

## Blockers
- `cd prisma && make check-schema` may still fail until #53 lands and desired/runtime schemas are reconciled

## Next Session Suggestion
Start #53 by mapping each current `transcriptions` runtime field to desired target, then implement migration and app-layer updates in one pass.
