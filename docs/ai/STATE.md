# Project State

Current status as of 2026-03-29.

## Current Focus
Issue #32 (ZAI provider + model dropdowns + failure feedback) completed and committed. CI build fixed.

## Completed This Session
- [x] Installed arm64 `sqlite3` on device from `SQLite-v3.51.1-for-magisk.multi-arch.zip`
- [x] Verified on-device DB access by copying `sqlite3` into app sandbox and querying `aitranscribe.db`
- [x] Issue #32: ZAI as LLM provider — `ZaiApiService`, `ProviderConfig`, settings dropdowns
- [x] Issue #32: Model selection dropdowns replacing free-text fields (STT + LLM)
- [x] Issue #32: Post-processing failure feedback — `COMPLETED_WITH_WARNING` status + amber warning banner
- [x] CI: Fixed failing GitHub Actions by removing broken keystore signing step (now unsigned release APKs for safety/simplicity)
- [x] Unit tests: ProviderConfigTest (11 cases), PostProcessTextUseCaseTest updated
- [x] API testing: confirmed ZAI chat completions work
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
