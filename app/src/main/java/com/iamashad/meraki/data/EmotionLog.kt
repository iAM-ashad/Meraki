package com.iamashad.meraki.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Phase 3: Room entity that persists the output of [EmotionClassifier] for every
 * classified user message.
 *
 * Schema: `emotion_logs`
 *
 * @param id          Auto-generated primary key.
 * @param sessionId   Identifies the conversation session (UUID set per chat session
 *                    in ChatViewModel).
 * @param messageId   Row ID of the corresponding [ChatMessage] in `chat_messages`.
 *                    Stored as a plain Long so the table can be queried independently
 *                    without enforcing a FK constraint (avoids cascade deletion of
 *                    emotion history when messages are cleared).
 * @param emotion     Lowercase [EmotionCategory.key] string, e.g. "anxious".
 * @param intensity   Lowercase [EmotionIntensity.displayName], e.g. "high".
 * @param confidence  Model confidence in [0, 1].
 * @param timestamp   Unix epoch milliseconds at classification time.
 */
@Entity(
    tableName = "emotion_logs",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["messageId"])
    ]
)
data class EmotionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sessionId: String,
    val messageId: Long,
    val emotion: String,
    val intensity: String,
    val confidence: Float,
    val timestamp: Long = System.currentTimeMillis()
)
