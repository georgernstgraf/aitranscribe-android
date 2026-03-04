# Architectural Decisions

## 2026-03-04: hiltViewModel() over viewModel() for all Compose screens
- **Reason:** Plain `viewModel()` bypasses Hilt dependency injection, causing runtime crashes when ViewModels have `@Inject` constructor parameters (repository, database, etc.)
- **Affected:** MainScreen, TranscriptionDetailScreen, SetupScreen
- **Rule:** Every `@HiltViewModel` must be obtained via `hiltViewModel()` in Compose, never `viewModel()`

## 2026-03-04: TranscriptionWorker fetches queued item by ID, not "next in queue"
- **Reason:** `getNextQueued()` returned arbitrary queued items, causing ID mismatches when multiple recordings were queued. The worker receives a specific `transcription_id` as input data, so it must fetch that exact record.
- **Changed:** `QueuedTranscriptionDao` now has `getById(id)` query; `TranscriptionRepository` exposes `getQueuedById(id)`
- **Tradeoff:** Slightly less flexible than a generic queue, but eliminates race conditions

## 2026-03-04: Navigation route parameter "transcription_id" (not "id")
- **Reason:** `TranscriptionDetailViewModel` uses `SavedStateHandle` with key `KEY_TRANSCRIPTION_ID = "transcription_id"`. The navigation route must use the same parameter name or the ViewModel gets null.
- **Route:** `"transcription/{transcription_id}"`

## 2026-03-04: RECEIVER_NOT_EXPORTED for internal BroadcastReceivers
- **Reason:** Android 13+ (API 33) requires explicit export flag when registering receivers. Internal broadcasts (recording completion) must use `RECEIVER_NOT_EXPORTED` to avoid SecurityException.
- **Affected:** MainViewModel's BroadcastReceiver for `RecordingService.ACTION_RECORDING_COMPLETE`

## 2026-03-04: audio/mp4 MIME type for .m4a uploads to GROQ
- **Reason:** RecordingService outputs `.m4a` files (AAC in MP4 container). Initially sent as `audio/mpeg` which is incorrect for M4A. Changed to `audio/mp4` for accurate content-type in multipart upload.
- **Status:** GROQ accepted it but still returned empty transcription (" .") -- the MIME type was not the root cause; audio content itself needs investigation.

## 2026-03-04: Emulator must use -allow-host-audio flag
- **Reason:** Default emulator launch with `-no-audio` or no audio flag means the virtual microphone captures silence. The `-allow-host-audio` flag enables host microphone passthrough so recordings contain actual audio data.
- **Impact:** Without this, all recordings are silent files with valid headers but no speech data, causing GROQ to return empty transcriptions.
