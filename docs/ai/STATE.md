# Project State

Current status as of 2026-03-30.

## Current Focus
Dynamic provider models and offline queueing reliability (#41).

## Completed (this cycle)
- [x] Migrated API keys to provider-centric auth flow
- [x] Rebuilt Settings UI with separate Provider list and Active Search model dropdowns
- [x] Implemented `ModelSyncWorker` to fetch provider models in the background
- [x] Refactored `TranscriptionWorker` to keep audio file active until LLM post-processing succeeds

## Pending
- None.

## Blockers
- None.

## Next Session Suggestion
Test the background sync behavior after 12 hours, and ensure queueing reliably picks up old files when connectivity/keys are restored.
