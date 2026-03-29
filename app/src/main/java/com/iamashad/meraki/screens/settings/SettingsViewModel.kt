package com.iamashad.meraki.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _user = MutableStateFlow(firebaseAuth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user.asStateFlow()

    private val _profilePicRes = MutableStateFlow(R.drawable.avatar1) // Default avatar
    val profilePicRes: StateFlow<Int> = _profilePicRes.asStateFlow()

    init {
        user.value?.uid?.let { fetchUserProfile(it) }
    }

    private fun fetchUserProfile(userId: String) {
        viewModelScope.launch {
            try {
                val document = firestore.collection("users").document(userId).get().await()
                if (document.exists()) {
                    val profilePicRes = (document.getLong("profilePicRes") ?: R.drawable.avatar1.toLong()).toInt()
                    _profilePicRes.emit(profilePicRes)
                }
            } catch (e: Exception) {
                println("Failed to fetch user profile: ${e.localizedMessage}")
            }
        }
    }

    fun updateUserAvatar(newAvatarRes: Int) {
        val userId = user.value?.uid ?: return

        viewModelScope.launch {
            try {
                firestore.collection("users").document(userId)
                    .update("profilePicRes", newAvatarRes)
                    .await()

                _profilePicRes.emit(newAvatarRes) // Update UI instantly

                // 🔥 Fetch user profile again to ensure the change is reflected globally
                fetchUserProfile(userId)
            } catch (e: Exception) {
                println("Failed to update avatar: ${e.localizedMessage}")
            }
        }
    }

}
