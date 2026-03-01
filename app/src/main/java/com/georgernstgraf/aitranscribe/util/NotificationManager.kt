package com.georgernstgraf.aitranscribe.util

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.georgernstgraf.aitranscribe.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages notifications for transcription service.
 * Shows progress, completion, and error notifications.
 */
@Singleton
class NotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val notificationManager = 
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    /**
     * Shows notification when offline processing is queued.
     */
    fun showOfflineNotification() {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TRANSCRIPTION)
            .setContentTitle(context.getString(R.string.offline_queued))
            .setContentText(context.getString(R.string.offline_no_connection))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        notificationManager.notify(NOTIFICATION_ID_OFFLINE, notification)
    }

    /**
     * Shows transcription progress notification.
     */
    fun showTranscriptionProgressNotification(queuedId: Long) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TRANSCRIPTION)
            .setContentTitle(context.getString(R.string.notification_transcribing_title))
            .setContentText(context.getString(R.string.notification_transcribing_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setProgress(100, 50, true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_TRANSCRIPTION, notification)
    }

    /**
     * Shows post-processing progress notification.
     */
    fun showPostProcessingNotification(transcriptionId: Long) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TRANSCRIPTION)
            .setContentTitle("Post-processing transcription...")
            .setContentText("Applying LLM processing...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setProgress(100, 75, true)
            .build()

        notificationManager.notify(NOTIFICATION_ID_TRANSCRIPTION, notification)
    }

    /**
     * Shows transcription complete notification.
     */
    fun showTranscriptionCompleteNotification(transcriptionId: Long) {
        val intent = Intent(context, com.georgernstgraf.aitranscribe.ui.screen.MainActivity::class.java).apply {
            putExtra(EXTRA_TRANSCRIPTION_ID, transcriptionId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TRANSCRIPTION)
            .setContentTitle(context.getString(R.string.notification_transcribing_title))
            .setContentText(context.getString(R.string.notification_transcribing_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        notificationManager.notify(NOTIFICATION_ID_TRANSCRIPTION, notification)
    }

    /**
     * Shows transcription error notification.
     */
    fun showTranscriptionErrorNotification(error: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_TRANSCRIPTION)
            .setContentTitle(context.getString(R.string.transcription_failed))
            .setContentText(error)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(NOTIFICATION_ID_ERROR, notification)
    }

    /**
     * Cancels all transcription notifications.
     */
    fun cancelTranscriptionNotifications() {
        notificationManager.cancel(NOTIFICATION_ID_TRANSCRIPTION)
        notificationManager.cancel(NOTIFICATION_ID_ERROR)
        notificationManager.cancel(NOTIFICATION_ID_OFFLINE)
    }

    companion object {
        const val CHANNEL_ID_TRANSCRIPTION = "transcription_channel"
        const val NOTIFICATION_ID_TRANSCRIPTION = 2001
        const val NOTIFICATION_ID_ERROR = 2002
        const val NOTIFICATION_ID_OFFLINE = 2003
        const val EXTRA_TRANSCRIPTION_ID = "transcription_id"
    }
}