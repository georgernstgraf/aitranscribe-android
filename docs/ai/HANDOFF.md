Branch: main

1. [ ] See #56 — Remove `SecurePreferences` and migrate settings to Room + desired schema
   - Remaining scope: non-auth settings still in `SecurePreferences` (`stt/llm provider/model selection`, `processing_mode`, `preferred_share_app`).

2. [ ] See #53 — Transcription schema simplification alignment
   - Remaining gap between desired and runtime transcription fields/types.

Context:
- #57 added ZAI coding-endpoint fallback for post-processing 429 package/balance errors.
- Auth token source is now DB-backed (`providers.api_token`) in runtime code paths.
- Full SecurePreferences removal is intentionally tracked as separate work in #56.
