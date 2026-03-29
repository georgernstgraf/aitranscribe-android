# Handoff

## Open Tasks

1. [ ] Fix startup: setup screen flashes briefly before main screen (setup screen `startDestination="setup"` always shows first, then auto-navigates when keys found in `loadExistingKeys()`)
2. [ ] Fix settings: move API key validation from timeout-based auto-check to explicit save-button action
3. [ ] Port post-processing prompts from companion project `../aitranscribe` — prompts in Android differ from the authoritative Python project
4. [ ] Investigate and fix OpenRouter API post-processing failures (HTTP error with empty message body)

## Context
- Filter pills were integrated into BottomControlPanel (issue #25, committed)
- Audio file cleanup was hardened: files deleted on success only, orphaned files swept, queued files preserved
- TranscriptionWorker was split: transcription errors → retry, post-processing errors → non-fatal (was causing infinite duplicate transcriptions)
- Companion project `../aitranscribe` is the authoritative source for prompts and pipeline logic
