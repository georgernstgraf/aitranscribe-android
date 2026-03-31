# Project State

Current status as of 2026-03-31.

## Current Focus
Dynamic provider models and offline queueing reliability (#41).

## Completed (this cycle)
- [x] Migrated API keys to provider-centric auth flow
- [x] Rebuilt Settings UI with separate Provider list and Active Search model dropdowns
- [x] Implemented `ModelSyncWorker` to fetch provider models in the background
- [x] Refactored `TranscriptionWorker` to keep audio file active until LLM post-processing succeeds
- [x] Backfilled database, sync worker, and viewmodel tests
- [x] Fixed LLM model routing to properly dispatch Groq STT and Groq LLM requests to the correct endpoints
- [x] Fixed saving separate preferences for LLM and STT models to prevent UI state overwrites

## Pending
- None.

## Blockers
- None.

## Next Session Suggestion
Address client-side audio conversion (.m4a to .mp3) to support ZAI ASR functionality (Issue #43).
