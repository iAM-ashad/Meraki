package com.iamashad.meraki.screens.register

import android.content.Intent
import androidx.lifecycle.ViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val googleSignInClient: GoogleSignInClient
) : ViewModel() {

    private val _user = MutableStateFlow<FirebaseUser?>(firebaseAuth.currentUser)
    val user: StateFlow<FirebaseUser?> = _user

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    // Get Google Sign-In Intent
    fun getGoogleSignInIntent(): Intent {
        return googleSignInClient.signInIntent
    }

    // Firebase Sign-In with Google Account
    fun firebaseAuthWithGoogle(account: GoogleSignInAccount?, onComplete: (Boolean) -> Unit) {
        if (account == null) {
            _errorMessage.value = "Google Sign-In failed."
            onComplete(false)
            return
        }

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        firebaseAuth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    _user.value = firebaseAuth.currentUser
                    onComplete(true)
                } else {
                    _errorMessage.value = task.exception?.localizedMessage ?: "Authentication failed."
                    onComplete(false)
                }
            }
    }

    // Logout
    fun logout() {
        firebaseAuth.signOut()
        googleSignInClient.signOut()
        _user.value = null
    }
}

