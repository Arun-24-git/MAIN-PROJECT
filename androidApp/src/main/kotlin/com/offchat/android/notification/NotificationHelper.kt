package com.offchat.android.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.offchat.android.MainActivity
import com.offchat.android.R

/**
 * Creates and shows WhatsApp-style notifications for incoming mesh messages.
 */
class NotificationHelper(private val context: Context) {

    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createMessageChannel()
    }

    private fun createMessageChannel() {
        val channel = NotificationChannel(
            MESSAGE_CHANNEL_ID,
            "HOPE NET Messages",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Incoming mesh network messages"
            enableVibration(true)
            enableLights(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
        }
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Show a notification for an incoming message.
     * Uses the peer's endpointId hashCode as the notification ID so
     * each peer gets one notification (updated with latest message).
     */
    fun showMessageNotification(
        senderName: String,
        messageText: String,
        peerId: String
    ) {
        // Don't notify if the user is currently viewing this chat
        if (ActiveChatTracker.activePeerId == peerId) return

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_CHAT_PEER_ID, peerId)
            putExtra(EXTRA_CHAT_PEER_NAME, senderName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            peerId.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(senderName)
            .setContentText(messageText)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(messageText)
                    .setBigContentTitle(senderName)
                    .setSummaryText("HOPE NET")
            )
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(GROUP_KEY_MESSAGES)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .build()

        notificationManager.notify(peerId.hashCode(), notification)

        // Show summary notification for grouping (multiple peers)
        val summaryNotification = NotificationCompat.Builder(context, MESSAGE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("HOPE NET")
            .setContentText("New messages received")
            .setStyle(NotificationCompat.InboxStyle().setSummaryText("HOPE NET"))
            .setGroup(GROUP_KEY_MESSAGES)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(SUMMARY_NOTIFICATION_ID, summaryNotification)
    }

    /** Cancel notification for a specific peer (when user opens that chat). */
    fun cancelForPeer(peerId: String) {
        notificationManager.cancel(peerId.hashCode())
    }

    companion object {
        const val MESSAGE_CHANNEL_ID = "hopenet_messages"
        const val EXTRA_CHAT_PEER_ID = "chat_peer_id"
        const val EXTRA_CHAT_PEER_NAME = "chat_peer_name"
        private const val GROUP_KEY_MESSAGES = "com.offchat.android.MESSAGES"
        private const val SUMMARY_NOTIFICATION_ID = 9999
    }
}
