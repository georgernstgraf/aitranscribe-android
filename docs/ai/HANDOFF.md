Branch: main

1. [ ] See #53 — Transcription schema simplification alignment
   - Remaining gap between current runtime and desired transcription model fields/types.

Context:
- #56 is complete: production `SecurePreferences` removed and replaced by `AppSettingsStore` with Room-backed settings/auth.
- Device-introspected schema (`cd prisma && make`) is the reference for current runtime truth.
- Do not modify `prisma/desired/schema.prisma` unless explicitly requested by the user.
