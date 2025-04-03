package com.iamashad.meraki.model

/**
 * Data model representing a single message in a chat interaction.
 *
 * @property message The textual content of the message.
 * @property role The role of the sender (e.g., "user", "assistant").
 */
data class Message(
    val message: String,
    val role: String,
)
