Branch: main

1. [ ] See #52 — Provider/model/capability normalization in Kotlin/Room
   - Gap: desired has `provider_model` + `capability` + `model_capability`; runtime still stores capabilities as a string in `models`.

2. [ ] See #55 — Provider auth storage alignment
   - Gap: desired has `provider.api_token`; runtime auth lives in `SecurePreferences` and Room `providers` lacks `api_token`.

3. [ ] See #53 — Transcription schema simplification alignment
   - Gap: desired transcriptions (`seen`, `DateTime`, slimmer fields) diverges from runtime fields (`played_count`, `retry_count`, `post_processing_type`, `stt_model`, `llm_model`, `created_at` string).

Context:
- #51 (desired-schema hardening) is complete and ready to close after commit/push.
- Desired schema updates were committed in #52 (`7ef511a`, `5a6a637`) and hardened in #51 (uncommitted before finish).
- Governance contract: `prisma/GOVERNANCE.md`.
- Refresh runtime schema: `cd prisma && make`.
- Drift check: `cd prisma && make check-schema` (expected to fail until runtime implementation catches up).
