package com.georgernstgraf.aitranscribe

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Build.VERSION_CODES
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import org.acra.ACRA
import org.acra.annotation.AcraCore
import org.acra.annotation.AcraHttpSender
import org.acra.data.StringFormat
import javax.inject.Inject

@HiltAndroidApp
@AcraCore(
    buildConfigClass = BuildConfig::class,
    reportFormat = StringFormat.JSON
)
@AcraHttpSender(
    uri = "https://your-crash-reporting-url.com/reports",
    httpMethod = org.acra.sender.HttpSender.Method.POST
)
class AITranscribeApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()

        initCrashReporting()
        createNotificationChannels()
    }

    private fun initCrashReporting() {
        ACRA.init(this)
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
            
            val transcriptionChannel = NotificationChannel(
                CHANNEL_ID_TRANSCRIPTION,
                getString(R.string.notification_channel_transcription),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for transcription progress"
            }
            
            notificationManager.createNotificationChannel(recordingChannel)
            notificationManager.createNotificationChannel(transcriptionChannel)
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    companion object {
        const val CHANNEL_ID_RECORDING = "recording_channel"
        const val CHANNEL_ID_TRANSCRIPTION = "transcription_channel"
    }
}