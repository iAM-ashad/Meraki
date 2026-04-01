package com.iamashad.meraki.model

import androidx.compose.runtime.Immutable

/**
 * Represents the data for the Living Mood Card states.
 */
@Immutable
data class MoodInsight(
    val weeklySummary: String? = null,
    val patternAlert: PatternAlert? = null,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Immutable
data class PatternAlert(
    val title: String,
    val message: String,
    val actionType: AlertActionType
)

enum class AlertActionType {
    BREATHING,
    CHATBOT,
    JOURNAL
}
