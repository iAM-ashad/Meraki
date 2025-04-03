package com.iamashad.meraki.model

/**
 * Data model representing a single journal entry.
 *
 * @property journalId Unique ID of the journal entry (used for database or Firestore).
 * @property userId ID of the user who owns this journal.
 * @property title The mood or emotion title (e.g., "Happy", "Sad").
 * @property content Free-form text note written by the user.
 * @property moodScore A numerical representation of the user's mood.
 * @property reasons List of selected reasons explaining the mood.
 * @property date Timestamp of when the journal entry was created (in milliseconds).
 * @property imageUrl Optional URL pointing to an image associated with the journal.
 */
data class Journal(
    val journalId: String,
    val userId: String,
    val title: String,
    val content: String,
    val moodScore: Int,
    val reasons: List<String>,
    val date: Long,
    val imageUrl: String? = null
)
