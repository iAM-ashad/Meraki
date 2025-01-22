package com.iamashad.meraki.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
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
import com.iamashad.meraki.utils.LocalDimens
import com.iamashad.meraki.utils.ProvideDimens

@Composable
fun MerakiNavigation() {
    val navController = rememberNavController()
    val currentDestination by navController.currentBackStackEntryFlow.collectAsState(initial = null)
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp

    ProvideDimens(screenWidth, screenHeight) {
        val dimens = LocalDimens.current

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                if (shouldShowBottomBar(currentDestination?.destination?.route)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(dimens.paddingSmall * 10)
                            .background(MaterialTheme.colorScheme.background),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        BottomNavigationBar(navController)
                    }
                }
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                startDestination = Screens.INSIGHTS.name,
                enterTransition = { fadeInAnimation() },
                exitTransition = { fadeOutAnimation() },
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(Screens.SPLASH.name) { SplashScreen(navController) }
                composable(Screens.HOME.name) { HomeScreen(navController) }
                composable(Screens.REGISTER.name) { RegisterScreen(navController) }
                composable(Screens.ABOUT.name) { AboutScreen(navController) }
                composable(Screens.INSIGHTS.name) {
                    val viewModel = hiltViewModel<InsightsViewModel>()
                    MoodInsightsScreen(viewModel, navController)
                }
                composable(Screens.ONBOARDING.name) {
                    val viewModel = hiltViewModel<RegisterViewModel>()
                    OnBoardingScreen(navController, viewModel)
                }
                composable(
                    route = "${Screens.CHATBOT.name}/{prompt}",
                    arguments = listOf(navArgument("prompt") { defaultValue = "" })
                ) {
                    val viewModel = hiltViewModel<ChatViewModel>()
                    ChatbotScreen(viewModel, navController)
                }
                composable(Screens.MOODTRACKER.name) {
                    val viewModel = hiltViewModel<MoodTrackerViewModel>()
                    MoodTrackerScreen(onMoodLogged = { viewModel.fetchMoodTrend() })
                }
                composable(Screens.SETTINGS.name) {
                    val viewModel = hiltViewModel<ChatViewModel>()
                    SettingsScreen(navController, viewModel)
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
                    AddJournalScreen(
                        viewModel = viewModel,
                        userId = FirebaseAuth.getInstance().currentUser?.uid.orEmpty(),
                        journalId = journalId,
                        onClose = { navController.popBackStack() },
                        onSave = { navController.popBackStack() }
                    )
                }
                composable(Screens.JOURNAL.name) {
                    val viewModel = hiltViewModel<JournalViewModel>()
                    JournalScreen(
                        viewModel = viewModel,
                        onAddJournalClick = {
                            val newJournalId =
                                FirebaseFirestore.getInstance().collection("journals").document().id
                            navController.navigate("${Screens.ADDJOURNAL.name}/$newJournalId")
                        },
                        onViewJournalClick = { journal ->
                            navController.navigate("${Screens.VIEWJOURNAL.name}/${journal.journalId}")
                        }
                    )
                }
                composable(
                    route = "${Screens.VIEWJOURNAL.name}/{journalId}",
                    arguments = listOf(navArgument("journalId") { defaultValue = "" })
                ) { backStackEntry ->
                    val journalId = backStackEntry.arguments?.getString("journalId").orEmpty()
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
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
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
}

fun fadeInAnimation(): EnterTransition {
    return fadeIn(animationSpec = tween(600, easing = EaseIn))
}

fun fadeOutAnimation(): ExitTransition {
    return fadeOut(animationSpec = tween(600, easing = LinearEasing))
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
fun BottomNavigationBar(
    navController: NavController
) {
    val items = listOf(
        NavigationItem("Health", Screens.MOODTRACKER.name, R.drawable.ic_moodtracker),
        NavigationItem("Chatbot", "${Screens.CHATBOT.name}/Hi", R.drawable.ic_chat),
        NavigationItem("Home", Screens.HOME.name, R.drawable.home_123),
        NavigationItem("Journal", Screens.JOURNAL.name, R.drawable.ic_journal),
        NavigationItem("Insights", Screens.INSIGHTS.name, R.drawable.ic_insights)
    )

    val dimens = LocalDimens.current

    NavigationBar(
        containerColor = Color.Transparent,
        tonalElevation = dimens.elevation,
        modifier = Modifier
            .fillMaxWidth()
    ) {
        items.forEach { item ->
            val currentDestination = navController.currentBackStackEntry?.destination?.route
            val isSelected = currentDestination == item.route
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0.7f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = 50f
                )
            )
            val offset by animateDpAsState(
                targetValue = if (isSelected) (-(5.dp)) else 0.dp,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = 50f
                )
            )

            NavigationBarItem(
                icon = {
                    Box(
                        modifier = Modifier
                            .padding(top = dimens.paddingSmall)
                            .offset(y = offset)
                            .clip(CircleShape)
                            .background(
                                brush = if (isSelected) {
                                    Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.onBackground,
                                            MaterialTheme.colorScheme.primary
                                        )
                                    )
                                } else {
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Transparent
                                        )
                                    )
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = item.icon),
                            contentDescription = item.label,
                            tint = if (isSelected) MaterialTheme.colorScheme.background else MaterialTheme.colorScheme.onBackground.copy(
                                0.8f
                            ),
                            modifier = Modifier
                                .scale(scale)
                                .padding(dimens.paddingMedium)
                        )
                    }
                },
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        try {
                            navController.navigate(item.route) {
                                popUpTo(Screens.HOME.name) { saveState = true }
                                launchSingleTop = true
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent
                ),
                modifier = Modifier.align(Alignment.Bottom)
            )
        }
    }
}


data class NavigationItem(
    val label: String, val route: String, val icon: Int
)


