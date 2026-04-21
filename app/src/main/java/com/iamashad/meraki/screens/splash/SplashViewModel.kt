package com.iamashad.meraki.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.iamashad.meraki.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Lightweight ViewModel that exposes the [hasCompletedOnboarding] flag from
 * [UserPreferencesRepository] to [SplashScreen] as a [StateFlow].
 *
 * Navigation fires as soon as DataStore emits a non-null value — there is no
 * artificial delay, so the app reaches the real destination as fast as possible.
 */
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    /**
     * True once the user has completed the full onboarding arc (set after [WelcomeAIScreen]).
     * Defaults to `null` until the DataStore value is read — this prevents returning
     * authenticated users from being briefly routed to [Onboarding] on slow reads.
     *
     * [SplashScreen] waits for the first non-null emission before navigating.
     */
    val hasCompletedOnboarding: StateFlow<Boolean?> =
        userPreferencesRepository.hasCompletedOnboarding
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = null // null = "loading" — SplashScreen waits until non-null
            )
}
