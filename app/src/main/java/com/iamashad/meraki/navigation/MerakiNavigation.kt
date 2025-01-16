package com.iamashad.meraki.navigation

import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.R
import com.iamashad.meraki.screens.about.AboutScreen
import com.iamashad.meraki.screens.breathing.BreathingScreen
import com.iamashad.meraki.screens.chatbot.ChatViewModel
import com.iamashad.meraki.screens.chatbot.ChatbotScreen
import com.iamashad.meraki.screens.home.HomeScreen
import com.iamashad.meraki.screens.insights.InsightsViewModel
import com.iamashad.meraki.screens.insights.MoodInsightsScreen
import com.iamashad.meraki.screens.journal.AddJournalScreen
import com.iamashad.meraki.screens.journal.JournalScreen
import com.iamashad.meraki.screens.journal.JournalViewModel
import com.iamashad.meraki.screens.journal.ViewJournalScreen
import com.iamashad.meraki.screens.moodtracker.MoodTrackerScreen
import com.iamashad.meraki.screens.moodtracker.MoodTrackerViewModel
import com.iamashad.meraki.screens.register.OnBoardingScreen
import com.iamashad.meraki.screens.register.RegisterScreen
import com.iamashad.meraki.screens.register.RegisterViewModel
import com.iamashad.meraki.screens.settings.SettingsScreen
import com.iamashad.meraki.screens.splash.SplashScreen

@Composable
fun MerakiNavigation() {
    val navController = rememberNavController()

    val currentDestination =
        navController.currentBackStackEntryFlow.collectAsState(initial = null).value?.destination?.route

    Scaffold(modifier = Modifier.fillMaxSize(), bottomBar = {
        if (shouldShowBottomBar(currentDestination)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(getResponsiveNavBarHeight())
                    .background(
                        Color.Black
                    )
            ) {
                BottomNavigationBar(navController)
            }

        }
    }) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screens.SPLASH.name,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(600, easing = EaseIn)
                )
            },
            exitTransition = {
                fadeOut(
                    animationSpec = tween(600, easing = LinearEasing)
                )
            },
            modifier = Modifier.padding(paddingValues)
        ) {

            composable(Screens.SPLASH.name) {
                SplashScreen(navController)
            }

            composable(Screens.HOME.name) {
                HomeScreen(navController)
            }

            composable(Screens.REGISTER.name) {
                RegisterScreen(navController)
            }
            composable(Screens.ABOUT.name) {
                AboutScreen(navController)
            }
            composable(Screens.INSIGHTS.name) {
                val viewModel = hiltViewModel<InsightsViewModel>()
                MoodInsightsScreen(viewModel)
            }
            composable(Screens.ONBOARDING.name) {
                val viewModel = hiltViewModel<RegisterViewModel>()
                OnBoardingScreen(navController, viewModel)
            }

            composable(
                route = "${Screens.CHATBOT.name}/{prompt}",
                arguments = listOf(navArgument("prompt") { defaultValue = "" })
            ) { backStackEntry ->
                val viewModel = hiltViewModel<ChatViewModel>()
                ChatbotScreen(viewModel, navController)
            }
            composable(Screens.MOODTRACKER.name) {
                val moodTrackerViewModel = hiltViewModel<MoodTrackerViewModel>()
                MoodTrackerScreen(onMoodLogged = {
                    moodTrackerViewModel.fetchMoodTrend()
                })
            }

            composable(Screens.SETTINGS.name) {
                SettingsScreen(navController)
            }

            composable(Screens.BREATHING.name) {
                BreathingScreen(navController)
            }

            composable(
                route = "${Screens.ADDJOURNAL.name}/{journalId}",
                arguments = listOf(navArgument("journalId") { defaultValue = "" })
            ) { backStackEntry ->
                val journalId = backStackEntry.arguments?.getString("journalId").orEmpty()
                val viewModel = hiltViewModel<JournalViewModel>()

                AddJournalScreen(viewModel = viewModel,
                    userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                    journalId = journalId,
                    onClose = { navController.popBackStack() },
                    onSave = { navController.popBackStack() })
            }

            composable(Screens.JOURNAL.name) {
                val viewModel = hiltViewModel<JournalViewModel>()
                JournalScreen(viewModel = viewModel, onAddJournalClick = {
                    val newJournalId =
                        FirebaseFirestore.getInstance().collection("journals").document().id
                    navController.navigate("${Screens.ADDJOURNAL.name}/$newJournalId")
                }, onViewJournalClick = { journal ->
                    navController.navigate("${Screens.VIEWJOURNAL.name}/${journal.journalId}")
                })
            }


            composable(
                route = "${Screens.VIEWJOURNAL.name}/{journalId}",
                arguments = listOf(navArgument("journalId") { defaultValue = "" })
            ) { backStackEntry ->
                val journalId = backStackEntry.arguments?.getString("journalId") ?: ""
                val viewModel = hiltViewModel<JournalViewModel>()

                val journals by viewModel.journals.collectAsState()
                val searchResults by viewModel.searchResults.collectAsState()
                val isSearching by viewModel.isSearching.collectAsState()

                val displayedJournals = if (isSearching) searchResults else journals

                val journal = displayedJournals.find { it.journalId == journalId }

                if (journal != null) {
                    ViewJournalScreen(journal = journal)
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Loading or Journal not found.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun getResponsiveNavBarHeight(): Dp {
    val configuration = LocalConfiguration.current
    val screenHeightDp = configuration.screenHeightDp

    return when {
        screenHeightDp < 600 -> 56.dp // Small devices
        screenHeightDp < 800 -> 64.dp // Medium devices
        else -> 72.dp // Large devices
    }
}

@Composable
fun shouldShowBottomBar(currentDestination: String?): Boolean {
    return currentDestination in listOf(
        Screens.HOME.name,
        Screens.CHATBOT.name,
        Screens.MOODTRACKER.name,
        Screens.JOURNAL.name,
        Screens.INSIGHTS.name
    )
}

@Composable
fun BottomNavigationBar(navController: NavController) {
    val items = listOf(
        NavigationItem("Home", Screens.HOME.name, R.drawable.home_123),
        NavigationItem("Chatbot", "${Screens.CHATBOT.name}/Hi", R.drawable.ic_chat),
        NavigationItem("Health", Screens.MOODTRACKER.name, R.drawable.ic_moodtracker),
        NavigationItem("Journal", Screens.JOURNAL.name, R.drawable.ic_journal),
        NavigationItem("Insights", Screens.INSIGHTS.name, R.drawable.ic_insights)
    )

    NavigationBar(
        containerColor = Color.Transparent,
        tonalElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        items.forEach { item ->
            val currentDestination = navController.currentBackStackEntry?.destination?.route
            val isSelected = currentDestination == item.route

            NavigationBarItem(icon = {
                Icon(
                    painter = painterResource(id = item.icon),
                    contentDescription = item.label,
                    tint = if (isSelected) Color.White else Color.White.copy(
                        alpha = 0.5f
                    ),
                    modifier = Modifier.scale(.35f)
                )
            }, selected = isSelected, onClick = {
                if (!isSelected) { // Prevent redundant navigation
                    try {
                        navController.navigate(item.route) {
                            popUpTo(Screens.HOME.name) { saveState = true }
                            launchSingleTop = true
                        }
                    } catch (e: Exception) {
                        e.printStackTrace() // Log navigation errors
                    }
                }
            }, colors = NavigationBarItemDefaults.colors(
                indicatorColor = Color.Transparent
            )
            )
        }
    }
}


data class NavigationItem(
    val label: String, val route: String, val icon: Int
)

