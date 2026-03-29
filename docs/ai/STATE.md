# Project State

Current status as of 2026-03-29.

## Current Focus
Issue #32 (ZAI provider + model dropdowns + failure feedback) committed and closed.

## Completed This Session
- [x] Issue #29: Share transcriptions with summary prepended — `Transcription.getShareText()` + tests
- [x] Issue #32: ZAI as LLM provider — `ZaiApiService`, `ProviderConfig`, settings dropdowns
- [x] Issue #32: Model selection dropdowns replacing free-text fields (STT + LLM)
- [x] Issue #32: Post-processing failure feedback — `COMPLETED_WITH_WARNING` status + amber warning banner
- [x] Issue #26: Unit tests, test infrastructure, markAsViewed bug fix — closed
- [x] Issue #28: Swipe fix (flatMapLatest) committed, deployed, and closed
- [x] Issue #31: Defined policy for project-specific skills and tool-agnosticism
- [x] Unit tests: ProviderConfigTest (11 cases), PostProcessTextUseCaseTest updated
- [x] API testing: confirmed ZAI chat completions work, ASR rejects .m4a
- [x] Device testing: installed and verified on real hardware

## Pending
- [ ] Issue #12: Emulator audio captures silence
- [ ] Issue #22: Compose UI tests
- [ ] Port authoritative prompts from `../aitranscribe/main.py`
- [ ] Consider: retry button in post-processing warning banner
- [ ] Consider: notification channel for post-processing failures

## Blockers
- None

## Next Session Suggestion
Port authoritative prompts from `../aitranscribe/main.py`, or tackle Issue #22 (Compose UI tests).
