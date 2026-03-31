# Project State

Current status as of 2026-03-31.

## Current Focus
DB governance rollout with desired-first Prisma schema workflow and follow-up schema improvement issues.

## Completed (this cycle)
- [x] Added root `Makefile` target `pull-db` to force-stop app, clean local copies, and pull DB/WAL/SHM via `adb` + `run-as`
- [x] Created schema roadmap issues: #51 (schema hardening), #52 (provider-model-capability normalization), #53 (transcription simplification)
- [x] Created governance issue #54
- [x] Added `prisma/GOVERNANCE.md` documenting desired-first schema workflow
- [x] Updated `prisma/Makefile` with `refresh-device` and `check-schema` targets

## Pending
- [ ] #51 — Improve `prisma/desired/schema.prisma` indexes/constraints while keeping runtime compatibility
- [ ] #52 — Normalize provider-model-capability relationship with migration/backfill plan
- [ ] #53 — Reduce transcription schema/object complexity with migration plan

## Blockers
- `make check-schema` currently fails because `prisma/desired/schema.prisma` intentionally drifts from `prisma/device/schema.prisma`

## Next Session Suggestion
Start with issue #51: align `prisma/desired/schema.prisma` to current runtime schema first, then apply hardening changes incrementally with passing `make check-schema`.
