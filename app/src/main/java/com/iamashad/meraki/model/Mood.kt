package com.iamashad.meraki.model

/**
 * Data model representing a mood entry for a user.
 *
 * @property id Unique ID for the mood entry (usually from Firestore).
 * @property userId ID of the user this mood entry belongs to.
 * @property score Numeric representation of the mood (e.g., scale from -100 to 100).
 * @property label Text label for the mood (e.g., "Happy", "Sad").
 * @property timestamp Time when the mood was recorded (epoch time in milliseconds).
 */
data class Mood(
    val id: String = "",           // Default empty string
    val userId: String = "",       // Default empty string
    val score: Int = 0,            // Default mood score
    val label: String = "",        // Default mood label
    val timestamp: Long = 0L       // Default to epoch time
) {
    /**
     * No-argument constructor required for Firestore deserialization.
     */
    constructor() : this("", "", 0, "", 0L)
}
