# Prisma DB Governance

## Source of Truth

- `prisma/desired/schema.prisma` is the canonical database contract.
- `prisma/device/schema.prisma` is the observed runtime schema captured from a fresh app install on a real device.

## Required Workflow for DB Changes

1. Update `prisma/desired/schema.prisma` first.
2. Implement matching Room entity and migration changes in app code.
3. Refresh runtime schema snapshot with:
   - `cd prisma && make`
4. Compare canonical and observed schemas with:
   - `cd prisma && make check-schema`
5. If differences remain, either:
   - align app/migration code to desired schema, or
   - update desired schema intentionally with rationale in the PR.

## Allowed Exceptions

The following tables are Room/SQLite internals and are not domain schema:

- `android_metadata`
- `room_master_table`

Any additional exceptions must be explicitly documented in the PR.

## PR Expectations for DB-Affecting Changes

- Include both schema files when relevant:
  - `prisma/desired/schema.prisma`
  - `prisma/device/schema.prisma`
- Include migration notes and data-compatibility implications.
- Confirm local governance checks were run:
  - `cd prisma && make`
  - `cd prisma && make check-schema`
