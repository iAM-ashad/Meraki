package com.iamashad.meraki.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.R
import com.iamashad.meraki.notifications.CheckInWorker
import com.iamashad.meraki.notifications.WeeklyInsightWorker
import com.iamashad.meraki.repository.UserPreferencesRepository
import com.iamashad.meraki.utils.MemoryManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * ViewModel for [SettingsScreen].
 *
 * Phase 5 additions:
 *  - Exposes notification-preference [StateFlow]s backed by [UserPreferencesRepository].
 *  - Exposes setter functions that persist the preference AND schedule/cancel the
 *    corresponding WorkManager job atomically.
 *  - [sessionCount] drives the smart-nudge auto-prompt threshold check in the UI.
 *  - [shouldShowNudgePrompt] combines session count, smart-nudge toggle state, and the
 *    one-shot "has-been-shown" flag to produce a single derivable boolean for the UI.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val memoryManager: MemoryManager
) : ViewModel() {

    // ── Firebase / Profile ────────────────────────────────────────────────────

    private val _user = MutableStateFlow(firebaseAuth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _profilePicRes = MutableStateFlow(R.drawable.avatar1)
    val profilePicRes: StateFlow<Int> = _profilePicRes.asStateFlow()

    /** True while fetchUserProfile() or updateUserAvatar() is in flight. */
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Flips to true once updateUserAvatar() confirms Firestore success.
     * The Settings screen observes this to navigate back to Home only after
     * the write is confirmed, rather than doing an optimistic navigate-immediately.
     * Reset to false after the screen has consumed the event.
     */
    private val _avatarUpdateComplete = MutableStateFlow(false)
    val avatarUpdateComplete: StateFlow<Boolean> = _avatarUpdateComplete.asStateFlow()

    /** Number of stored session summaries (used to gate the smart-nudge auto-prompt). */
    private val _sessionCount = MutableStateFlow(0)
    val sessionCount: StateFlow<Int> = _sessionCount.asStateFlow()

    init {
        user.value?.uid?.let { fetchUserProfile(it) }
        loadSessionCount()
    }

    private fun fetchUserProfile(userId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val document = firestore.collection("users").document(userId).get().await()
                if (document.exists()) {
                    val res = (document.getLong("profilePicRes") ?: R.drawable.avatar1.toLong()).toInt()
                    _profilePicRes.emit(res)
                }
            } catch (e: Exception) {
                println("Failed to fetch user profile: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun updateUserAvatar(newAvatarRes: Int) {
        val userId = user.value?.uid ?: return
        _avatarUpdateComplete.value = false
        viewModelScope.launch {
            _isLoading.value = true
            try {
                firestore.collection("users").document(userId)
                    .update("profilePicRes", newAvatarRes)
                    .await()
                _profilePicRes.emit(newAvatarRes)
                // Signal navigation only after Firestore confirms the write.
                _avatarUpdateComplete.value = true
            } catch (e: Exception) {
                println("Failed to update avatar: ${e.localizedMessage}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    /** Call after the UI has consumed the avatarUpdateComplete signal to reset it. */
    fun consumeAvatarUpdateComplete() {
        _avatarUpdateComplete.value = false
    }

    // ── Phase 5: Notification preferences ────────────────────────────────────

    /**
     * Whether the daily check-in reminder is enabled.
     * Emits true by default (new installs start with check-ins active).
     */
    val dailyCheckInEnabled: StateFlow<Boolean> =
        userPreferencesRepository.dailyCheckInEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /**
     * Whether the weekly emotional insight summary is enabled.
     * Emits true by default.
     */
    val weeklyInsightsEnabled: StateFlow<Boolean> =
        userPreferencesRepository.weeklyInsightsEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    /**
     * Whether personalised smart nudges are enabled.
     * Default false — the feature is opt-in, auto-prompted after 4 sessions.
     */
    val smartNudgesEnabled: StateFlow<Boolean> =
        userPreferencesRepository.smartNudgesEnabled
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * The user's preferred check-in clock time as "HH:mm" (24-h).
     * Defaults to [UserPreferencesRepository.DEFAULT_CHECKIN_TIME] ("20:00").
     */
    val preferredCheckInTime: StateFlow<String> =
        userPreferencesRepository.preferredCheckInTime
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                UserPreferencesRepository.DEFAULT_CHECKIN_TIME
            )

    // ── Phase 5: Smart-nudge auto-prompt ─────────────────────────────────────

    /** Whether the one-shot nudge auto-prompt has already been shown. */
    val hasShownNudgePrompt: StateFlow<Boolean> =
        userPreferencesRepository.hasShownNudgePrompt
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /**
     * True when all three conditions are met:
     *  1. The user has accumulated 4+ sessions (sufficient data for accuracy).
     *  2. Smart nudges are currently **OFF** (no need to prompt if already on).
     *  3. The prompt has **not** been shown before (one-shot).
     *
     * The UI collects this flow and shows an [AlertDialog] when it emits true.
     */
    val shouldShowNudgePrompt: StateFlow<Boolean> =
        MutableStateFlow(false).also { flow ->
            viewModelScope.launch {
                // Recalculate whenever sessionCount, smartNudgesEnabled, or hasShownNudgePrompt changes.
                combine(
                    _sessionCount,
                    smartNudgesEnabled,
                    hasShownNudgePrompt
                ) { count, nudgesOn, alreadyShown ->
                    count >= 4 && !nudgesOn && !alreadyShown
                }.collect { flow.value = it }
            }
        }.asStateFlow()

    private fun loadSessionCount() {
        viewModelScope.launch {
            _sessionCount.value = memoryManager.getSessionCount()
        }
    }

    // ── Phase 5: Preference setters ───────────────────────────────────────────

    /**
     * Persists the daily check-in preference and either schedules or cancels
     * [CheckInWorker] atomically in the same coroutine.
     *
     * @param enabled New state of the toggle.
     * @param context Needed to interact with WorkManager.
     * @param checkInTime Current preferred time string (used when re-scheduling).
     */
    fun setDailyCheckInEnabled(enabled: Boolean, context: Context, checkInTime: String) {
        viewModelScope.launch {
            userPreferencesRepository.setDailyCheckInEnabled(enabled)
            if (enabled) {
                CheckInWorker.schedule(context, checkInTime)
            } else {
                CheckInWorker.cancel(context)
            }
        }
    }

    /**
     * Persists the weekly insights preference and either schedules or cancels
     * [WeeklyInsightWorker] atomically.
     */
    fun setWeeklyInsightsEnabled(enabled: Boolean, context: Context) {
        viewModelScope.launch {
            userPreferencesRepository.setWeeklyInsightsEnabled(enabled)
            if (enabled) {
                WeeklyInsightWorker.schedule(context)
            } else {
                WeeklyInsightWorker.cancel(context)
            }
        }
    }

    /** Persists the smart-nudges preference (no WorkManager side-effect needed). */
    fun setSmartNudgesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesRepository.setSmartNudgesEnabled(enabled)
        }
    }

    /**
     * Persists the new preferred check-in time and immediately re-schedules
     * [CheckInWorker] at the updated time (if daily check-ins are enabled).
     *
     * @param time     New time string in "HH:mm" format.
     * @param context  Needed by WorkManager.
     */
    fun setPreferredCheckInTime(time: String, context: Context) {
        viewModelScope.launch {
            userPreferencesRepository.setPreferredCheckInTime(time)
            if (dailyCheckInEnabled.value) {
                CheckInWorker.schedule(context, time)
            }
        }
    }

    /** Marks the smart-nudge auto-prompt as permanently shown. */
    fun markNudgePromptShown() {
        viewModelScope.launch {
            userPreferencesRepository.markNudgePromptShown()
        }
    }
}
