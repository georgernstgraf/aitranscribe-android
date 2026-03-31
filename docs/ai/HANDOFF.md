Branch: main

1. [ ] See #51 — DB Schema Hardening: improve `prisma/desired/schema.prisma` indexes and constraints
2. [ ] See #52 — Normalize provider-model-capability relationship
3. [ ] See #53 — Simplify transcription data model and reduce object complexity

Context:
- Governance workflow is documented in `prisma/GOVERNANCE.md`.
- Runtime schema refresh command: `cd prisma && make`.
- Drift check command: `cd prisma && make check-schema` (currently expected to fail until #51/#52/#53 reconcile desired vs device schemas).
