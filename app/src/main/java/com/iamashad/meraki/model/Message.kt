package com.iamashad.meraki.model

import androidx.compose.runtime.Immutable

/**
 * Data model representing a single message in a chat interaction.
 *
 * @property message The textual content of the message.
 * @property role The role of the sender (e.g., "user", "assistant").
 */
// Phase 6: @Immutable signals to the Compose compiler that all properties are stable,
// enabling skipping of unnecessary recompositions in chat lists.
@Immutable
data class Message(
    val message: String,
    val role: String,
)
