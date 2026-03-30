package com.iamashad.meraki.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.testing.TestNavHostController
import com.google.common.truth.Truth.assertThat
import com.iamashad.meraki.screens.register.AuthUiState
import com.iamashad.meraki.screens.register.RegisterViewModel
import com.iamashad.meraki.screens.login.LoginScreen
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    // ────────────────── 1. Start destination is Splash ───────────────────

    @Test
    fun `NavHost - start destination is Splash`() {
        lateinit var navController: NavController

        composeTestRule.setContent {
            // rememberNavController() registers ComposeNavigator internally.
            // SideEffect ensures navController always captures the stable remembered instance.
            val nc = rememberNavController()
            SideEffect { navController = nc }
            NavHost(navController = nc, startDestination = Splash) {
                composable<Splash> { Text("Splash Screen") }
                composable<Login>  { Text("Login Screen")  }
                composable<Home>   { Text("Home Screen")   }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(
            navController.currentDestination?.hasRoute<Splash>()
        ).isTrue()
    }

    // ────────── 2. Login → CreateUser via onNavigateToRegister ───────────

    @Test
    fun `LoginScreen - clicking Sign Up invokes the onNavigateToRegister callback`() {
        var registerCallbackFired = false

        val fakeViewModel = mockk<RegisterViewModel>(relaxed = true).also {
            every { it.uiState } returns MutableStateFlow(AuthUiState())
            every { it.events } returns emptyFlow()
        }

        composeTestRule.setContent {
            val nc = rememberNavController()
            LoginScreen(
                viewModel = fakeViewModel,
                navController = nc,
                onNavigateToRegister = { registerCallbackFired = true }
            )
        }

        // TextButton is at the bottom of a vertically scrollable Column — scroll first.
        composeTestRule
            .onNodeWithText("Don't have an account? Sign Up")
            .performScrollTo()
            .performClick()

        assertThat(registerCallbackFired).isTrue()
    }

    @Test
    fun `NavHost - Login composable navigates to CreateUser on Sign Up click`() {
        lateinit var navController: NavController

        val fakeViewModel = mockk<RegisterViewModel>(relaxed = true).also {
            every { it.uiState } returns MutableStateFlow(AuthUiState())
            every { it.events } returns emptyFlow()
        }

        composeTestRule.setContent {
            // rememberNavController() + SideEffect: stable NavController across recompositions.
            // Without this, navController = TestNavHostController(...) inside setContent would
            // create a fresh instance on every recomposition (including the one triggered by
            // navigate()), causing the destination assertion to see the start route again.
            val nc = rememberNavController()
            SideEffect { navController = nc }
            NavHost(navController = nc, startDestination = Login) {
                composable<Login> {
                    LoginScreen(
                        viewModel = fakeViewModel,
                        navController = nc,
                        onNavigateToRegister = { nc.navigate(CreateUser) }
                    )
                }
                composable<CreateUser> { Text("Create User Screen") }
            }
        }

        composeTestRule
            .onNodeWithText("Don't have an account? Sign Up")
            .performScrollTo()
            .performClick()

        composeTestRule.waitForIdle()
        assertThat(
            navController.currentDestination?.hasRoute<CreateUser>()
        ).isTrue()
    }

    // ──────────── 3. BottomNavigationBar navigates between tabs ──────────

    @Test
    fun `BottomNavigationBar - clicking Journal navigates to Journal destination`() {
        lateinit var navController: NavController

        composeTestRule.setContent {
            val nc = rememberNavController()
            SideEffect { navController = nc }
            NavHost(navController = nc, startDestination = Home) {
                composable<Home>       { Text("Home Screen")    }
                composable<Journal>    { Text("Journal Screen") }
                composable<Insights>   { Text("Insights Screen") }
                composable<MoodTracker>{ Text("Health Screen")  }
                composable<Chatbot>    { Text("Chatbot Screen") }
            }
            BottomNavigationBar(navController = nc)
        }

        composeTestRule.waitForIdle()
        // Verify start destination is Home
        assertThat(navController.currentDestination?.hasRoute<Home>()).isTrue()

        // Click the Journal icon (contentDescription = "Journal")
        composeTestRule.onNode(
            hasContentDescription("Journal") and hasClickAction()
        ).performClick()

        composeTestRule.waitForIdle()
        assertThat(
            navController.currentDestination?.hasRoute<Journal>()
        ).isTrue()
    }

    @Test
    fun `BottomNavigationBar - clicking Insights navigates to Insights destination`() {
        lateinit var navController: NavController

        composeTestRule.setContent {
            val nc = rememberNavController()
            SideEffect { navController = nc }
            NavHost(navController = nc, startDestination = Home) {
                composable<Home>       { Text("Home Screen")     }
                composable<Journal>    { Text("Journal Screen")  }
                composable<Insights>   { Text("Insights Screen") }
                composable<MoodTracker>{ Text("Health Screen")   }
                composable<Chatbot>    { Text("Chatbot Screen")  }
            }
            BottomNavigationBar(navController = nc)
        }

        composeTestRule.onNode(
            hasContentDescription("Insights") and hasClickAction()
        ).performClick()

        composeTestRule.waitForIdle()
        assertThat(
            navController.currentDestination?.hasRoute<Insights>()
        ).isTrue()
    }

    @Test
    fun `BottomNavigationBar - all five navigation items are present`() {
        composeTestRule.setContent {
            val nc = rememberNavController()
            NavHost(navController = nc, startDestination = Home) {
                composable<Home>       { Text("Home")    }
                composable<Journal>    { Text("Journal") }
                composable<Insights>   { Text("Insights") }
                composable<MoodTracker>{ Text("Health")  }
                composable<Chatbot>    { Text("Chatbot") }
            }
            BottomNavigationBar(navController = nc)
        }

        composeTestRule.onNodeWithContentDescription("Home").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Journal").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Insights").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Health").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Chatbot").assertIsDisplayed()
    }
}
