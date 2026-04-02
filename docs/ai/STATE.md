# Project State

Current status as of 2026-04-02.

## Current Focus
Issue #67 — Switch STT response_format from `json` to `verbose_json` to capture language field. Fix committed, test passing.

## Completed (this cycle)
- [x] Applied Issue #67 fix: `"json"` → `"verbose_json"` in `TranscriptionWorker.createFormatPart()`
- [x] Added `worker captures language from verbose_json response` test — fixed mock issues (Language relaxed mock, FakeTranscriptionRepository.insert() auto-ID)
- [x] All unit tests passing (BUILD SUCCESSFUL)

## Previously Completed (Issue #66)
- [x] Language infrastructure: LanguageEntity, LanguageDao, LanguageRepository with auto-insert
- [x] LanguageSettingsScreen with active/inactive toggle, validation
- [x] SettingsScreen Languages section with "Manage" button
- [x] Detail screen: ModalBottomSheet for forced language selection, FlowRow translate buttons, cleanup toggle
- [x] Fixed Settings state reset bug, loadAvailableLanguages DB fix, ModalBottomSheet confirmValueChange fix
- [x] TranscriptionDetailViewModelTest: 4 new tests all passing

## Pending
- [ ] Move `./tmp/english.m4a` and `./tmp/deutsch.m4a` into project as test fixtures
- [ ] Create integration/device test using real audio files for language capture end-to-end
- [ ] Device-test detail screen UI (language picker, translate buttons, cleanup toggle)
- [ ] "Select/deselect all" control on LanguageSettingsScreen
- [ ] Consider ZAI `verbose_json` support
- [ ] Remove debug logging from SettingsScreen/SettingsViewModel before final release

## Blockers
- None.

## Next Session Suggestion
Move test audio files into the project and create an end-to-end language capture test. Then device-test the detail screen UI.
