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

## Repository
- GitHub: `georgernstgraf/aitranscribe-android`
- Issue tracker: GitHub Issues (primary issue: #12)

## Key Contacts
- Owner: Georg Ernstgraf

## Development Environment
- Android Emulator: `Medium_Phone_API_36.1` (Pixel-style, API 36)
- Emulator must be started with `-allow-host-audio` for microphone input
- ADB used for install (`adb install -r`) and log inspection (`adb logcat`)
