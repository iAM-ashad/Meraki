package com.iamashad.meraki.screens.home

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.iamashad.meraki.repository.AdviceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
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
}
