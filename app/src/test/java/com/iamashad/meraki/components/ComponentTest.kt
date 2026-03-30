package com.iamashad.meraki.components

import android.text.format.DateFormat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.google.common.truth.Truth.assertThat
import com.iamashad.meraki.model.Journal
import com.iamashad.meraki.utils.CompactDimens
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.getMoodLabelFromTitle
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ComponentTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ─────────────────────────── AppSearchBar ────────────────────────────

    @Test
    fun `AppSearchBar - onQueryChanged is invoked when user types text`() {
        var captured = ""
        composeTestRule.setContent {
            AppSearchBar(
                query = "",
                onQueryChanged = { captured = it },
                onClearQuery = {}
            )
        }

        composeTestRule.onNodeWithText("Search...").performTextInput("calm")

        assertThat(captured).isEqualTo("calm")
    }

    @Test
    fun `AppSearchBar - Clear Search button is visible when query is not empty`() {
        composeTestRule.setContent {
            AppSearchBar(
                query = "anxiety",
                onQueryChanged = {},
                onClearQuery = {}
            )
        }

        composeTestRule.onNodeWithContentDescription("Clear Search").assertIsDisplayed()
    }

    @Test
    fun `AppSearchBar - Clear Search button is absent when query is empty`() {
        composeTestRule.setContent {
            AppSearchBar(
                query = "",
                onQueryChanged = {},
                onClearQuery = {}
            )
        }

        composeTestRule.onAllNodesWithContentDescription("Clear Search").assertCountEquals(0)
    }

    @Test
    fun `AppSearchBar - onClearQuery is invoked when the X icon is clicked`() {
        var cleared = false
        composeTestRule.setContent {
            AppSearchBar(
                query = "stress",
                onQueryChanged = {},
                onClearQuery = { cleared = true }
            )
        }

        composeTestRule.onNode(
            hasContentDescription("Clear Search") and hasClickAction()
        ).performClick()

        assertThat(cleared).isTrue()
    }

    // ─────────────────────────── JournalCard ─────────────────────────────

    private fun makeJournal(title: String, dateMs: Long = System.currentTimeMillis()) = Journal(
        journalId = "j1",
        userId = "u1",
        title = title,
        content = "Today was a good day.",
        moodScore = 75,
        reasons = listOf("Work", "Exercise"),
        date = dateMs
    )

    @Test
    fun `JournalCard - renders the mood label derived from the journal title`() {
        val journal = makeJournal("Happy")
        val expectedLabel = getMoodLabelFromTitle(journal.title) // "Good"

        composeTestRule.setContent {
            JournalCard(
                journal = journal,
                onEditClick = {},
                onDeleteButtonClick = {}
            )
        }

        composeTestRule.onNodeWithText(expectedLabel).assertIsDisplayed()
    }

    @Test
    fun `JournalCard - renders the formatted time from the journal date`() {
        val fixedMs = 1_700_000_000_000L
        val journal = makeJournal("Calm", dateMs = fixedMs)
        // Use the same DateFormat the component uses so the test is timezone-agnostic.
        val expectedTime = DateFormat.format("HH:mm", fixedMs).toString()

        composeTestRule.setContent {
            JournalCard(
                journal = journal,
                onEditClick = {},
                onDeleteButtonClick = {}
            )
        }

        composeTestRule.onNodeWithText(expectedTime).assertIsDisplayed()
    }

    // ─────────────────────────── EmotionChip ─────────────────────────────

    @Test
    fun `EmotionChip - renders the emoji and emotion name`() {
        composeTestRule.setContent {
            EmotionChip(
                emotionName = "Happy",
                emoji = "😊",
                isSelected = false,
                onClick = {}
            )
        }

        composeTestRule.onNodeWithText("😊").assertIsDisplayed()
        composeTestRule.onNodeWithText("Happy").assertIsDisplayed()
    }

    @Test
    fun `EmotionChip - onClick is invoked when the chip is clicked`() {
        var clicked = false
        composeTestRule.setContent {
            EmotionChip(
                emotionName = "Calm",
                emoji = "😌",
                isSelected = false,
                onClick = { clicked = true }
            )
        }

        composeTestRule.onNodeWithText("😌").performClick()

        assertThat(clicked).isTrue()
    }

    @Test
    fun `EmotionChip - both selected and unselected states render the chip content`() {
        var isSelected by mutableStateOf(false)
        composeTestRule.setContent {
            EmotionChip(
                emotionName = "Excited",
                emoji = "🤩",
                isSelected = isSelected,
                onClick = { isSelected = !isSelected }
            )
        }

        // Unselected: chip content visible
        composeTestRule.onNodeWithText("Excited").assertIsDisplayed()

        // Transition to selected via click
        composeTestRule.onNodeWithText("🤩").performClick()
        assertThat(isSelected).isTrue()

        // Selected: chip content still visible
        composeTestRule.onNodeWithText("Excited").assertIsDisplayed()

        // Transition back to unselected
        composeTestRule.onNodeWithText("🤩").performClick()
        assertThat(isSelected).isFalse()
    }
}
