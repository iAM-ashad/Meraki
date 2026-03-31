package com.iamashad.meraki.model

import androidx.compose.runtime.Immutable

/**
 * Represents a personalized "nudge" — a bite-sized piece of insight, 
 * reflection, or affirmation tailored to the user's mental state.
 */
@Immutable
data class MindfulNudge(
    val text: String?,
    val type: NudgeType,
    val source: String? = "Meraki AI"
)

enum class NudgeType {
    AFFIRMATION, // Emotional support and validation
    REFLECTION,  // Thought-provoking questions based on history
    INSIGHT      // Pattern detection and observations
}
