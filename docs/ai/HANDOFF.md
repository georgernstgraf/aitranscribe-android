Branch: main

1. [ ] See #53 — Transcription schema simplification alignment
   - Desired transcription shape still diverges from runtime (`played_count`, `retry_count`, `post_processing_type`, model fields, timestamp type).

Context:
- #55 implemented DB-first provider auth storage (`providers.api_token`) with migration `6->7` and SecurePreferences compatibility fallback.
- Runtime schema snapshot was refreshed with `cd prisma && make`; include `prisma/device/schema.prisma` updates when reviewing drift.
- Remaining schema convergence work is centered on the `transcriptions` model.
