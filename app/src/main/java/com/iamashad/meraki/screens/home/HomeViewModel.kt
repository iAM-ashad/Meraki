package com.iamashad.meraki.screens.home

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.repository.AdviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore // Injected Firestore instance
) : ViewModel() {

    private val _advice = MutableStateFlow("Loading advice...")
    val advice: StateFlow<String> = _advice.asStateFlow()

    private val _user = MutableStateFlow(firebaseAuth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _photoUrl = MutableStateFlow(firebaseAuth.currentUser?.photoUrl)
    val photoUrl: StateFlow<Uri?> = _photoUrl.asStateFlow()

    private val dateFormat =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    init {
        fetchAdvice()
    }

    private fun fetchAdvice() {
        viewModelScope.launch {
            try {
                val response = adviceRepository.getAdvice()
                _advice.emit(response.slip.advice)
            } catch (e: Exception) {
                _advice.emit("Failed to load advice: ${e.localizedMessage}")
            }
        }
    }

    suspend fun logDailyUsage(userId: String) {
        try {
            val today = dateFormat.format(Date())
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("streakLogs")
                .whereEqualTo("date", today)
                .get()
                .await()

            if (snapshot.isEmpty) {
                firestore.collection("users")
                    .document(userId)
                    .collection("streakLogs")
                    .add(mapOf("date" to today))
            }
        } catch (e: Exception) {
            println("Error logging daily usage: ${e.localizedMessage}")
        }
    }

    suspend fun calculateStreak(userId: String): Int {
        return try {
            val today = dateFormat.parse(dateFormat.format(Date())) ?: return 0
            var streak = 0

            val logs = firestore.collection("users")
                .document(userId)
                .collection("streakLogs")
                .get()
                .await()
                .documents
                .mapNotNull { dateFormat.parse(it.getString("date") ?: "") }
                .sortedDescending()

            var currentStreakDate = today
            for (logDate in logs) {
                if (dateFormat.format(logDate) == dateFormat.format(currentStreakDate)) {
                    streak++
                    currentStreakDate = Calendar.getInstance().apply {
                        time = currentStreakDate
                        add(Calendar.DAY_OF_YEAR, -1)
                    }.time
                } else {
                    break
                }
            }
            streak
        } catch (e: Exception) {
            println("Error logging daily usage: ${e.localizedMessage}")
            0
        }
    }
}

