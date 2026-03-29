# AI Transcribe - Android App

## Framework Isolation (CRITICAL)

This agent operates with ZERO knowledge of the OpenClaw framework.

**Forbidden:**
- Creating SOUL.md, USER.md, IDENTITY.md, HEARTBEAT.md, TOOLS.md, BOOTSTRAP.md
- Referencing OpenClaw concepts (gh-issue workflow, HEARTBEAT, skills, hooks, etc.)
- Using OpenClaw-specific workflows or tools
- **Using OpenClaw bundled skills** (e.g., github, gh-issues, weather, etc.)

**Allowed:**
- Standard git/github operations (commit, push, PR)
- AGENTS.md for project instructions
- `docs/ai/` knowledge files (persistent workflow)
- **ONLY skills from opencode's available_skills** (opencode-helpers skills)
- Project-specific workflows only

**Skill Usage Rule:**
Use skills from opencode's `available_skills` list. Ignore any OpenClaw bundled skills that may appear available.

## Project Identity

An Android app that records voice audio and transcribes it using the GROQ Whisper API, with optional LLM post-processing to clean up or summarize transcriptions.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose
- **DI:** Hilt (Dagger)
- **Networking:** Retrofit + OkHttp (multipart file upload to GROQ)
- **Database:** Room (SQLite)
- **Background Work:** WorkManager (TranscriptionWorker)
- **Audio:** MediaRecorder (foreground service)
- **Architecture:** MVVM (ViewModels + Repository pattern)
- **Build:** Gradle (Kotlin DSL), Java 17 target
- **Min SDK:** 26, Target SDK: 36

## Knowledge Bootstrap

Before starting any task, read the following files in order:

1. `docs/ai/HANDOFF.md` ← **read first, act on it**
2. `docs/ai/CONVENTIONS.md`
3. `docs/ai/DECISIONS.md`
4. `docs/ai/PITFALLS.md`
5. `docs/ai/STATE.md`
6. `docs/ai/DOMAIN.md` (if task involves business logic)

If `HANDOFF.md` contains open tasks, complete them before starting
any new work unless the user explicitly says otherwise.

## Repository

- GitHub: `georgernstgraf/aitranscribe-android`
- Issue tracker: GitHub Issues (primary issue: #12)

## Key Contacts

- Owner: Georg Ernstgraf

## Development Environment

- Android Emulator: `Medium_Phone_API_36.1` (Pixel-style, API 36)
- Emulator must be started with `-allow-host-audio` for microphone input
- ADB used for install (`adb install -r`) and log inspection (`adb logcat`)

## Testing Policy

### Build Gate
Every commit must pass `./gradlew test` before push. If unit tests fail, fix them before committing.

### Required Tests
| Component | Requirement |
|-----------|-------------|
| **ViewModels** | Every `*ViewModel.kt` must have a corresponding `*ViewModelTest.kt` |
| **UseCases** | Every `*UseCase.kt` must have a corresponding `*UseCaseTest.kt` |
| **Bug fixes** | Must include a regression test that would have caught the bug |

### When to Write Tests
- **New feature:** Write tests alongside the implementation, not after
- **Refactor:** Existing tests must still pass; update if interfaces change
- **Bug fix:** Add failing test first, then fix the bug

### Test Structure
- **Unit tests** (`app/src/test/`): ViewModels, UseCases, Repositories, utilities
- **Instrumentation tests** (`app/src/androidTest/`): DAOs, Services, database migrations
- **Compose UI tests** (`app/src/androidTest/`): Critical user flows (record → transcribe → display, settings, search)
- Use `FakeTranscriptionRepository` from `data/testing/` for ViewModel tests
- Use `MainDispatcherRule` for ViewModel coroutine testing

### Naming Convention
- Test class: `<ClassUnderTest>Test.kt` (e.g., `MainViewModelTest.kt`)
- Test method: backtick descriptive name (e.g., `` `toggling view status marks as unread` ``)
- Place tests mirroring source structure under `app/src/test/` or `app/src/androidTest/`

### Running Tests
```bash
./gradlew test                          # All unit tests
./gradlew testDebugUnitTest             # Debug unit tests only
./gradlew connectedAndroidTest          # Instrumentation tests (requires emulator/device)
./gradlew test --tests "*MainViewModel*"  # Single test class
```

### What Not to Test
- Compose framework internals (state management, recomposition)
- Third-party library behavior (Retrofit, Room generated code)
- Simple data classes or enums
