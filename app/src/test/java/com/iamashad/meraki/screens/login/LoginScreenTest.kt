package com.iamashad.meraki.screens.login

import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.navigation.compose.rememberNavController
import androidx.compose.ui.text.input.ImeAction
import com.google.common.truth.Truth.assertThat
import com.iamashad.meraki.screens.register.AuthUiState
import com.iamashad.meraki.screens.register.RegisterViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * UI tests for [LoginScreen] — focused on the password visibility toggle.
 *
 * Strategy
 * --------
 * The password visibility toggle button exposes its current state via its
 * content description:
 *   - "Show Password" → field is currently masked   (password hidden)
 *   - "Hide Password" → field is currently revealed (password shown)
 *
 * This gives us a stable, semantics-correct test hook that works reliably
 * across all Compose versions, independent of how EditableText is populated
 * internally by PasswordVisualTransformation.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun buildFakeViewModel(): RegisterViewModel =
        mockk<RegisterViewModel>(relaxed = true).also {
            every { it.uiState } returns MutableStateFlow(AuthUiState())
            every { it.events } returns emptyFlow()
        }

    // ─────────────────────── helpers ─────────────────────────────────────

    /** Sets up the LoginScreen with a mocked ViewModel and a real NavController. */
    private fun setUpLoginScreen(onNavigateToRegister: () -> Unit = {}) {
        val viewModel = buildFakeViewModel()
        composeTestRule.setContent {
            val navController = rememberNavController()
            LoginScreen(
                viewModel = viewModel,
                navController = navController,
                onNavigateToRegister = onNavigateToRegister
            )
        }
        composeTestRule.waitForIdle()
    }

    /** Finds the password field via ImeAction.Done and types [text] into it. */
    private fun enterPassword(text: String) {
        composeTestRule
            .onNode(hasSetTextAction() and hasImeAction(ImeAction.Done))
            .performTextInput(text)
    }

    // ────────────────────────── tests ────────────────────────────────────

    @Test
    fun `password field is initially masked - toggle button shows Show Password`() {
        setUpLoginScreen()

        // Before any interaction the toggle button should indicate the field is hidden.
        // "Show Password" means "tap me to show the password" → currently masked.
        composeTestRule
            .onNodeWithContentDescription("Show Password")
            .assertExists()
    }

    @Test
    fun `clicking the eye icon reveals the password - button changes to Hide Password`() {
        setUpLoginScreen()
        enterPassword("mys3cr3t")

        // Sanity: button starts in the "Show" (masked) state
        composeTestRule
            .onNodeWithContentDescription("Show Password")
            .assertExists()

        // Toggle password visibility
        composeTestRule
            .onNodeWithContentDescription("Show Password")
            .performClick()
        composeTestRule.waitForIdle()

        // After toggle the button description must change to "Hide Password"
        // confirming the field is now in the revealed state.
        composeTestRule
            .onNodeWithContentDescription("Hide Password")
            .assertExists()
    }

    @Test
    fun `clicking the eye icon twice hides the password again - button returns to Show Password`() {
        setUpLoginScreen()
        enterPassword("p4ssw0rd")

        // First click → reveal
        composeTestRule
            .onNodeWithContentDescription("Show Password")
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithContentDescription("Hide Password").assertExists()

        // Second click → mask again
        composeTestRule
            .onNodeWithContentDescription("Hide Password")
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("Show Password")
            .assertExists()
    }

    @Test
    fun `the eye icon toggle button is always present in the password field`() {
        setUpLoginScreen()

        // The icon must exist before any input (either description satisfies the requirement)
        val showNode  = composeTestRule.onNodeWithContentDescription("Show Password")
        val hideNode  = composeTestRule.onNodeWithContentDescription("Hide Password")
        // At startup exactly one of them exists — the "Show" state (initially masked).
        showNode.assertExists()
    }

    @Test
    fun `clicking Sign Up button fires the onNavigateToRegister callback`() {
        var navigateCalled = false
        setUpLoginScreen(onNavigateToRegister = { navigateCalled = true })

        composeTestRule
            .onNodeWithText("Don't have an account? Sign Up")
            .performScrollTo()
            .performClick()

        assertThat(navigateCalled).isTrue()
    }
}
