# AI Transcribe - Android App

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

1. `.opencode/knowledge/HANDOFF.md` ← **read first, act on it**
2. `.opencode/knowledge/CONVENTIONS.md`
3. `.opencode/knowledge/DECISIONS.md`
4. `.opencode/knowledge/PITFALLS.md`
5. `.opencode/knowledge/STATE.md`
6. `.opencode/knowledge/DOMAIN.md` (if task involves business logic)

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
