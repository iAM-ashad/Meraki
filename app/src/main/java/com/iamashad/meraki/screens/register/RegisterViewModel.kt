package com.iamashad.meraki.screens.register

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _user = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    fun registerUser(email: String, password: String, name: String, profilePicRes: Int?, onComplete: (Boolean, String?) -> Unit) {
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
                            _user.value = firebaseAuth.currentUser

                            // Store user details in Firestore
                            val userData = mapOf(
                                "uid" to userId,
                                "name" to name,
                                "email" to email,
                                "profilePicRes" to (profilePicRes ?: DEFAULT_AVATAR)
                            )

                            firestore.collection("users").document(userId)
                                .set(userData)
                                .addOnSuccessListener {
                                    onComplete(true, null)
                                }
                                .addOnFailureListener { e ->
                                    _errorMessage.value = e.localizedMessage
                                    onComplete(false, _errorMessage.value)
                                }
                        } else {
                            _errorMessage.value = profileTask.exception?.message
                            onComplete(false, _errorMessage.value)
                        }
                    }
                } else {
                    _errorMessage.value = task.exception?.message
                    onComplete(false, _errorMessage.value)
                }
            }
    }

    fun loginUser(email: String, password: String, onComplete: (Boolean, String?, String?, Int?) -> Unit) {
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
                                val profilePicRes = (document.getLong("profilePicRes") ?: DEFAULT_AVATAR.toLong()).toInt()
                                _user.value = user
                                onComplete(true, name, email, profilePicRes)
                            } else {
                                onComplete(false, null, null, null)
                            }
                        }
                        .addOnFailureListener {
                            _errorMessage.value = it.localizedMessage
                            onComplete(false, null, null, null)
                        }
                } else {
                    _errorMessage.value = task.exception?.message
                    onComplete(false, null, null, null)
                }
            }
    }

    fun resetPassword(email: String, onComplete: (Boolean, String?) -> Unit) {
        firebaseAuth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    onComplete(true, "Password reset email sent.")
                } else {
                    _errorMessage.value = task.exception?.localizedMessage ?: "Failed to send reset email."
                    onComplete(false, _errorMessage.value)
                }
            }
    }

    /**
     * Delete the current user's account.
     */
    fun deleteUserAccount(password: String, onComplete: (Boolean, String?) -> Unit) {
        val user = firebaseAuth.currentUser ?: return onComplete(false, "User not found")
        val email = user.email ?: return onComplete(false, "No email associated with this account")

        val credential = EmailAuthProvider.getCredential(email, password)
        user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
            if (reauthTask.isSuccessful) {
                val userId = user.uid

                // Delete user data from Firestore
                firestore.collection("users").document(userId).delete().addOnCompleteListener { deleteTask ->
                    if (deleteTask.isSuccessful) {
                        // Delete Firebase Auth user
                        user.delete().addOnCompleteListener { accountDeleteTask ->
                            if (accountDeleteTask.isSuccessful) {
                                _user.value = null
                                onComplete(true, null)
                            } else {
                                onComplete(false, "Failed to delete account: ${accountDeleteTask.exception?.message}")
                            }
                        }
                    } else {
                        onComplete(false, "Failed to delete user data: ${deleteTask.exception?.message}")
                    }
                }
            } else {
                onComplete(false, "Reauthentication failed: ${reauthTask.exception?.message}")
            }
        }
    }

    companion object {
        val DEFAULT_AVATAR = R.drawable.avatar1
    }
}
