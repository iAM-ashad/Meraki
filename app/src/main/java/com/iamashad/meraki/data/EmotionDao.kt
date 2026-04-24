package com.iamashad.meraki.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Phase 3: Data Access Object for [EmotionLog].
 *
 * All write operations are suspend functions; read operations expose
 * [Flow] so the UI layer can react to new classifications in real time.
 */
@Dao
interface EmotionDao {

    /**
     * Persists a single emotion classification result.
     * Returns the auto-generated row ID so callers can cross-reference the log
     * entry if needed.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmotionLog(log: EmotionLog): Long

    /**
     * Returns all [EmotionLog] entries for a given session, ordered
     * chronologically (oldest first).
     */
    @Query("SELECT * FROM emotion_logs WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getLogsForSession(sessionId: String): Flow<List<EmotionLog>>

    /**
     * Returns the most recent [EmotionLog] linked to a specific message row ID.
     * Useful for rehydrating the emotion state when loading conversation history.
     */
    @Query("SELECT * FROM emotion_logs WHERE messageId = :messageId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLogForMessage(messageId: Long): EmotionLog?

    /**
     * Returns all [EmotionLog] entries across all sessions for a given user
     * (resolved via the message table join) — useful for aggregate mood reports.
     *
     * NOTE: This query joins on `chat_messages.id`, so it requires both tables
     * to exist in the same [ChatDatabase].
     */
    @Query(
        """
        SELECT el.* FROM emotion_logs el
        INNER JOIN chat_messages cm ON el.messageId = cm.id
        WHERE cm.userId = :userId
        ORDER BY el.timestamp ASC
        """
    )
    fun getLogsForUser(userId: String): Flow<List<EmotionLog>>

    /** Deletes all emotion log entries associated with a given session. */
    @Query("DELETE FROM emotion_logs WHERE sessionId = :sessionId")
    suspend fun clearLogsForSession(sessionId: String)

    // ── Confidence-score aggregates ───────────────────────────────────────────

    /**
     * Returns the total number of emotion-log rows across all sessions.
     *
     * Used by [com.iamashad.meraki.repository.ConfidenceScoreRepository] as the
     * "mood log count" component of the user confidence score.
     */
    @Query("SELECT COUNT(*) FROM emotion_logs")
    suspend fun getTotalLogCount(): Int

    /**
     * Returns the mean classifier confidence across all emotion-log rows,
     * or 0.0 when the table is empty.
     *
     * The COALESCE guard prevents SQLite from returning NULL on an empty table
     * (which would otherwise crash Kotlin's non-nullable Float mapping).
     *
     * Used by [com.iamashad.meraki.repository.ConfidenceScoreRepository] as the
     * "average emotion confidence" component of the user confidence score.
     */
    @Query("SELECT COALESCE(AVG(confidence), 0.0) FROM emotion_logs")
    suspend fun getAverageConfidence(): Float
}
