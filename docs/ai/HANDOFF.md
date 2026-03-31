Branch: main

1. [ ] See #55 — Provider auth storage alignment
   - Desired has `provider.api_token`; runtime auth currently relies on `SecurePreferences`.

2. [ ] See #53 — Transcription schema simplification alignment
   - Desired transcription shape still diverges from runtime fields/types.

Context:
- #52 is implemented locally: normalized `capabilities` + `model_capabilities`, model `external_id`, DAO+worker updates, and migration `5->6`.
- Migration crash was fixed by seeding join rows in a temporary non-FK table and creating final FK table after `models_new` rename.
- Validate runtime schema after next alignment steps with `cd prisma && make` and compare using `cd prisma && make check-schema`.
