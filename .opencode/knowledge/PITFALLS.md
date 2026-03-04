# Pitfalls and Gotchas

## Hilt + Compose: viewModel() vs hiltViewModel()
Using `viewModel()` instead of `hiltViewModel()` silently bypasses Hilt injection. The ViewModel constructor parameters won't be provided, causing a crash at runtime. Every `@HiltViewModel` must use `hiltViewModel()`.

## Android 13+ BroadcastReceiver requires export flag
Registering a BroadcastReceiver without `Context.RECEIVER_NOT_EXPORTED` (for internal broadcasts) or `RECEIVER_EXPORTED` throws a SecurityException on API 33+. This affects the MainViewModel's receiver for recording completion broadcasts.

## Navigation parameter name must match SavedStateHandle key
If the Compose Navigation route uses `"transcription/{id}"` but the ViewModel reads `savedStateHandle["transcription_id"]`, the parameter is null. Route parameter names and SavedStateHandle keys must be identical.

## TranscriptionWorker: Don't use "get next queued" pattern
Fetching the "next" queued transcription instead of the specific one by ID causes mismatches when multiple items are queued concurrently. Always pass and use the specific transcription ID.

## Emulator audio: -allow-host-audio is required
The Android emulator with default settings or `-no-audio` captures silence from the virtual microphone. Audio files will have valid headers and duration but contain no speech. GROQ returns " ." for silent audio. Must start emulator with:
```bash
emulator -avd Medium_Phone_API_36.1 -allow-host-audio
```

## adb install -r to preserve app data
Using `adb uninstall` followed by `adb install` wipes all app data including the GROQ API key stored in SharedPreferences. Always use `adb install -r` (replace) to preserve data across reinstalls.

## GROQ API returns " ." for empty/silent audio
When the audio file contains no speech (silence), GROQ Whisper doesn't return an error -- it returns `" ."` (space + period, length=2). This is not an API error but indicates the audio content has no recognizable speech.

## .m4a files need audio/mp4 MIME type, not audio/mpeg
RecordingService outputs .m4a (AAC in MP4 container). The correct MIME type for multipart upload is `audio/mp4`, not `audio/mpeg`. However, GROQ seems to accept both; this alone doesn't fix empty transcriptions.

## Room database query timing on first launch
Accessing Room queries before the database is ready can cause crashes on the settings/statistics screen. Ensure queries use Flow or handle the initial empty state gracefully.
