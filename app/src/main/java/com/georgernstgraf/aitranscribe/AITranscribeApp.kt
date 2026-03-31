package com.georgernstgraf.aitranscribe

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Build.VERSION_CODES
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.georgernstgraf.aitranscribe.data.local.TranscriptionDatabase
import com.georgernstgraf.aitranscribe.service.ModelSyncWorker
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

@HiltAndroidApp
class AITranscribeApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        enqueueModelSync()
        logAudioDiagnostics()
    }

    private fun logAudioDiagnostics() {
        val recordingsDir = File(filesDir, RECORDINGS_DIR_NAME)
        val recordingFiles = recordingsDir
            .listFiles()
            ?.count { it.name.startsWith("recording_") && it.name.endsWith(".m4a") }
            ?: 0

        appScope.launch {
            val dao = TranscriptionDatabase.getDatabase(this@AITranscribeApp).transcriptionDao()
            val unfinished = dao.getUnfinishedSttTranscriptions()
            val missingAudioRefs = unfinished.count { entity ->
                val path = entity.audioFilePath ?: return@count true
                !File(path).exists()
            }

            Log.i(
                TAG,
                "startup_audio_diagnostics dir=${recordingsDir.absolutePath} files=$recordingFiles unfinished=${unfinished.size} missing_refs=$missingAudioRefs"
            )
        }
    }

    private fun enqueueModelSync() {
        val workRequest = OneTimeWorkRequestBuilder<ModelSyncWorker>().build()
        WorkManager.getInstance(this).enqueueUniqueWork(
            "model_sync_worker",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= VERSION_CODES.O) {
            val notificationManager = getSystemService(NotificationManager::class.java)

            val recordingChannel = NotificationChannel(
                CHANNEL_ID_RECORDING,
                getString(R.string.notification_channel_recording),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground notification for recording"
            }

            notificationManager.createNotificationChannel(recordingChannel)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    companion object {
        private const val TAG = "AITranscribeApp"
        private const val RECORDINGS_DIR_NAME = "recordings"
        const val CHANNEL_ID_RECORDING = "recording_channel"
    }
}
