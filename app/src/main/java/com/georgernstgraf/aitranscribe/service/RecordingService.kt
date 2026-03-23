package com.georgernstgraf.aitranscribe.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.georgernstgraf.aitranscribe.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import javax.inject.Inject

/**
 * Foreground service for audio recording.
 * Handles push-to-talk recording with foreground notification.
 */
@AndroidEntryPoint
class RecordingService : Service() {

    @Inject
    lateinit var notificationManager: NotificationManager

    private var mediaRecorder: MediaRecorder? = null
    private var recordingJob: Job? = null
    private var currentAudioFile: File? = null
    private var recordingDuration = 0
    private var isRecording = false

    companion object {
        private const val TAG = "RecordingService"
        const val NOTIFICATION_ID_RECORDING = 1001
        const val CHANNEL_ID_RECORDING = "recording_channel"

        const val ACTION_START_RECORDING = "com.georgernstgraf.aitranscribe.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.georgernstgraf.aitranscribe.STOP_RECORDING"
        const val ACTION_CANCEL_RECORDING = "com.georgernstgraf.aitranscribe.CANCEL_RECORDING"

        const val ACTION_RECORDING_RESULT = "com.georgernstgraf.aitranscribe.RECORDING_RESULT"
        const val EXTRA_AUDIO_PATH = "audio_path"
        const val EXTRA_DURATION = "duration"
        const val EXTRA_WAS_CANCELLED = "was_cancelled"
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action}")
        when (intent?.action) {
            ACTION_START_RECORDING -> startRecording()
            ACTION_STOP_RECORDING -> stopRecording()
            ACTION_CANCEL_RECORDING -> cancelRecording()
        }
        return START_NOT_STICKY
    }

    private fun startRecording() {
        if (isRecording) {
            Log.w(TAG, "startRecording: already recording")
            return
        }

        Log.d(TAG, "startRecording: starting...")
        isRecording = true
        recordingDuration = 0

        createNotificationChannel()
        startForeground(NOTIFICATION_ID_RECORDING, createRecordingNotification())

        initializeMediaRecorder()
        startRecordingJob()
    }

    private fun stopRecording() {
        if (!isRecording) {
            Log.w(TAG, "stopRecording: not recording")
            return
        }

        Log.d(TAG, "stopRecording: stopping...")
        isRecording = false
        recordingJob?.cancel()
        
        try {
            mediaRecorder?.stop()
            mediaRecorder?.reset()
        } catch (e: Exception) {
            Log.e(TAG, "stopRecording: error stopping mediaRecorder", e)
        }

        mediaRecorder?.release()
        mediaRecorder = null

        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(NOTIFICATION_ID_RECORDING)

        val audioPath = currentAudioFile?.absolutePath
        Log.d(TAG, "stopRecording: audioPath=$audioPath, duration=$recordingDuration")
        broadcastRecordingResult(audioPath, recordingDuration, wasCancelled = false)
    }

    private fun cancelRecording() {
        if (!isRecording) return

        isRecording = false
        recordingJob?.cancel()

        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            // Ignore stop exception
        }

        mediaRecorder?.release()
        mediaRecorder = null

        currentAudioFile?.delete()

        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(NOTIFICATION_ID_RECORDING)

        broadcastRecordingResult(null, recordingDuration, wasCancelled = true)
    }

    private fun initializeMediaRecorder() {
        try {
            mediaRecorder = MediaRecorder(this@RecordingService).apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setAudioChannels(1)

                val audioFile = createTempAudioFile()
                currentAudioFile = audioFile
                setOutputFile(audioFile.absolutePath)

                prepare()
                start()
            }
        } catch (e: IOException) {
            throw RecordingException("Failed to initialize MediaRecorder", e)
        }
    }

    private fun startRecordingJob() {
        recordingJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && isRecording) {
                delay(1000)
                recordingDuration++
                updateNotification()
            }
        }
    }

    private fun createTempAudioFile(): File {
        val cacheDir = cacheDir
        val timestamp = System.currentTimeMillis()
        return File(cacheDir, "recording_$timestamp.m4a")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID_RECORDING,
                getString(R.string.notification_channel_recording),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground notification for recording"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createRecordingNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this,
            0,
            Intent(this, RecordingService::class.java).apply {
                action = ACTION_STOP_RECORDING
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return Notification.Builder(this, CHANNEL_ID_RECORDING)
            .setContentTitle(getString(R.string.notification_recording_title))
            .setContentText(getString(R.string.notification_recording_text))
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .addAction(
                Notification.Action.Builder(
                    android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_media_pause),
                    "Stop",
                    stopIntent
                ).build()
            )
            .build()
    }

    private fun updateNotification() {
        val notification = Notification.Builder(this, CHANNEL_ID_RECORDING)
            .setContentTitle(getString(R.string.notification_recording_title))
            .setContentText("Recording duration: $recordingDuration seconds")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_RECORDING, notification)
    }

    private fun broadcastRecordingResult(
        audioPath: String?,
        duration: Int,
        wasCancelled: Boolean
    ) {
        Log.e(TAG, "broadcastRecordingResult: audioPath=$audioPath, duration=$duration, wasCancelled=$wasCancelled")
        val intent = Intent(ACTION_RECORDING_RESULT).apply {
            putExtra(EXTRA_AUDIO_PATH, audioPath)
            putExtra(EXTRA_DURATION, duration)
            putExtra(EXTRA_WAS_CANCELLED, wasCancelled)
            setPackage(packageName)
        }
        Log.e(TAG, "broadcastRecordingResult: packageName=$packageName")
        sendBroadcast(intent)
        Log.e(TAG, "broadcastRecordingResult: broadcast sent with action=$ACTION_RECORDING_RESULT")
    }

    fun isRecording(): Boolean = isRecording

    fun getCurrentAudioFile(): File? = currentAudioFile

    override fun onDestroy() {
        super.onDestroy()
        if (isRecording) {
            cancelRecording()
        }
    }

    class RecordingException(message: String, cause: Throwable?) : Exception(message, cause)
}
