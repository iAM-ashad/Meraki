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

class NotificationWorker(
    context: Context,
    workerParams: WorkerParameters
) : Worker(context, workerParams) {

    override fun doWork(): Result {
        Log.d("NotificationWorker", "NotificationWorker executed")
        sendNotification()
        return Result.success()
    }

    private fun sendNotification() {
        val channelId = "mood_journal_reminders"

        try {
            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId, "Mood Journal Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                )
                notificationManager.createNotificationChannel(channel)
                Log.d("NotificationWorker", "Notification channel created: $channelId")
            }

            val intent = Intent(applicationContext, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                applicationContext, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )

            Log.d("NotificationWorker", "Building notification")
            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setContentTitle("Your Mood Matters: Let’s Log Today’s Feelings")
                .setContentText("You’re building a habit of self-care. Don’t miss today’s entry!")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Ensure this drawable exists
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

            Log.d("NotificationWorker", "Displaying notification")
            notificationManager.notify(1, notification)
        } catch (e: Exception) {
            Log.e("NotificationWorker", "Error while sending notification: ${e.message}", e)
        }
    }

}
