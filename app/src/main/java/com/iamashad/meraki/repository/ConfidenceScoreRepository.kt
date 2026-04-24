package com.iamashad.meraki.repository

import com.iamashad.meraki.data.ChatDao
import com.iamashad.meraki.data.EmotionDao
import com.iamashad.meraki.data.SessionSummaryDao
import com.iamashad.meraki.di.IoDispatcher
import com.iamashad.meraki.model.ConfidenceScore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single source of truth for the user's [ConfidenceScore].
 *
 * Aggregates signal counts from three local DAOs and delegates the weighted
 * formula to [ConfidenceScore.compute].  All database reads run on [IoDispatcher].
 *
 * Call [compute] whenever the score needs to be refreshed — e.g. after each
 * mood log is saved or when the Home screen is composed.
 *
 * @param emotionDao         Provides mood-log count and average classifier confidence.
 * @param sessionSummaryDao  Provides completed chat-session count.
 * @param chatDao            Provides total user-authored chat messages (conversational depth).
 * @param ioDispatcher       Background dispatcher for Room queries.
 */
@Singleton
class ConfidenceScoreRepository @Inject constructor(
    private val emotionDao: EmotionDao,
    private val sessionSummaryDao: SessionSummaryDao,
    private val chatDao: ChatDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    /**
     * Computes a fresh [ConfidenceScore] for [userId].
     *
     * This is a suspend function — it must be called from a coroutine or another
     * suspend function.  Results are NOT cached here; the ViewModel is responsible
     * for caching via [StateFlow].
     *
     * @param userId Firebase UID of the current user; required to scope the chat
     *               message count correctly to that user's conversation history.
     */
    suspend fun compute(userId: String): ConfidenceScore = withContext(ioDispatcher) {
        val moodLogCount         = emotionDao.getTotalLogCount()
        val avgConf              = emotionDao.getAverageConfidence()
        val sessionCount         = sessionSummaryDao.getTotalSessionCount()
        val chatMessageCount     = chatDao.getUserMessageCount(userId)

        ConfidenceScore.compute(
            moodLogCount         = moodLogCount,
            sessionCount         = sessionCount,
            avgEmotionConfidence = avgConf,
            chatMessageCount     = chatMessageCount
        )
    }
}
