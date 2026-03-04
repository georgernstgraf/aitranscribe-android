# Handoff

Pending tasks for the next session.

## Branch: main

## Open Tasks

- [ ] **Diagnose empty GROQ transcription**
  - GROQ returns `" ."` (length=2) instead of actual text
  - Audio files exist with valid durations (6s, 11s)
  - Suspect emulator mic captures silence despite `-allow-host-audio`
  - Next step: pull .m4a from emulator and play locally
  - File path on device: `/data/data/com.georgernstgraf.aitranscribe/files/`
  - Relevant code: `service/TranscriptionWorker.kt` (API call + logging)

- [ ] **Implement LLM post-processing**
  - Not yet started; depends on transcription working first

- [ ] **Add error handling for API failures**
  - TranscriptionWorker has no retry/backoff logic
  - No user-facing error messages for auth failures, timeouts, etc.

## Context

- GitHub Issue: #12 (open)
- Latest commit: 3edeb85
- All crash fixes are in place; the app is stable
- The only functional gap is GROQ returning empty text
