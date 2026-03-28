# Current State (2026-03-29)

## Current Focus
Issue #24 complete — full UI redesign matching SVG mockup, processing modes wired through pipeline.

## Completed This Session
- [x] Issue #24: UI redesign — TranscriptionItem with pill badges, accent bar, summary title
- [x] BottomControlPanel with radio buttons + mic button
- [x] ProcessingMode (RAW/CLEANUP/ENGLISH) persisted and wired to Worker
- [x] Summary field added to TranscriptionEntity, DB migration v1→v2
- [x] PostProcessTextUseCase.generateSummary() for LLM-generated titles
- [x] OpenRouterApiService DI provider added to NetworkModule
- [x] HiltSmokeTest instrumentation test added
- [x] All GRAMMAR references renamed to CLEANUP
- [x] Tested on physical device

## Pending
- [ ] Issue #12: Emulator audio captures silence (use physical device instead)
- [ ] Issue #22: Compose UI tests (lower priority)

## Blockers
- None

## Next Session Suggestion
Test full recording → transcription → post-processing → summary generation pipeline on physical device with real audio.
