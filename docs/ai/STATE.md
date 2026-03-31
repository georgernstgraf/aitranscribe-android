# Project State

Current status as of 2026-03-31.

## Current Focus
Post-normalization alignment work: provider auth storage (#55) and transcription schema simplification (#53).

## Completed (this cycle)
- [x] Closed #54 (DB governance) and codified desired-first workflow
- [x] Closed #51 (desired schema hardening for indexes/constraints)
- [x] Implemented #52 runtime normalization in Kotlin/Room:
  - added `CapabilityEntity` + `ModelCapabilityEntity`
  - migrated models to surrogate PK + `external_id`
  - added Room migration `MIGRATION_5_6` with legacy capability backfill
  - updated DAO sync flow, worker mapping, and settings/dropdown model ID usage
- [x] Fixed startup crash in migration path by avoiding FK reference to temporary table names during rename

## Pending
- [ ] #55 — Align provider auth storage (`api_token`) with desired schema and migrate from `SecurePreferences`
- [ ] #53 — Simplify transcription model to match desired schema contract

## Blockers
- `cd prisma && make check-schema` may still fail until remaining runtime alignment (#53/#55) is implemented and introspected

## Next Session Suggestion
Start #55: add `providers.api_token` to Room schema and implement compatibility migration from `SecurePreferences`.
