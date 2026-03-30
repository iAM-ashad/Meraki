package com.iamashad.meraki.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for the four @Immutable data-class models.
 *
 * Verifies:
 *  - structural equality (equals / hashCode contract)
 *  - copy() correctness — both no-arg shallow copy and field-overriding copy
 *  - default values where declared
 *  - inequality when any field differs
 */
class ModelTest {

    // ══════════════════════════════════════════════════════════════════════════
    // Journal
    // ══════════════════════════════════════════════════════════════════════════

    private val sampleJournal = Journal(
        journalId = "j1",
        userId    = "u1",
        title     = "Happy",
        content   = "Had a wonderful day",
        moodScore = 85,
        reasons   = listOf("Work", "Friends"),
        date      = 1_700_000_000_000L,
        imageUrl  = "https://example.com/img.jpg"
    )

    @Test
    fun `Journal - identical instances are equal`() {
        val copy = sampleJournal.copy()
        assertThat(copy).isEqualTo(sampleJournal)
    }

    @Test
    fun `Journal - identical instances have equal hashCodes`() {
        val copy = sampleJournal.copy()
        assertThat(copy.hashCode()).isEqualTo(sampleJournal.hashCode())
    }

    @Test
    fun `Journal - copy preserves all fields when no overrides supplied`() {
        val copy = sampleJournal.copy()
        assertThat(copy.journalId).isEqualTo(sampleJournal.journalId)
        assertThat(copy.userId).isEqualTo(sampleJournal.userId)
        assertThat(copy.title).isEqualTo(sampleJournal.title)
        assertThat(copy.content).isEqualTo(sampleJournal.content)
        assertThat(copy.moodScore).isEqualTo(sampleJournal.moodScore)
        assertThat(copy.reasons).containsExactlyElementsIn(sampleJournal.reasons).inOrder()
        assertThat(copy.date).isEqualTo(sampleJournal.date)
        assertThat(copy.imageUrl).isEqualTo(sampleJournal.imageUrl)
    }

    @Test
    fun `Journal - copy with new title differs from original`() {
        val modified = sampleJournal.copy(title = "Sad")
        assertThat(modified.title).isEqualTo("Sad")
        assertThat(modified).isNotEqualTo(sampleJournal)
    }

    @Test
    fun `Journal - copy with new moodScore differs from original`() {
        val modified = sampleJournal.copy(moodScore = 20)
        assertThat(modified.moodScore).isEqualTo(20)
        assertThat(modified).isNotEqualTo(sampleJournal)
    }

    @Test
    fun `Journal - imageUrl defaults to null when omitted`() {
        val journal = Journal(
            journalId = "j2",
            userId    = "u2",
            title     = "Calm",
            content   = "Quiet afternoon",
            moodScore = 70,
            reasons   = emptyList(),
            date      = 1_000L
        )
        assertThat(journal.imageUrl).isNull()
    }

    @Test
    fun `Journal - two instances with different journalId are not equal`() {
        val other = sampleJournal.copy(journalId = "j99")
        assertThat(sampleJournal).isNotEqualTo(other)
    }

    @Test
    fun `Journal - reasons list equality is value-based`() {
        val j1 = sampleJournal.copy(reasons = listOf("Work", "Friends"))
        val j2 = sampleJournal.copy(reasons = listOf("Work", "Friends"))
        assertThat(j1).isEqualTo(j2)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Message
    // ══════════════════════════════════════════════════════════════════════════

    private val sampleMessage = Message(message = "Hello, how are you?", role = "user")

    @Test
    fun `Message - identical instances are equal`() {
        val copy = sampleMessage.copy()
        assertThat(copy).isEqualTo(sampleMessage)
    }

    @Test
    fun `Message - identical instances have equal hashCodes`() {
        val copy = sampleMessage.copy()
        assertThat(copy.hashCode()).isEqualTo(sampleMessage.hashCode())
    }

    @Test
    fun `Message - copy preserves message and role`() {
        val copy = sampleMessage.copy()
        assertThat(copy.message).isEqualTo(sampleMessage.message)
        assertThat(copy.role).isEqualTo(sampleMessage.role)
    }

    @Test
    fun `Message - copy with new role differs from original`() {
        val modified = sampleMessage.copy(role = "assistant")
        assertThat(modified.role).isEqualTo("assistant")
        assertThat(modified).isNotEqualTo(sampleMessage)
    }

    @Test
    fun `Message - copy with new message differs from original`() {
        val modified = sampleMessage.copy(message = "I am fine, thanks!")
        assertThat(modified.message).isEqualTo("I am fine, thanks!")
        assertThat(modified).isNotEqualTo(sampleMessage)
    }

    @Test
    fun `Message - instances with same content but different roles are not equal`() {
        val user = Message("Hi", "user")
        val assistant = Message("Hi", "assistant")
        assertThat(user).isNotEqualTo(assistant)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Mood
    // ══════════════════════════════════════════════════════════════════════════

    private val sampleMood = Mood(
        id        = "m1",
        userId    = "u1",
        score     = 75,
        label     = "Great",
        timestamp = 1_700_000_000_000L
    )

    @Test
    fun `Mood - no-arg constructor produces all default values`() {
        val mood = Mood()
        assertThat(mood.id).isEmpty()
        assertThat(mood.userId).isEmpty()
        assertThat(mood.score).isEqualTo(0)
        assertThat(mood.label).isEmpty()
        assertThat(mood.timestamp).isEqualTo(0L)
    }

    @Test
    fun `Mood - identical instances are equal`() {
        val copy = sampleMood.copy()
        assertThat(copy).isEqualTo(sampleMood)
    }

    @Test
    fun `Mood - identical instances have equal hashCodes`() {
        val copy = sampleMood.copy()
        assertThat(copy.hashCode()).isEqualTo(sampleMood.hashCode())
    }

    @Test
    fun `Mood - copy preserves all fields`() {
        val copy = sampleMood.copy()
        assertThat(copy.id).isEqualTo(sampleMood.id)
        assertThat(copy.userId).isEqualTo(sampleMood.userId)
        assertThat(copy.score).isEqualTo(sampleMood.score)
        assertThat(copy.label).isEqualTo(sampleMood.label)
        assertThat(copy.timestamp).isEqualTo(sampleMood.timestamp)
    }

    @Test
    fun `Mood - copy with new score differs from original`() {
        val modified = sampleMood.copy(score = 30)
        assertThat(modified.score).isEqualTo(30)
        assertThat(modified).isNotEqualTo(sampleMood)
    }

    @Test
    fun `Mood - copy with new label differs from original`() {
        val modified = sampleMood.copy(label = "Bad")
        assertThat(modified.label).isEqualTo("Bad")
        assertThat(modified).isNotEqualTo(sampleMood)
    }

    @Test
    fun `Mood - two instances with different ids are not equal`() {
        val other = sampleMood.copy(id = "m99")
        assertThat(sampleMood).isNotEqualTo(other)
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Quotes
    // ══════════════════════════════════════════════════════════════════════════

    private val sampleQuote = Quotes(
        author = "Albert Einstein",
        quote  = "Imagination is more important than knowledge."
    )

    @Test
    fun `Quotes - identical instances are equal`() {
        val copy = sampleQuote.copy()
        assertThat(copy).isEqualTo(sampleQuote)
    }

    @Test
    fun `Quotes - identical instances have equal hashCodes`() {
        val copy = sampleQuote.copy()
        assertThat(copy.hashCode()).isEqualTo(sampleQuote.hashCode())
    }

    @Test
    fun `Quotes - copy preserves author and quote`() {
        val copy = sampleQuote.copy()
        assertThat(copy.author).isEqualTo(sampleQuote.author)
        assertThat(copy.quote).isEqualTo(sampleQuote.quote)
    }

    @Test
    fun `Quotes - copy with new author differs from original`() {
        val modified = sampleQuote.copy(author = "Nikola Tesla")
        assertThat(modified.author).isEqualTo("Nikola Tesla")
        assertThat(modified).isNotEqualTo(sampleQuote)
    }

    @Test
    fun `Quotes - copy with new quote text differs from original`() {
        val modified = sampleQuote.copy(quote = "Life is like riding a bicycle.")
        assertThat(modified.quote).isEqualTo("Life is like riding a bicycle.")
        assertThat(modified).isNotEqualTo(sampleQuote)
    }

    @Test
    fun `Quotes - same quote text different authors are not equal`() {
        val q1 = Quotes("Einstein", "Genius.")
        val q2 = Quotes("Tesla", "Genius.")
        assertThat(q1).isNotEqualTo(q2)
    }

    @Test
    fun `Quotes - same author different quotes are not equal`() {
        val q1 = Quotes("Einstein", "Quote A.")
        val q2 = Quotes("Einstein", "Quote B.")
        assertThat(q1).isNotEqualTo(q2)
    }
}
