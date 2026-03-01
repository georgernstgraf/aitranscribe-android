# Multi-Agent Orchestration Plan

## Overview
This repository has 4 subissues in separate git worktrees for parallel development by multiple agents.

## Issues & Worktrees

| Issue | Worktree | Title | Priority | Dependencies |
|--------|-----------|--------|--------------|
| #34 | ../aitranscribe-android-issue34 | EnhancedNotificationManager API usage | HIGH | #37 |
| #35 | ../aitranscribe-android-issue35 | FilePicker ActivityResultLauncher | MEDIUM | None |
| #36 | ../aitranscribe-android-issue36 | TranscriptionWorker Hilt integration | HIGH | #34, #37 |
| #37 | ../aitranscribe-android-issue37 | String resources for notifications | MEDIUM | None |

## Dependency Graph

```
#37 (String Resources)
    ↓
#34 (EnhancedNotificationManager)
    ↓
#36 (TranscriptionWorker)

#35 (FilePicker) [Independent]
```

## Recommended Work Order

1. **Issue #37** - String resources (foundational, no dependencies)
2. **Issue #34** - EnhancedNotificationManager (depends on #37)
3. **Issue #36** - TranscriptionWorker (depends on #34, #37)
4. **Issue #35** - FilePicker (independent, can be done in parallel)

## Worktree Navigation

```bash
# To work on specific issues:
cd ../aitranscribe-android-issue34  # Issue #34
cd ../aitranscribe-android-issue35  # Issue #35
cd ../aitranscribe-android-issue36  # Issue #36
cd ../aitranscribe-android-issue37  # Issue #37

# To return to main worktree:
cd /home/georg/gitm/aitranscribe-android/main
```

## Status Tracking

- [ ] #37 - String resources created and added
- [ ] #34 - EnhancedNotificationManager fixed and tests passing
- [ ] #36 - TranscriptionWorker Hilt integration resolved
- [ ] #35 - FilePicker implemented with ActivityResultLauncher

## Notes for Agents

- Each issue has detailed description in GitHub
- Work in assigned worktree directory
- Push changes when complete
- Reference issue number in commit messages
- Close issue on GitHub when complete

## Merging Strategy

When all issues complete:

```bash
# Merge back to main
git checkout main
git merge issue-34-enhanced-notification
git merge issue-35-filepicker
git merge issue-36-worker
git merge issue-37-strings
git push origin main
```

Then close all issues on GitHub.
