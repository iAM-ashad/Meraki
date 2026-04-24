package com.iamashad.meraki.model

/**
 * Represents how well the intelligence system "knows" this user — i.e. how much
 * signal it has accumulated to generate trustworthy insights.
 *
 * The score is a weighted composite in [0, 1] built from four sources:
 *
 *   ┌──────────────────────────────────┬────────┬─────────────────────────┐
 *   │ Source                           │ Weight │ Saturates at            │
 *   ├──────────────────────────────────┼────────┼─────────────────────────┤
 *   │ Mood log count                   │  35 %  │ 20 logs                 │
 *   │ Chat session count               │  35 %  │ 10 sessions             │
 *   │ Average emotion-classifier conf. │  20 %  │ 1.0 (inherent)          │
 *   │ Chat message count               │  10 %  │ 30 messages             │
 *   └──────────────────────────────────┴────────┴─────────────────────────┘
 *
 * Each component is clamped to [0, 1] before weighting, so early engagement
 * drives the score up quickly while diminishing returns set in beyond the
 * saturation point.
 */
data class ConfidenceScore(
    /** Composite score in [0.0, 1.0]. */
    val value: Float,
    /** The tier derived from [value] — drives insight gating in the UI and workers. */
    val tier: InsightTier,
    /** Raw inputs — exposed so the UI can show "N more check-ins needed" hints. */
    val moodLogCount: Int,
    val sessionCount: Int,
    val avgEmotionConfidence: Float,
    val chatMessageCount: Int
) {
    companion object {

        // ── Tier thresholds ───────────────────────────────────────────────────
        const val THRESHOLD_LOW      = 0.20f
        const val THRESHOLD_MODERATE = 0.50f
        const val THRESHOLD_HIGH     = 0.75f

        // ── Component weights (must sum to 1.0) ───────────────────────────────
        private const val W_MOOD_LOGS       = 0.35f
        private const val W_SESSIONS        = 0.35f
        private const val W_AVG_CONFIDENCE  = 0.20f
        private const val W_CHAT_MESSAGES   = 0.10f

        // ── Saturation ceilings ───────────────────────────────────────────────
        private const val SAT_MOOD_LOGS     = 20
        private const val SAT_SESSIONS      = 10
        private const val SAT_CHAT_MESSAGES = 30

        /** Zero-data starting state — used before the first async compute completes. */
        val EMPTY = ConfidenceScore(
            value                = 0f,
            tier                 = InsightTier.FORMING,
            moodLogCount         = 0,
            sessionCount         = 0,
            avgEmotionConfidence = 0f,
            chatMessageCount     = 0
        )

        /**
         * Computes a [ConfidenceScore] from raw signal counts.
         *
         * [avgEmotionConfidence] is the mean of the on-device TFLite classifier's
         * per-message confidence values stored in [EmotionLog.confidence].
         */
        fun compute(
            moodLogCount: Int,
            sessionCount: Int,
            avgEmotionConfidence: Float,
            chatMessageCount: Int
        ): ConfidenceScore {
            val v =
                (moodLogCount.toFloat()    / SAT_MOOD_LOGS    ).coerceIn(0f, 1f) * W_MOOD_LOGS      +
                (sessionCount.toFloat()     / SAT_SESSIONS      ).coerceIn(0f, 1f) * W_SESSIONS       +
                avgEmotionConfidence.coerceIn(0f, 1f)                              * W_AVG_CONFIDENCE +
                (chatMessageCount.toFloat() / SAT_CHAT_MESSAGES ).coerceIn(0f, 1f) * W_CHAT_MESSAGES

            val tier = when {
                v >= THRESHOLD_HIGH     -> InsightTier.HIGH
                v >= THRESHOLD_MODERATE -> InsightTier.MODERATE
                v >= THRESHOLD_LOW      -> InsightTier.LOW
                else                    -> InsightTier.FORMING
            }

            return ConfidenceScore(
                value                = v,
                tier                 = tier,
                moodLogCount         = moodLogCount,
                sessionCount         = sessionCount,
                avgEmotionConfidence = avgEmotionConfidence,
                chatMessageCount     = chatMessageCount
            )
        }
    }
}

/**
 * Tiered gate that controls what kind of insight content is shown to the user.
 *
 * The four tiers are designed to build trust progressively:
 *  - [FORMING]  — not enough data; show a warm placeholder instead of a real insight.
 *  - [LOW]      — enough mood data for a basic AI pattern sentence.
 *  - [MODERATE] — enough combined data for a personalized insight.
 *  - [HIGH]     — rich chat + mood history; fully personalized insight with session context.
 */
enum class InsightTier {
    /** Composite score < 0.20. User has very little data — insight would be meaningless. */
    FORMING,

    /** Composite score 0.20–0.49. Basic mood pattern insight is viable. */
    LOW,

    /** Composite score 0.50–0.74. Personalized insight enabled, moderate depth. */
    MODERATE,

    /** Composite score ≥ 0.75. Full personalization with chat session context. */
    HIGH
}
