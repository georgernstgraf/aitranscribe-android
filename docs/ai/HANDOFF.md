# Hand Off

## Current Work: Issue #67 — STT verbose_json for language capture

### Status: Fix applied, test passing, ready for commit

#### What was done this session
1. **Applied Issue #67 fix**: `TranscriptionWorker.kt:237` — changed `"json"` to `"verbose_json"` in `createFormatPart()`
2. **Added and fixed test**: `TranscriptionWorkerTest.worker captures language from verbose_json response`
   - Root cause #1: `languageRepository` relaxed mock couldn't construct `Language` data class (no-arg constructor) → added explicit `coEvery` mock
   - Root cause #2: `FakeTranscriptionRepository.insert()` ignores provided ID and auto-assigns sequential IDs → test now uses returned `insertedId`
3. All unit tests pass (BUILD SUCCESSFUL)

#### Remaining on Issue #67
- [ ] Move `./tmp/english.m4a` and `./tmp/deutsch.m4a` into project as test fixtures
- [ ] Create integration/device test with real audio files for language capture end-to-end
- [ ] Device-test: record audio → verify language captured from verbose_json response

#### Remaining on Issue #66
- [ ] Device-test forced language picker
- [ ] Device-test cleanup toggle with real LLM calls
- [ ] Device-test translate buttons with FlowRow layout
- [ ] "Select/deselect all" on LanguageSettingsScreen
- [ ] Remove debug logging before final release
- [ ] Consider ZAI `verbose_json` support

#### Key Files Changed This Session
- `app/src/main/java/.../service/TranscriptionWorker.kt` — Line 237: `"json"` → `"verbose_json"`
- `app/src/test/.../service/TranscriptionWorkerTest.kt` — New test for language capture, with Language import and mock fix

Last updated: 2026-04-02
