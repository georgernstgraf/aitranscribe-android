# Project State

Current status as of 2026-05-02.

## Current Focus
Issues #66, #67, #68, #69 all closed. Language infrastructure complete.

## Completed (this cycle)
- [x] Created `docs/ai/ARCHITECTURE.md` with database schema, system overview, migration history
- [x] Raised knowledge file line limit from 200 to 1000 in CONVENTIONS.md
- [x] Fixed Whisper language name → BCP-47 code mismatch (#69)
  - Added `WhisperLanguageMapper` with all 100 Whisper languages
  - Updated `TranscriptionWorker` to map before `ensureLanguageExists`/`markSttSuccess`
  - Migration 14→15: remaps existing stub `languagesId` values, deletes orphaned stubs
  - Migration 15→16: seeds 63 inactive languages with proper native names
  - DB version bumped to 16
- [x] Expanded language support from 37 to all 100 Whisper languages
- [x] Closed issues #66, #67, #68, #69

## Previously Completed (Issue #66/#67)
- [x] Language infrastructure: LanguageEntity, LanguageDao, LanguageRepository with auto-insert
- [x] LanguageSettingsScreen with active/inactive toggle
- [x] Detail screen: ModalBottomSheet for forced language selection, FlowRow translate buttons
- [x] Switched STT response_format to verbose_json for language capture

## Pending
- [ ] Move `./tmp/english.m4a` and `./tmp/deutsch.m4a` into project as test fixtures
- [ ] Create integration/device test using real audio files for language capture end-to-end
- [ ] Device-test detail screen UI (language picker, translate buttons, cleanup toggle)
- [ ] "Select/deselect all" control on LanguageSettingsScreen
- [ ] Remove debug logging from SettingsScreen/SettingsViewModel before final release

## Blockers
- None.

## Next Session Suggestion
Pick from open issues: #64 (remove model capabilities), #47 (DB caching for swiping), #45 (sharing popup).
