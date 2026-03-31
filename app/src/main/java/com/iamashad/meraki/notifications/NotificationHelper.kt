package com.iamashad.meraki.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.iamashad.meraki.MainActivity
import com.iamashad.meraki.R

/**
 * Phase 5: Central helper for the companion check-in notification channel.
 *
 * Architecture:
 *  - Single dedicated channel [CHANNEL_ID] = "companion_checkin" with [IMPORTANCE_DEFAULT]
 *    so notifications arrive silently (no intrusive sound/vibration).
 *  - Every notification built here deep-links to the Chatbot screen via
 *    [buildChatbotPendingIntent], passing [EXTRA_NAVIGATE_TO] = [VALUE_CHATBOT] to
 *    MainActivity.  The Activity reads this extra and navigates accordingly.
 *  - Notification IDs are kept in distinct slots so check-in, weekly insight,
 *    and smart nudge notifications don't overwrite each other.
 */
object NotificationHelper {

    // ── Channel ──────────────────────────────────────────────────────────────

    /** Channel ID for all companion check-in notifications (check-in, insights, nudges). */
    const val CHANNEL_ID   = "companion_checkin"
    const val CHANNEL_NAME = "Companion Check-ins"

    // ── Notification IDs (non-overlapping) ───────────────────────────────────

    /** Notification slot for the daily check-in reminder. */
    const val NOTIF_ID_CHECKIN = 200

    /** Notification slot for the weekly emotional insight summary. */
    const val NOTIF_ID_WEEKLY  = 201

    /** Notification slot for contextual smart nudges. */
    const val NOTIF_ID_NUDGE   = 202

    // ── Deep-link contract ────────────────────────────────────────────────────

    /**
     * Intent extra key used to signal a destination to MainActivity.
     * Mirrors [com.iamashad.meraki.MainActivity.EXTRA_NAVIGATE_TO].
     */
    const val EXTRA_NAVIGATE_TO = "navigate_to"

    /** Value indicating the Chatbot screen should be opened. */
    const val VALUE_CHATBOT = "chatbot"

    // ── Channel setup ─────────────────────────────────────────────────────────

    /**
     * Creates (or no-ops if already exists) the [CHANNEL_ID] notification channel.
     *
     * Must be called before posting any notification.  Safe to call multiple times —
     * NotificationManager ignores duplicate [createNotificationChannel] calls.
     *
     * IMPORTANCE_DEFAULT: shows in the shade with an alert but no sound/vibration,
     * meeting the UX requirement of non-intrusive companion notifications.
     */
    fun ensureChannelExists(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return   // already exists

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily check-ins, weekly insights, and personalised nudges from Meraki."
            enableVibration(false)
            setSound(null, null)   // Silent — no intrusive notification sound
        }
        nm.createNotificationChannel(channel)
    }

    // ── PendingIntent factory ─────────────────────────────────────────────────

    /**
     * Returns a [PendingIntent] that opens [MainActivity] with the [EXTRA_NAVIGATE_TO]
     * extra set to [VALUE_CHATBOT].
     *
     * Flags:
     *  - FLAG_ACTIVITY_NEW_TASK     — required when starting an Activity from a Worker.
     *  - FLAG_ACTIVITY_CLEAR_TOP    — if MainActivity is already running, bring it to the
     *                                 front and deliver the new Intent via onNewIntent().
     *  - FLAG_IMMUTABLE             — required for API 31+.
     *  - FLAG_UPDATE_CURRENT        — replace any cached PendingIntent with the latest extras.
     */
    fun buildChatbotPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra(EXTRA_NAVIGATE_TO, VALUE_CHATBOT)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            /* requestCode = */ 0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    // ── Notification builder / dispatcher ─────────────────────────────────────

    /**
     * Builds and posts a notification on the [CHANNEL_ID] channel.
     *
     * @param context    Application context.
     * @param notifId    One of [NOTIF_ID_CHECKIN], [NOTIF_ID_WEEKLY], or [NOTIF_ID_NUDGE].
     * @param title      Short notification title shown in bold.
     * @param body       Notification body text.
     */
    fun send(context: Context, notifId: Int, title: String, body: String) {
        ensureChannelExists(context)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.ic_app)
            .setContentIntent(buildChatbotPendingIntent(context))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(notifId, notification)
    }
}
