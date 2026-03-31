# Project State

Current status as of 2026-03-31.

## Current Focus
Kotlin/Room implementation work to align runtime DB with desired schema (#52, #53, #55).

## Completed (this cycle)
- [x] Closed governance issue #54 after adding desired-first Prisma governance docs and Make targets
- [x] Updated desired schema for normalized provider-model-capability shape in #52 (`7ef511a`)
- [x] Added `provider.api_token` to desired schema in #52 (`5a6a637`)
- [x] Completed desired-schema hardening pass in #51 (defaults, index coverage, FK update cascades, redundant index cleanup)
- [x] Created gap issue #55 for provider auth storage migration from `SecurePreferences` to schema-aligned storage

## Pending
- [ ] #52 — Implement Kotlin/Room normalization for provider-model-capability and migration/backfill
- [ ] #53 — Simplify transcription model to match desired schema contract
- [ ] #55 — Align provider auth storage (`api_token`) with desired schema and migrate from `SecurePreferences`

## Blockers
- `cd prisma && make check-schema` still fails due expected desired-vs-runtime drift until #52/#53/#55 implementation work lands

## Next Session Suggestion
Start #52 Kotlin migration: add capability/join entities and Room migration from legacy `models.capabilities` string storage.
