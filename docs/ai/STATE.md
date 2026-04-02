# Project State

Current status as of 2026-04-02.

## Current Focus
Issue #66 — Transcription detail screen enhancements (IN PROGRESS, checkpoint committed)

## Completed (this cycle)
- [x] Created LanguageEntity, LanguageDao, and LanguageRepository
- [x] Implemented `ensureLanguageExists()` for auto-inserting new languages from STT
- [x] Updated TranscriptionWorker to auto-update languages table on STT response
- [x] Added foreign key constraint from transcriptions to languages table
- [x] Fixed Room schema validation by adding proper annotations to TranscriptionEntity
- [x] Added logging for STT language detection in TranscriptionWorker
- [x] Created LanguageSettingsScreen for managing active languages
- [x] Added language management UI with check/uncheck functionality
- [x] Implemented validation to prevent unchecking last active language
- [x] Added Languages row to Settings screen with "Manage" button
- [x] Fixed Settings state reset bug (changed `SettingsUiState()` to `.copy()` in loadSettings)
- [x] Added `cleanupEnabled` to TranscriptionDetailUiState (defaults based on cleanedText)
- [x] Added `toggleCleanup()` to TranscriptionDetailViewModel
- [x] Fixed `translateTo()` to respect `cleanupEnabled` toggle
- [x] Added ModalBottomSheet for forced language selection when language is null
- [x] Replaced TextButton row with FlowRow + OutlinedButton for language translate buttons
- [x] Language buttons display `language.name` and filter out current source language
- [x] Fixed loadAvailableLanguages() to use languageRepository.getActiveLanguages() (DB query) instead of AppSettingsStore
- [x] Fixed ModalBottomSheet confirmValueChange to only block Hidden transition
- [x] Added 4 new tests for cleanup toggle and language selection
- [x] All tests passing (BUILD SUCCESSFUL)

## Pending
- [ ] Test forced language picker on device with language=null transcriptions
- [ ] Test cleanup toggle behavior on device with real LLM calls
- [ ] Test translate buttons with FlowRow layout on device
- [ ] Add "select/deselect all" control to LanguageSettingsScreen
- [ ] Remove debug logging from SettingsScreen/SettingsViewModel before final release
- [ ] Review Issue #66 for any remaining scope

## Blockers
- None.

## Next Session Suggestion
Manual device testing of the three new UI features on the detail screen, then address remaining items from Issue #66.
