# Project State

Current status as of 2026-04-02.

## Current Focus
Issue #66 — Define database schema for languages and improve Kotlin code (IN PROGRESS)

## Completed (this cycle)
- [x] Created LanguageEntity, LanguageDao, and LanguageRepository
- [x] Implemented `ensureLanguageExists()` for auto-inserting new languages from STT
- [x] Updated TranscriptionWorker to auto-update languages table on STT response
- [x] Added foreign key constraint from transcriptions to languages table
- [x] Fixed Room schema validation by adding proper annotations to TranscriptionEntity
- [x] Added logging for STT language detection in TranscriptionWorker
- [x] All 120 tests passing
- [x] APK deployed and verified working

## Pending
- [ ] Complete remaining Issue #66 scope (if any)

## Blockers
- None.

## Testing Infrastructure
- **Unit tests**: JUnit 5, all passing (120 tests)
- **Instrumentation tests**: JUnit 4, all passing on API 35 emulator
- **Emulator**: API 35 (Android 15) - `Medium_Phone_API_35` AVD configured

## Next Session Suggestion
Review Issue #66 for remaining scope or proceed with other open issues.

## Recent Architecture Decisions
1. Languages table auto-populates from STT-detected languages
2. Unknown ISO codes use uppercase code as display name (e.g., "XX")
3. New languages are immediately active for translation UI
4. Room entities must declare foreign keys to match database schema exactly
