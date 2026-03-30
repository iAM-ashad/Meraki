package com.iamashad.meraki.integration

import app.cash.turbine.test
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.rules.MainDispatcherRule
import com.iamashad.meraki.screens.register.AuthUiEvent
import com.iamashad.meraki.screens.register.RegisterViewModel
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Phase 5 – Golden Path 1: "The New User"
 *
 * Integration scenario tested end-to-end through RegisterViewModel:
 *
 *   1. App opens with no authenticated user  (uiState.currentUser == null)
 *   2. User fills in credentials and submits registration
 *   3. Firebase Auth creates the account successfully
 *   4. Display-name profile update succeeds
 *   5. Firestore user document is saved
 *   6. ViewModel emits ShowToast("Account created successfully!") + NavigateToHome
 *
 * All Firebase operations are synchronously-dispatched mocks so the entire
 * chain runs within the [runTest] coroutine.  [MainDispatcherRule] installs
 * [UnconfinedTestDispatcher] so viewModelScope coroutines run eagerly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NewUserFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ── fakes ─────────────────────────────────────────────────────────────────

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var mockUser: FirebaseUser
    private lateinit var usersCollection: CollectionReference
    private lateinit var userDocRef: DocumentReference
    private lateinit var viewModel: RegisterViewModel

    @Before
    fun setUp() {
        mockUser = mockk(relaxed = true) {
            every { uid } returns "uid-new-user"
            every { email } returns "alice@example.com"
        }

        firebaseAuth = mockk(relaxed = true) {
            every { currentUser } returns null   // not signed-in at startup
        }

        firestore = mockk(relaxed = true)
        usersCollection = mockk(relaxed = true)
        userDocRef = mockk(relaxed = true)
        every { firestore.collection("users") } returns usersCollection

        viewModel = RegisterViewModel(firebaseAuth, firestore)
    }

    // ── helper: wire up a successful Firebase Auth registration ────────────────

    private fun stubSuccessfulRegistration() {
        // 1) createUserWithEmailAndPassword → success task
        val authResultTask = mockk<Task<AuthResult>>(relaxed = true)
        val authListenerSlot = slot<OnCompleteListener<AuthResult>>()
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns authResultTask
        every { authResultTask.addOnCompleteListener(capture(authListenerSlot)) } answers {
            val completedAuthTask = mockk<Task<AuthResult>>(relaxed = true) {
                every { isSuccessful } returns true
            }
            authListenerSlot.captured.onComplete(completedAuthTask)
            authResultTask
        }

        // 2) auth.currentUser now returns the new user
        every { firebaseAuth.currentUser } returns mockUser

        // 3) updateProfile → success
        val profileTask = mockk<Task<Void>>(relaxed = true)
        val profileSlot = slot<OnCompleteListener<Void>>()
        every { mockUser.updateProfile(any<UserProfileChangeRequest>()) } returns profileTask
        every { profileTask.addOnCompleteListener(capture(profileSlot)) } answers {
            val completedProfile = mockk<Task<Void>>(relaxed = true) {
                every { isSuccessful } returns true
            }
            profileSlot.captured.onComplete(completedProfile)
            profileTask
        }

        // 4) firestore.collection("users").document(uid).set(...) → success
        every { usersCollection.document("uid-new-user") } returns userDocRef
        val setTask = mockk<Task<Void>>(relaxed = true)
        val setSuccessSlot = slot<OnSuccessListener<Void>>()
        every { userDocRef.set(any<Map<String, Any>>()) } returns setTask
        every { setTask.addOnSuccessListener(capture(setSuccessSlot)) } answers {
            setSuccessSlot.captured.onSuccess(null)
            setTask
        }
        every { setTask.addOnFailureListener(any<OnFailureListener>()) } returns setTask
    }

    // ── tests ─────────────────────────────────────────────────────────────────

    @Test
    fun `step 1 - app opens unauthenticated - currentUser is null`() = runTest {
        assertThat(viewModel.uiState.value.currentUser).isNull()
        assertThat(viewModel.uiState.value.isLoading).isFalse()
        assertThat(viewModel.uiState.value.errorMessage).isNull()
    }

    @Test
    fun `step 2 - registerUser sets isLoading true before Firebase responds`() = runTest {
        // Fire the real call without resolving the auth task (no stub → the mock
        // relaxed default never invokes the listener, so isLoading stays true).
        val hangingTask = mockk<Task<AuthResult>>(relaxed = true)
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns hangingTask

        viewModel.registerUser("alice@example.com", "Pass123!", "Alice", null)

        assertThat(viewModel.uiState.value.isLoading).isTrue()
    }

    @Test
    fun `full golden path - register succeeds - emits ShowToast then NavigateToHome`() =
        runTest {
            stubSuccessfulRegistration()

            viewModel.events.test {
                viewModel.registerUser("alice@example.com", "Pass123!", "Alice", null)

                val first = awaitItem()
                assertThat(first).isInstanceOf(AuthUiEvent.ShowToast::class.java)
                assertThat((first as AuthUiEvent.ShowToast).message)
                    .isEqualTo("Account created successfully!")

                val second = awaitItem()
                assertThat(second).isEqualTo(AuthUiEvent.NavigateToHome)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `full golden path - register succeeds - uiState reflects new user and no loading`() =
        runTest {
            stubSuccessfulRegistration()

            viewModel.registerUser("alice@example.com", "Pass123!", "Alice", null)

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.currentUser).isNotNull()
            assertThat(state.errorMessage).isNull()
        }

    @Test
    fun `registration failure - errorMessage is set and isLoading is false`() = runTest {
        val authTask = mockk<Task<AuthResult>>(relaxed = true)
        val listenerSlot = slot<OnCompleteListener<AuthResult>>()
        every { firebaseAuth.createUserWithEmailAndPassword(any(), any()) } returns authTask
        every { authTask.addOnCompleteListener(capture(listenerSlot)) } answers {
            val failedTask = mockk<Task<AuthResult>>(relaxed = true) {
                every { isSuccessful } returns false
                every { exception } returns Exception("Email already in use")
            }
            listenerSlot.captured.onComplete(failedTask)
            authTask
        }

        viewModel.registerUser("alice@example.com", "Pass123!", "Alice", null)

        val state = viewModel.uiState.value
        assertThat(state.isLoading).isFalse()
        assertThat(state.errorMessage).isEqualTo("Email already in use")
    }
}
