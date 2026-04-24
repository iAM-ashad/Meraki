package com.iamashad.meraki.screens.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Phase 2: UDF — single immutable state class replacing two separate StateFlow properties.
data class AuthUiState(
    val currentUser: FirebaseUser? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // One-shot toast message for operations that don't trigger navigation (e.g. resetPassword).
    // Cleared by the screen after display via clearToastMessage().
    val toastMessage: String? = null
)

// Phase 6: One-time UI events replacing onComplete callbacks in registerUser, loginUser,
// and deleteUserAccount. Exposed via Channel.BUFFERED so events aren't dropped before
// the screen's LaunchedEffect subscribes.
sealed interface AuthUiEvent {
    data object NavigateToHome : AuthUiEvent
    data object NavigateToLogin : AuthUiEvent
    data class ShowToast(val message: String) : AuthUiEvent
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState(currentUser = firebaseAuth.currentUser))
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // Phase 6: Buffered channel for one-time navigation/toast events.
    private val _events = Channel<AuthUiEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    // Clears the one-shot toast after the screen has consumed it
    fun clearToastMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    // Phase 6: onComplete callback removed — success/failure emitted as AuthUiEvent.
    fun registerUser(
        email: String,
        password: String,
        name: String,
        profilePicRes: Int?
    ) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        firebaseAuth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    val userId = user?.uid ?: return@addOnCompleteListener

                    val profileUpdates = userProfileChangeRequest {
                        displayName = name
                    }

                    user.updateProfile(profileUpdates).addOnCompleteListener { profileTask ->
                        if (profileTask.isSuccessful) {
                            val userData = mapOf(
                                "uid" to userId,
                                "name" to name,
                                "email" to email,
                                "profilePicRes" to (profilePicRes ?: DEFAULT_AVATAR)
                            )

                            firestore.collection("users").document(userId)
                                .set(userData)
                                .addOnSuccessListener {
                                    _uiState.update {
                                        it.copy(
                                            currentUser = firebaseAuth.currentUser,
                                            isLoading = false
                                        )
                                    }
                                    viewModelScope.launch {
                                        _events.send(AuthUiEvent.ShowToast("Account created successfully!"))
                                        _events.send(AuthUiEvent.NavigateToHome)
                                    }
                                }
                                .addOnFailureListener { e ->
                                    val msg = e.localizedMessage
                                    _uiState.update { it.copy(errorMessage = msg, isLoading = false) }
                                }
                        } else {
                            val msg = profileTask.exception?.message
                            _uiState.update { it.copy(errorMessage = msg, isLoading = false) }
                        }
                    }
                } else {
                    val msg = task.exception?.message
                    _uiState.update { it.copy(errorMessage = msg, isLoading = false) }
                }
            }
    }

    // Phase 6: onComplete callback removed — success/failure emitted as AuthUiEvent.
    fun loginUser(email: String, password: String) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        firebaseAuth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = firebaseAuth.currentUser
                    val userId = user?.uid ?: return@addOnCompleteListener

                    firestore.collection("users").document(userId)
                        .get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val name = document.getString("name") ?: "Unknown"
                                _uiState.update {
                                    it.copy(currentUser = user, isLoading = false)
                                }
                                viewModelScope.launch {
                                    _events.send(AuthUiEvent.ShowToast("Welcome back, $name!"))
                                    _events.send(AuthUiEvent.NavigateToHome)
                                }
                            } else {
                                _uiState.update { it.copy(isLoading = false) }
                                viewModelScope.launch {
                                    _events.send(AuthUiEvent.ShowToast("Invalid email or password"))
                                }
                            }
                        }
                        .addOnFailureListener { e ->
                            val msg = e.localizedMessage
                            _uiState.update { it.copy(errorMessage = msg, isLoading = false) }
                            viewModelScope.launch {
                                _events.send(AuthUiEvent.ShowToast("Invalid email or password"))
                            }
                        }
                } else {
                    val msg = task.exception?.message
                    _uiState.update { it.copy(errorMessage = msg, isLoading = false) }
                    viewModelScope.launch {
                        _events.send(AuthUiEvent.ShowToast("Invalid email or password"))
                    }
                }
            }
    }

    // Phase 2: callback replaced with state-driven toastMessage for one-shot display.
    fun resetPassword(email: String) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                val message = if (task.isSuccessful) {
                    "Password reset email sent."
                } else {
                    task.exception?.localizedMessage ?: "Failed to send reset email."
                }
                _uiState.update { it.copy(toastMessage = message) }
            }
    }

    /**
     * Delete the current user's account.
     * Phase 6: onComplete callback removed — success/failure emitted as AuthUiEvent.
     * isLoading is toggled so the loading overlay in SettingsScreen covers the
     * multi-step reauthenticate → delete → navigate chain.
     */
    fun deleteUserAccount(password: String) {
        val user = firebaseAuth.currentUser ?: run {
            viewModelScope.launch { _events.send(AuthUiEvent.ShowToast("User not found")) }
            return
        }
        val email = user.email ?: run {
            viewModelScope.launch { _events.send(AuthUiEvent.ShowToast("No email associated with this account")) }
            return
        }

        // Signal loading for the full delete chain (reauth → Firestore delete → auth delete).
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                val userId = user.uid

                // Delete user data from Firestore
                firestore.collection("users").document(userId)
                    .delete()
                    .addOnCompleteListener { deleteTask ->
                        if (deleteTask.isSuccessful) {
                            // Delete Firebase Auth user
                            user.delete().addOnCompleteListener { accountDeleteTask ->
                                if (accountDeleteTask.isSuccessful) {
                                    _uiState.update { it.copy(currentUser = null, isLoading = false) }
                                    viewModelScope.launch {
                                        _events.send(AuthUiEvent.ShowToast("Account successfully deleted."))
                                        _events.send(AuthUiEvent.NavigateToLogin)
                                    }
                                } else {
                                    _uiState.update { it.copy(isLoading = false) }
                                    viewModelScope.launch {
                                        _events.send(
                                            AuthUiEvent.ShowToast(
                                                "Failed to delete account: ${accountDeleteTask.exception?.message}"
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            _uiState.update { it.copy(isLoading = false) }
                            viewModelScope.launch {
                                _events.send(
                                    AuthUiEvent.ShowToast(
                                        "Failed to delete user data: ${deleteTask.exception?.message}"
                                    )
                                )
                            }
                        }
                    }
            } else {
                _uiState.update { it.copy(isLoading = false) }
                viewModelScope.launch {
                    _events.send(
                        AuthUiEvent.ShowToast(
                            "Reauthentication failed: ${reauthTask.exception?.message}"
                        )
                    )
                }
            }
        }
    }

    companion object {
        val DEFAULT_AVATAR = R.drawable.avatar1
    }
}
