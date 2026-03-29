# Orchestrator Skill

## Purpose
Coordinates debugging and feature development for the AI Transcribe Android app.

## Workflow
1. **Diagnose** - Read logcat output, identify crash stacktraces or unexpected behavior
2. **Fix** - Edit Kotlin source files, focusing on the specific layer (UI, ViewModel, Service, Worker, Repository, API)
3. **Build** - Run `./gradlew assembleDebug` from the project root
4. **Deploy** - Use `adb install -r app/build/outputs/apk/debug/app-debug.apk` (preserves app data including API keys)
5. **Test** - Interact with emulator, capture logcat, verify fix
6. **Iterate** - If issue persists, add targeted logging and repeat

## Key Commands
```bash
# Build
./gradlew assembleDebug

# Install (preserves data)
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Logcat (filtered)
adb logcat -s "RecordingService" "TranscriptionWorker" "MainViewModel" "GroqApi"

# Start emulator with audio
emulator -avd Medium_Phone_API_36.1 -allow-host-audio

# Pull file from emulator
adb pull /data/data/com.georgernstgraf.aitranscribe/files/<filename> .
```

## Architecture Layers (debug order)
```
UI (Compose Screens) -> ViewModel -> Repository -> Room DB
                                  -> RetrofitAPI (GROQ)
RecordingService (foreground) -> BroadcastReceiver -> MainViewModel -> TranscriptionWorker
```
