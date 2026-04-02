# Hand Off

## Current Work: Issue #66 — Transcription detail screen enhancements

### Checkpoint: Detail screen language picker, translate buttons, cleanup toggle

#### What was implemented

1. **Forced language picker (ModalBottomSheet)**
   - When `transcription.language == null`, a non-dismissible bottom sheet appears
   - Shows all active languages from DB (`languageRepository.getActiveLanguages()`)
   - User must pick one; `confirmValueChange` blocks only the `Hidden` transition
   - File: `TranscriptionDetailScreen.kt:92-98` (sheet state) + `LanguagePickerBottomSheet` composable

2. **Translate buttons with FlowRow + OutlinedButton**
   - Replaced single `Row` of `TextButton` with `FlowRow` of `OutlinedButton`
   - Displays `language.name` (e.g., "German") instead of ISO code
   - Filters out the transcription's own source language
   - File: `TranscriptionDetailScreen.kt:226-240`

3. **Cleanup toggle (Switch)**
   - `cleanupEnabled: Boolean` in `TranscriptionDetailUiState`
   - Default: `true` when `cleanedText == null`, `false` when cleaned text exists
   - Reset on each page swipe (set in `observeActiveTranscription()`)
   - `translateTo()` passes `_uiState.value.cleanupEnabled` to PostProcessTextUseCase
   - File: `TranscriptionDetailViewModel.kt:139-141` (toggle), `TranscriptionDetailViewModel.kt:122` (used in translate)

4. **Bug fix: loadAvailableLanguages() now reads from DB**
   - Old: `appSettingsStore.getActiveLanguages()` returned empty (preference key never set)
   - New: `languageRepository.getActiveLanguages()` queries Room directly (`is_active = 1`)
   - File: `TranscriptionDetailViewModel.kt:66-70`

5. **Bug fix: ModalBottomSheet wouldn't open**
   - Old: `confirmValueChange = { false }` blocked ALL state transitions including show
   - New: `confirmValueChange = { it != SheetValue.Hidden }` blocks only dismiss
   - File: `TranscriptionDetailScreen.kt:92-96`

#### Remaining on Issue #66
- [ ] Device-test forced language picker
- [ ] Device-test cleanup toggle with real LLM calls
- [ ] Device-test translate button layout
- [ ] "Select/deselect all" on LanguageSettingsScreen
- [ ] Remove debug logging before final release

#### Key Files Changed
- `app/src/main/java/.../ui/screen/TranscriptionDetailScreen.kt` — Major rewrite (ModalBottomSheet, FlowRow, Switch)
- `app/src/main/java/.../ui/viewmodel/TranscriptionDetailViewModel.kt` — cleanupEnabled, toggleCleanup(), loadAvailableLanguages fix
- `app/src/test/.../ui/viewmodel/TranscriptionDetailViewModelTest.kt` — 4 new tests

Last updated: 2026-04-02
