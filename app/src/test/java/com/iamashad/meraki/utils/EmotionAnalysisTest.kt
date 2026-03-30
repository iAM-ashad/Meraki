package com.iamashad.meraki.utils

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [analyzeEmotion].
 *
 * The function iterates [emotionKeywords] in insertion order:
 * calm → stressed → anxious → neutral → happy → sad → angry
 * and returns the first matching category, or "neutral" for no match.
 */
class EmotionAnalysisTest {

    // ── happy ────────────────────────────────────────────────────────────────

    @Test
    fun `analyzeEmotion returns happy for keyword joyful`() {
        assertThat(analyzeEmotion("I feel joyful today")).isEqualTo("happy")
    }

    @Test
    fun `analyzeEmotion returns happy for keyword excited`() {
        assertThat(analyzeEmotion("I am so excited!")).isEqualTo("happy")
    }

    @Test
    fun `analyzeEmotion returns happy for keyword ecstatic`() {
        assertThat(analyzeEmotion("feeling ecstatic right now")).isEqualTo("happy")
    }

    // ── sad ──────────────────────────────────────────────────────────────────

    @Test
    fun `analyzeEmotion returns sad for keyword sad`() {
        assertThat(analyzeEmotion("I feel sad today")).isEqualTo("sad")
    }

    @Test
    fun `analyzeEmotion returns sad for keyword heartbroken`() {
        assertThat(analyzeEmotion("I am heartbroken")).isEqualTo("sad")
    }

    @Test
    fun `analyzeEmotion returns sad for keyword miserable`() {
        assertThat(analyzeEmotion("Everything feels miserable")).isEqualTo("sad")
    }

    // ── calm ─────────────────────────────────────────────────────────────────

    @Test
    fun `analyzeEmotion returns calm for keyword relaxed`() {
        assertThat(analyzeEmotion("I feel relaxed after the walk")).isEqualTo("calm")
    }

    @Test
    fun `analyzeEmotion returns calm for keyword serene`() {
        assertThat(analyzeEmotion("The morning was serene")).isEqualTo("calm")
    }

    // ── stressed ─────────────────────────────────────────────────────────────

    @Test
    fun `analyzeEmotion returns stressed for keyword overwhelmed`() {
        assertThat(analyzeEmotion("I am overwhelmed with work")).isEqualTo("stressed")
    }

    @Test
    fun `analyzeEmotion returns stressed for keyword burnout`() {
        assertThat(analyzeEmotion("Reaching burnout stage")).isEqualTo("stressed")
    }

    // ── anxious ──────────────────────────────────────────────────────────────

    @Test
    fun `analyzeEmotion returns anxious for keyword anxious`() {
        assertThat(analyzeEmotion("I feel anxious about the exam")).isEqualTo("anxious")
    }

    @Test
    fun `analyzeEmotion returns anxious for keyword worried`() {
        assertThat(analyzeEmotion("I am worried about the results")).isEqualTo("anxious")
    }

    @Test
    fun `analyzeEmotion returns anxious for keyword nervous`() {
        assertThat(analyzeEmotion("So nervous before the presentation")).isEqualTo("anxious")
    }

    // ── angry ────────────────────────────────────────────────────────────────

    @Test
    fun `analyzeEmotion returns angry for keyword frustrated`() {
        assertThat(analyzeEmotion("I am frustrated with this bug")).isEqualTo("angry")
    }

    @Test
    fun `analyzeEmotion returns angry for keyword furious`() {
        assertThat(analyzeEmotion("They made me furious")).isEqualTo("angry")
    }

    // ── neutral (fallback) ───────────────────────────────────────────────────

    @Test
    fun `analyzeEmotion returns neutral for unrecognised text`() {
        assertThat(analyzeEmotion("I was reading a book")).isEqualTo("neutral")
    }

    @Test
    fun `analyzeEmotion returns neutral for empty string`() {
        assertThat(analyzeEmotion("")).isEqualTo("neutral")
    }

    @Test
    fun `analyzeEmotion returns neutral for whitespace only`() {
        assertThat(analyzeEmotion("   ")).isEqualTo("neutral")
    }

    // ── edge cases ───────────────────────────────────────────────────────────

    @Test
    fun `analyzeEmotion is case insensitive for HAPPY`() {
        assertThat(analyzeEmotion("HAPPY day")).isEqualTo("happy")
    }

    @Test
    fun `analyzeEmotion is case insensitive for mixed case JOYFUL`() {
        assertThat(analyzeEmotion("JoYfUl mood")).isEqualTo("happy")
    }

    @Test
    fun `analyzeEmotion matches keyword embedded in longer sentence`() {
        // "glad" is a happy keyword
        assertThat(analyzeEmotion("I am so glad you came")).isEqualTo("happy")
    }

    @Test
    fun `analyzeEmotion returns first matching category when multiple keywords present`() {
        // "relaxed" (calm) appears before "sad" in emotionKeywords iteration order
        // → expects "calm" since calm is evaluated first
        assertThat(analyzeEmotion("feeling relaxed but also sad")).isEqualTo("calm")
    }
}
