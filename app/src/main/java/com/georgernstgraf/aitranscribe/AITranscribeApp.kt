package com.georgernstgraf.aitranscribe

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.georgernstgraf.aitranscribe.service.ModelSyncWorker

@HiltAndroidApp
class AITranscribeApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        enqueueModelSync()
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
        const val CHANNEL_ID_RECORDING = "recording_channel"
    }
}
