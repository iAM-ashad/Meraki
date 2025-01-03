package com.iamashad.meraki.screens.home

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.repository.AdviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeScreenViewModel @Inject constructor(
    private val adviceRepository: AdviceRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _advice = MutableLiveData<String>()
    val advice: LiveData<String> get() = _advice

    // User data in StateFlow
    private val _user = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    val user: StateFlow<FirebaseUser?> get() = _user

    private val _photoUrl = MutableStateFlow(firebaseAuth.currentUser?.photoUrl)
    val photoUrl: StateFlow<Uri?> = _photoUrl

    init {
        fetchAdvice()
    }

    private fun fetchAdvice() {
        viewModelScope.launch {
            try {
                val response = adviceRepository.getAdvice()
                _advice.value = response.slip.advice
            } catch (e: Exception) {
                _advice.value = "Failed to load advice"
            }
        }
    }

    // Logout functionality
    fun logout() {
        firebaseAuth.signOut()
        _user.value = null // Update the user state after logging out
    }
    suspend fun logDailyUsage(userId: String) {
        val firestore = FirebaseFirestore.getInstance()
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Check if today's usage is already logged
        val snapshot = firestore.collection("users")
            .document(userId)
            .collection("streakLogs")
            .whereEqualTo("date", today)
            .get()
            .await()

        if (snapshot.isEmpty) {
            // Log today's usage
            firestore.collection("users")
                .document(userId)
                .collection("streakLogs")
                .add(mapOf("date" to today))
        }
    }
    suspend fun calculateStreak(userId: String): Int {
        val firestore = FirebaseFirestore.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = sdf.parse(sdf.format(Date()))
        var streak = 0

        // Fetch all streak logs
        val logs = firestore.collection("users")
            .document(userId)
            .collection("streakLogs")
            .get()
            .await()
            .documents
            .mapNotNull { sdf.parse(it.getString("date") ?: "") }
            .sortedDescending()

        // Calculate streak
        var currentStreakDate = today
        logs.forEach { logDate ->
            if (sdf.format(logDate) == sdf.format(currentStreakDate)) {
                streak++
                currentStreakDate = Calendar.getInstance().apply {
                    time = currentStreakDate
                    add(Calendar.DAY_OF_YEAR, -1)
                }.time
            } else {
                return@forEach
            }
        }
        return streak
    }
}
