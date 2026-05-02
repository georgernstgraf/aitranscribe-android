# Project State

Current status as of 2026-05-02.

## Current Focus
Issue #64 closed. Capabilities removed from DB. No active issue.

## Completed (this cycle)
- [x] Removed `capabilities` and `model_capabilities` tables (migration 16→17)
- [x] Deleted `CapabilityEntity.kt`, `ModelCapabilityEntity.kt`
- [x] Simplified `ProviderModelDao`, `ModelSyncWorker`, `ModelCatalogEntry`
- [x] Updated `prisma/desired/schema.prisma` and `ARCHITECTURE.md`
- [x] Updated `ProviderModelDaoTest`
- [x] 123/123 unit tests pass, build succeeds

## Pending
- [ ] Move `./tmp/english.m4a` and `./tmp/deutsch.m4a` into project as test fixtures
- [ ] Create integration/device test using real audio files for language capture end-to-end
- [ ] Device-test detail screen UI (language picker, translate buttons, cleanup toggle)
- [ ] "Select/deselect all" control on LanguageSettingsScreen
- [ ] Remove debug logging from SettingsScreen/SettingsViewModel before final release

## Blockers
- None.

## Next Session Suggestion
Pick from open issues: #47 (DB caching for swiping), #45 (sharing popup), #62 (GROQ API / OpenRouter audio access).
