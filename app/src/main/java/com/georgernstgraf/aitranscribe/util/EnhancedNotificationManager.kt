package com.georgernstgraf.aitranscribe.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
        // Create channel groups
        val recordingGroup = NotificationChannelGroupCompat.Builder(GROUP_ID_RECORDING)
            .setName(context.getString(R.string.notification_channel_recording))
            .setDescription(context.getString(R.string.notification_group_recording_desc))
            .build()

        val transcriptionGroup = NotificationChannelGroupCompat.Builder(GROUP_ID_TRANSCRIPTION)
            .setName(context.getString(R.string.notification_channel_transcription))
            .setDescription(context.getString(R.string.notification_group_transcription_desc))
            .build()

        notificationManager.createNotificationChannelGroupsCompat(
            listOf(recordingGroup, transcriptionGroup)
        )

        // Create notification channels
        val recordingActiveChannel = NotificationChannelCompat.Builder(
            CHANNEL_RECORDING_ACTIVE,
            NotificationManagerCompat.IMPORTANCE_LOW
        )
            .setName(context.getString(R.string.notification_channel_active_recording))
            .setDescription(context.getString(R.string.notification_channel_desc_active_recording))
            .setLightsEnabled(false)
            .setVibrationEnabled(false)
            .setShowBadge(false)
            .setGroup(GROUP_ID_RECORDING)
            .build()

        val transcriptionProgressChannel = NotificationChannelCompat.Builder(
            CHANNEL_TRANSCRIPTION_PROGRESS,
            NotificationManagerCompat.IMPORTANCE_DEFAULT
        )
            .setName(context.getString(R.string.notification_channel_transcription_progress))
            .setDescription(context.getString(R.string.notification_channel_desc_transcription_progress))
            .setLightsEnabled(false)
            .setVibrationEnabled(false)
            .setGroup(GROUP_ID_TRANSCRIPTION)
            .build()

        val transcriptionCompleteChannel = NotificationChannelCompat.Builder(
            CHANNEL_TRANSCRIPTION_COMPLETE,
            NotificationManagerCompat.IMPORTANCE_HIGH
        )
            .setName(context.getString(R.string.notification_channel_transcription_complete))
            .setDescription(context.getString(R.string.notification_channel_desc_transcription_complete))
            .setLightsEnabled(true)
            .setVibrationEnabled(true)
            .setGroup(GROUP_ID_TRANSCRIPTION)
            .build()

        val errorsChannel = NotificationChannelCompat.Builder(
            CHANNEL_ERRORS,
            NotificationManagerCompat.IMPORTANCE_HIGH
        )
            .setName(context.getString(R.string.notification_channel_errors))
            .setDescription(context.getString(R.string.notification_channel_desc_errors))
            .setLightsEnabled(true)
            .setVibrationEnabled(true)
            .setGroup(GROUP_ID_TRANSCRIPTION)
            .build()

        notificationManager.createNotificationChannel(recordingActiveChannel)
        notificationManager.createNotificationChannel(transcriptionProgressChannel)
        notificationManager.createNotificationChannel(transcriptionCompleteChannel)
        notificationManager.createNotificationChannel(errorsChannel)
    }

    /**
     * Show active recording notification.
     */
    fun showRecordingActiveNotification(duration: Int) {
        val notification = NotificationCompat.Builder(context, CHANNEL_RECORDING_ACTIVE)
            .setContentTitle(context.getString(R.string.notification_recording_active))
            .setContentText(context.getString(R.string.notification_recording_duration, duration))
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
            .setContentTitle(context.getString(R.string.notification_transcribing))
            .setContentText(context.getString(R.string.notification_progress_percent, progress))
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
            .setContentTitle(context.getString(R.string.notification_transcription_complete))
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
                    context.getString(R.string.notification_action_copy),
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
            putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.notification_share_subject))
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
