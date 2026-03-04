# Domain Knowledge

## App Purpose
AI Transcribe is a voice-to-text Android app. Users record speech, which is transcribed via the GROQ Whisper API, and optionally post-processed by an LLM for cleanup/summarization.

## Core Workflow
1. **Setup:** User enters GROQ API key (mandatory before app use)
2. **Record:** User taps record button -> foreground service captures audio as .m4a (AAC/MP4)
3. **Transcribe:** On recording stop, a WorkManager job uploads the audio to GROQ Whisper API
4. **Display:** Transcription text is stored in Room DB and shown in a list
5. **Post-process (planned):** LLM cleans up or summarizes the raw transcription

## Data Model
- **QueuedTranscription:** Pending transcription job (audio file path, status: queued/processing/completed/failed)
- **Transcription:** Completed transcription (original text, processed text, timestamps, duration, viewed flag)
- **Statistics:** Derived from transcriptions (total count, unviewed count, total duration)

## Recording Pipeline
```
Record Button tap
  -> RecordingService starts (foreground, notification)
  -> MediaRecorder captures to .m4a file in app internal storage
  -> On stop: broadcasts ACTION_RECORDING_COMPLETE with file path + duration
  -> MainViewModel receives broadcast
  -> Creates QueuedTranscription in Room DB
  -> Enqueues TranscriptionWorker with transcription ID
  -> Worker reads file, uploads to GROQ, saves result to Transcription table
  -> UI observes Room Flow, updates automatically
```

## GROQ API
- **Endpoint:** Whisper transcription (speech-to-text)
- **Upload:** Multipart form data (file + model + language + response_format)
- **Response:** JSON with `text` field containing transcription
- **Auth:** Bearer token (API key from app preferences)
- **Behavior on silence:** Returns `" ."` (not an error)

## User-Facing Features
- Recording with timer display
- Transcription list with unviewed indicator
- Transcription detail view (original + processed text)
- Statistics screen (total recordings, unviewed count)
- Settings screen (API key management)
- Search functionality across transcriptions
