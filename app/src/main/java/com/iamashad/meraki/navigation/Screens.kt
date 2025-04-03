package com.iamashad.meraki.navigation

/**
 * Enum class representing all the screen routes used in the app's navigation.
 * Each enum value corresponds to a destination in the navigation graph.
 */
enum class Screens {
    HOME,         // Home screen after user login
    SPLASH,       // Initial splash/loading screen
    REGISTER,     // Registration screen
    CHATBOT,      // AI Chatbot interface
    MOODTRACKER,  // Screen for logging and viewing mood entries
    BREATHING,    // Breathing exercise screen
    JOURNAL,      // Journal entries listing screen
    ADDJOURNAL,   // Screen to create or update a journal entry
    VIEWJOURNAL,  // Detailed view of a selected journal entry
    SETTINGS,     // App settings screen
    ONBOARDING,   // Intro/tutorial shown on first app launch
    INSIGHTS,     // Mood analytics and visual insights
    LOGIN,        // Login screen
    CREATEUSER    // Create user profile screen after registration
}
