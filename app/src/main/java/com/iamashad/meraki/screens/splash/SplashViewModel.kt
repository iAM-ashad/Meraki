package com.iamashad.meraki.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iamashad.meraki.di.IoDispatcher
import com.iamashad.meraki.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Onboarding Overhaul — Phase 1.
 *
 * Lightweight ViewModel that exposes the [hasCompletedOnboarding] flag from
 * [UserPreferencesRepository] to [SplashScreen] as a [StateFlow].
 *
 * This is the recommended way to expose DataStore values in Compose — avoids
 * passing repository references into composable functions directly.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    /**
     * True once the user has completed the full onboarding arc (set after [WelcomeAIScreen]).
     * Defaults to `true` until the DataStore value is read — this prevents returning
     * authenticated users from being briefly routed to [Onboarding] on slow reads.
     *
     * IMPORTANT: [SplashScreen] must wait until the first non-default emission before
     * navigating, since the initial `true` is a placeholder.
     * The screen uses [WhileSubscribed] so the flow is cleaned up when the screen leaves.
     */
    val hasCompletedOnboarding: StateFlow<Boolean?> =
        userPreferencesRepository.hasCompletedOnboarding
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null // null = "loading" — SplashScreen waits until non-null
            )
}
