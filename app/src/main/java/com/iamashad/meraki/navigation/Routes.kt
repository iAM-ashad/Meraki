package com.iamashad.meraki.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe navigation routes for every screen in the Meraki app.
 *
 * Replaces the [Screens] enum (removed). Each route is a @Serializable
 * object or data class consumed by Navigation Compose 2.8+ composable<T> /
 * NavHost(startDestination = T) APIs.
 *
 * Screens with no arguments  → @Serializable object
 * Screens with arguments     → @Serializable data class with default values
 *                              (defaults make the route usable without arguments
 *                               and keep NavHost startDestination clean).
 */

/** Initial loading / auth-gate screen. */
@Serializable object Splash

/** Main feed shown to authenticated users. */
@Serializable object Home

/** Sign-up entry point (shown when unauthenticated). */
@Serializable object Register

/** First-launch onboarding walkthrough. */
@Serializable object Onboarding

/** Email/password login. */
@Serializable object Login

/** Profile creation step after social sign-in. */
@Serializable object CreateUser

/**
 * AI chatbot screen.
 * [prompt] is kept for future deep-link support (e.g. "Tell me about anxiety").
 * The nav bar always navigates here with the empty default.
 */
@Serializable data class Chatbot(val prompt: String = "")

/** Mood logging and trend view. */
@Serializable data class MoodTracker(val preFilledEmotions: String? = null)

/** App preferences and account management. */
@Serializable object Settings

/** Guided breathing exercise. */
@Serializable object Breathing

/** Journal entries list. */
@Serializable object Journal

/**
 * Create or edit a journal entry.
 * [journalId] is the Firestore document ID; a new (pre-generated) ID is passed
 * when creating, the existing ID when editing.
 */
@Serializable data class AddJournal(val journalId: String = "")

/**
 * Read-only detail view for a journal entry.
 * [journalId] is used to look up the entry in the ViewModel's state.
 */
@Serializable data class ViewJournal(val journalId: String = "")

/** Mood analytics and visual insights dashboard. */
@Serializable object Insights

// ── Onboarding Overhaul routes ────────────────────────────────────────────────

/**
 * Nested nav-graph wrapper for Phase 3–4 screens (MoodSeed → WelcomeMeraki →
 * NotificationSetup).  Declaring a graph route here means every composable nested
 * inside navigation<OnboardingGraph> shares a single back-stack entry, so
 * hiltViewModel() scoped to that entry gives all three screens the same
 * OnboardingViewModel instance — mood selected in MoodSeedScreen is visible in
 * WelcomeAIScreen and NotificationSetupScreen without any extra plumbing.
 */
@Serializable object OnboardingGraph

/**
 * Phase 3: Pre-account mood capture screen.
 * Inserted between [Onboarding] and [CreateUser].
 */
@Serializable object MoodSeed

/**
 * Phase 2: Full-screen avatar picker — step 2 of the sign-up flow.
 * [userId] passed from [CreateUser] after successful Firebase account creation.
 */
@Serializable data class AvatarCelebration(val userId: String = "")

/**
 * Phase 3: AI-generated personalised welcome screen shown after [AvatarCelebration].
 * Streams a Groq welcome using name + mood seed; displays the first journal prompt.
 */
@Serializable object WelcomeMeraki

/**
 * Phase 4: Explained notification opt-in with natural-language time input.
 * Replaces the cold runtime permission dialog. Shown after [WelcomeMeraki].
 */
@Serializable object NotificationSetup
