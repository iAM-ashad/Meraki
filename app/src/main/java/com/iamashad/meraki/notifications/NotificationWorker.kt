package com.iamashad.meraki.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.iamashad.meraki.MainActivity
import com.iamashad.meraki.R

/**
 * A background worker that sends a daily reminder notification to the user
 * encouraging them to log their mood.
 */
class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    /**
     * Called when the Worker is executed.
     * This will trigger a local notification.
     */
    override fun doWork(): Result {
        Log.d("NotificationWorker", "NotificationWorker executed")
        sendNotification()
        return Result.success()
    }

    /**
     * Builds and displays a high-priority reminder notification with an intent
     * that opens the MainActivity when tapped.
     */
    private fun sendNotification() {
        val channelId = "mood_journal_reminders"

        try {
            // Create NotificationManager and channel
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val channel = NotificationChannel(
                channelId,
                "Mood Journal Reminders",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
            Log.d("NotificationWorker", "Notification channel created: $channelId")

            // Intent to launch MainActivity when the user taps the notification
            val intent = Intent(applicationContext, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )

            Log.d("NotificationWorker", "Building notification")

            // Build the notification
            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setContentTitle("Your Mood Matters: Let’s Log Today’s Feelings")
                .setContentText("You’re building a habit of self-care. Don’t miss today’s entry!")
                .setSmallIcon(R.drawable.ic_app)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            Log.d("NotificationWorker", "Displaying notification")

            // Show the notification
            notificationManager.notify(1, notification)

        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error while sending notification: ${e.message}", e)
        }
    }
}
