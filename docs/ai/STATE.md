# Project State

Current status as of 2026-03-31.

## Current Focus
Complete remaining desired/runtime convergence for transcription schema (#53) and full SecurePreferences removal (#56).

## Completed (this cycle)
- [x] Closed #55 (provider auth storage aligned to `providers.api_token`)
- [x] Implemented #57 ZAI coding-endpoint resilience for post-processing
- [x] Unified runtime auth token reads to DB-backed provider tokens (removed legacy auth-key helper usage)
- [x] Confirmed build/test health after changes (`./gradlew test`, `./gradlew assembleDebug`)

## Pending
- [ ] #53 — Simplify transcription model to match desired schema contract
- [ ] #56 — Remove `SecurePreferences` and migrate remaining app settings into Room + desired schema updates

## Blockers
- None hard; ZAI behavior depends on provider package/key type and endpoint compatibility

## Next Session Suggestion
Start #56 by designing a Room-backed settings model in `prisma/desired/schema.prisma`, then migrate non-auth `SecurePreferences` values (provider/model selections, processing mode, share app) to DB.
