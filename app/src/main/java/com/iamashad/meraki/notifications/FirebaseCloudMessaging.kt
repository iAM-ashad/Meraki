package com.iamashad.meraki.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.iamashad.meraki.MainActivity
import com.iamashad.meraki.R

/**
 * Custom Firebase Messaging Service to handle push notifications.
 * This class receives FCM messages and displays local notifications.
 */
class MyFirebaseMessagingService : FirebaseMessagingService() {

    /**
     * Called when a new FCM message is received.
     * Extracts notification data and displays it.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        remoteMessage.notification?.let {
            sendNotification(it.title, it.body)
        }
    }

    /**
     * Called when a new FCM registration token is generated.
     * You can use this method to send the token to your server if needed.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token generated: $token")
        sendTokenToServer(token)
    }

    /**
     * Displays a local notification with the provided title and body.
     */
    private fun sendNotification(title: String?, body: String?) {
        val channelId = "mood_journal_notifications"

        // Create NotificationManager and notification channel
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channel = NotificationChannel(
            channelId,
            "Mood Journal Notifications",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        // Launch MainActivity when the notification is tapped
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_app)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        // Display the notification
        notificationManager.notify(0, notification)
    }

    /**
     * Stub method to simulate sending the FCM token to your server.
     * You can implement actual network logic here as needed.
     */
    private fun sendTokenToServer(token: String) {
        Log.d("FCM", "Token sent to server: $token")
    }
}
