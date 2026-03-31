Branch: main

1. [ ] See #50 — Prisma

Context:
- #53 is complete: runtime `transcriptions` now uses `seen` and no longer stores `stt_model`, `llm_model`, `post_processing_type`, `retry_count`.
- Device-introspected schema (`cd prisma && make`) is the current runtime truth reference.
- Do not modify `prisma/desired/schema.prisma` unless explicitly requested by the user.
