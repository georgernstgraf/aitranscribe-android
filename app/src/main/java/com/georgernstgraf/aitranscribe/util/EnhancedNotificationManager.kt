package com.georgernstgraf.aitranscribe.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.georgernstgraf.aitranscribe.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced notification manager.
 * Provides categorized notifications with better UX.
 */
@Singleton
class EnhancedNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = NotificationManagerCompat.from(context)

    init {
        createNotificationChannels()
    }

    /**
     * Create all notification channels and groups.
     */
    private fun createNotificationChannels() {
        val channels = listOf(
            NotificationChannelGroupCompat(
                GROUP_ID_RECORDING,
                "Recording"
            ).apply {
                description = "Recording related notifications"
                isBlocked = false
            },
            NotificationChannelGroupCompat(
                GROUP_ID_TRANSCRIPTION,
                "Transcription"
            ).apply {
                description = "Transcription progress and completion notifications"
                isBlocked = false
            }
        )

        notificationManager.createNotificationChannelGroupCompat(channels)
        notificationManager.createNotificationChannelCompat(
            CHANNEL_RECORDING_ACTIVE,
            NotificationCompat.Channel(
                CHANNEL_RECORDING_ACTIVE,
                "Active Recording",
                NotificationManagerCompat.IMPORTANCE_LOW
            ).apply {
                description = "Shows when recording is active"
                enableLights(false)
                enableVibration(false)
                setOngoing(true)
                setShowBadge(false)
                group = GROUP_ID_RECORDING
            }
        )

        notificationManager.createNotificationChannelCompat(
            CHANNEL_TRANSCRIPTION_PROGRESS,
            NotificationCompat.Channel(
                CHANNEL_TRANSCRIPTION_PROGRESS,
                "Transcription Progress",
                NotificationManagerCompat.IMPORTANCE_DEFAULT
            ).apply {
                description = "Shows transcription progress"
                enableLights(false)
                enableVibration(false)
                group = GROUP_ID_TRANSCRIPTION
            }
        )

        notificationManager.createNotificationChannelCompat(
            CHANNEL_TRANSCRIPTION_COMPLETE,
            NotificationCompat.Channel(
                CHANNEL_TRANSCRIPTION_COMPLETE,
                "Transcription Complete",
                NotificationManagerCompat.IMPORTANCE_HIGH
            ).apply {
                description = "Shows when transcription is complete"
                enableLights(true)
                enableVibration(true)
                group = GROUP_ID_TRANSCRIPTION
            }
        )

        notificationManager.createNotificationChannelCompat(
            CHANNEL_ERRORS,
            NotificationCompat.Channel(
                CHANNEL_ERRORS,
                "Errors",
                NotificationManagerCompat.IMPORTANCE_HIGH
            ).apply {
                description = "Shows error notifications"
                enableLights(true)
                enableVibration(true)
                group = GROUP_ID_TRANSCRIPTION
            }
        )
    }

    /**
     * Show active recording notification.
     */
    fun showRecordingActiveNotification(duration: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_RECORDING_ACTIVE)
            .setContentTitle("Recording Active")
            .setContentText("Duration: ${duration}s")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setGroup(GROUP_ID_RECORDING)
            .build()

        notificationManager.notify(NOTIFICATION_ID_RECORDING, notification)
    }

    /**
     * Show transcription progress notification with progress bar.
     */
    fun showTranscriptionProgressNotification(
        transcriptionId: Long,
        progress: Int,
        max: Int = 100
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_TRANSCRIPTION_PROGRESS)
            .setContentTitle("Transcribing...")
            .setContentText("$progress% complete")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(max, progress, false)
            .setGroup(GROUP_ID_TRANSCRIPTION)
            .build()

        notificationManager.notify(NOTIFICATION_ID_TRANSCRIPTION, notification)
    }

    /**
     * Show transcription complete notification with actions.
     */
    fun showTranscriptionCompleteNotification(
        transcriptionId: Long,
        text: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_TRANSCRIPTION_COMPLETE)
            .setContentTitle("Transcription Complete")
            .setContentText(text.take(50))
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(GROUP_ID_TRANSCRIPTION)
            .setAutoCancel(true)
            .addAction(
                NotificationCompat.Action(
                    R.mipmap.ic_launcher,
                    "Copy",
                    PendingIntent.getActivity(
                        context,
                        0,
                        createCopyIntent(text),
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                )
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID_TRANSCRIPTION, notification)
    }

    /**
     * Show error notification with detailed information.
     */
    fun showErrorNotification(
        title: String,
        message: String,
        error: Throwable
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ERRORS)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setGroup(GROUP_ID_TRANSCRIPTION)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_ERROR, notification)
    }

    /**
     * Cancel all notifications.
     */
    fun cancelAll() {
        notificationManager.cancelAll()
    }

    /**
     * Cancel specific notification by ID.
     */
    fun cancel(id: Int) {
        notificationManager.cancel(id)
    }

    private fun createCopyIntent(text: String): Intent {
        return Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "Transcription from AITranscribe")
        }
    }

    companion object {
        const val GROUP_ID_RECORDING = "recording_group"
        const val GROUP_ID_TRANSCRIPTION = "transcription_group"

        const val CHANNEL_RECORDING_ACTIVE = "recording_active"
        const val CHANNEL_TRANSCRIPTION_PROGRESS = "transcription_progress"
        const val CHANNEL_TRANSCRIPTION_COMPLETE = "transcription_complete"
        const val CHANNEL_ERRORS = "errors"

        const val NOTIFICATION_ID_RECORDING = 3001
        const val NOTIFICATION_ID_TRANSCRIPTION = 3002
        const val NOTIFICATION_ID_ERROR = 3003
    }
}