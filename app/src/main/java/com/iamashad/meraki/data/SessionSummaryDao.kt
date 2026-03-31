package com.iamashad.meraki.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Phase 4: Data Access Object for [SessionSummary].
 *
 * All operations are suspend functions so callers run them from coroutines
 * without blocking the main thread.
 */
@Dao
interface SessionSummaryDao {

    /**
     * Insert or replace a session summary (upsert keyed on [SessionSummary.sessionId]).
     * Called by [com.iamashad.meraki.utils.MemoryManager] after Gemini returns a valid
     * structured summary for the session.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(summary: SessionSummary)

    /**
     * Returns the 14 most-recent summaries in descending date order.
     *
     * This is the two-week sliding window used by [com.iamashad.meraki.utils.MemoryManager]
     * to build the user profile injected into new conversation sessions.
     */
    @Query("SELECT * FROM session_summaries ORDER BY date DESC LIMIT 14")
    suspend fun getLastFourteenSummaries(): List<SessionSummary>

    /**
     * Wipes all saved summaries.
     *
     * Called from [com.iamashad.meraki.screens.chatbot.ChatViewModel.clearChatHistory]
     * to honour the "Clear History" privacy contract — no personal memory logs survive
     * after the user requests a full reset.
     */
    @Query("DELETE FROM session_summaries")
    suspend fun clearAllSummaries()
}
