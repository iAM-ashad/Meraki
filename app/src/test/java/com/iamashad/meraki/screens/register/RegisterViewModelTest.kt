package com.iamashad.meraki.screens.register

import app.cash.turbine.test
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.common.truth.Truth.assertThat
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.iamashad.meraki.rules.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Unit tests for [RegisterViewModel] verifying:
 * - [AuthUiState.isLoading] toggling during auth operations
 * - [AuthUiEvent] emissions (ShowToast, NavigateToHome, NavigateToLogin) via the Channel
 * - [AuthUiState.errorMessage] set on failures
 * - [clearToastMessage] resetting toast state
 *
 * Firebase's callback-based API is mocked using MockK slots that capture
 * [OnCompleteListener] / [OnSuccessListener] lambdas and invoke them
 * synchronously within the mock answer body. Combined with
 * [UnconfinedTestDispatcher] (via [MainDispatcherRule]), this means the
 * entire async callback chain runs on the current thread before returning.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var viewModel: RegisterViewModel

    // Reusable Firestore mock chain components
    private lateinit var usersCollection: CollectionReference
    private lateinit var userDocRef: DocumentReference

    @Before
    fun setUp() {
        firebaseAuth = mockk(relaxed = true) {
            every { currentUser } returns null
        }
        firestore = mockk(relaxed = true)
        usersCollection = mockk(relaxed = true)
        userDocRef = mockk(relaxed = true)

        every { firestore.collection("users") } returns usersCollection

        viewModel = RegisterViewModel(firebaseAuth, firestore)
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    /**
     * Stubs [FirebaseAuth.signInWithEmailAndPassword] so that its
     * [OnCompleteListener] is captured and immediately invoked with a task
     * whose [Task.isSuccessful] is [success].
     */
    private fun stubAuthSignIn(
        success: Boolean,
        exception: Exception? = null
    ): Task<AuthResult> {
        val authTask = mockk<Task<AuthResult>>(relaxed = true)
        val listenerSlot = slot<OnCompleteListener<AuthResult>>()

        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns authTask
        every { authTask.addOnCompleteListener(capture(listenerSlot)) } answers {
            val completedTask = mockk<Task<AuthResult>>(relaxed = true) {
                every { isSuccessful } returns success
                every { this@mockk.exception } returns exception
            }
            listenerSlot.captured.onComplete(completedTask)
            authTask
        }
        return authTask
    }

    /**
     * Stubs the Firestore `collection("users").document(uid).get()` chain
     * to invoke the [OnSuccessListener] immediately with a document
     * that [DocumentSnapshot.exists] = true and name = [userName].
     */
    private fun stubFirestoreUserDoc(uid: String, userName: String) {
        val getTask = mockk<Task<DocumentSnapshot>>(relaxed = true)
        val successSlot = slot<OnSuccessListener<DocumentSnapshot>>()
        val mockDoc = mockk<DocumentSnapshot>(relaxed = true) {
            every { exists() } returns true
            every { getString("name") } returns userName
        }

        every { usersCollection.document(uid) } returns userDocRef
        every { userDocRef.get() } returns getTask
        every { getTask.addOnSuccessListener(capture(successSlot)) } answers {
            successSlot.captured.onSuccess(mockDoc)
            getTask
        }
        every { getTask.addOnFailureListener(any<OnFailureListener>()) } returns getTask
    }

    /**
     * Stubs the Firestore user document `.get()` to invoke the
     * [OnFailureListener] with [exception].
     */
    private fun stubFirestoreUserDocFails(uid: String, exception: Exception) {
        val getTask = mockk<Task<DocumentSnapshot>>(relaxed = true)
        val failSlot = slot<OnFailureListener>()

        every { usersCollection.document(uid) } returns userDocRef
        every { userDocRef.get() } returns getTask
        every { getTask.addOnSuccessListener(any<OnSuccessListener<DocumentSnapshot>>()) } returns getTask
        every { getTask.addOnFailureListener(capture(failSlot)) } answers {
            failSlot.captured.onFailure(exception)
            getTask
        }
    }

    // ── loginUser — loading state ─────────────────────────────────────────────

    @Test
    fun `loginUser - sets isLoading true immediately before callbacks fire`() = runTest {
        // Use a task whose addOnCompleteListener is never invoked (we capture but don't invoke)
        val authTask = mockk<Task<AuthResult>>(relaxed = true)
        val slot = slot<OnCompleteListener<AuthResult>>()
        every { firebaseAuth.signInWithEmailAndPassword(any(), any()) } returns authTask
        every { authTask.addOnCompleteListener(capture(slot)) } returns authTask

        viewModel.uiState.test {
            awaitItem() // initial (isLoading=false)
            viewModel.loginUser("test@test.com", "pass123")
            // isLoading set synchronously before any callback
            assertThat(awaitItem().isLoading).isTrue()
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ── loginUser — success path ──────────────────────────────────────────────

    @Test
    fun `loginUser success - emits ShowToast then NavigateToHome events`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val mockUser = mockk<FirebaseUser>(relaxed = true) { every { uid } returns "uid-1" }
            every { firebaseAuth.currentUser } returns mockUser
            stubAuthSignIn(success = true)
            stubFirestoreUserDoc(uid = "uid-1", userName = "Alice")

            viewModel.events.test {
                viewModel.loginUser("alice@test.com", "pass")

                val toast = awaitItem() as AuthUiEvent.ShowToast
                assertThat(toast.message).contains("Welcome back")

                val navigate = awaitItem()
                assertThat(navigate).isEqualTo(AuthUiEvent.NavigateToHome)

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `loginUser success - sets isLoading false after callbacks complete`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val mockUser = mockk<FirebaseUser>(relaxed = true) { every { uid } returns "uid-1" }
            every { firebaseAuth.currentUser } returns mockUser
            stubAuthSignIn(success = true)
            stubFirestoreUserDoc(uid = "uid-1", userName = "Alice")

            viewModel.loginUser("alice@test.com", "pass")

            assertThat(viewModel.uiState.value.isLoading).isFalse()
        }

    @Test
    fun `loginUser success - updates currentUser in state`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val mockUser = mockk<FirebaseUser>(relaxed = true) { every { uid } returns "uid-1" }
            every { firebaseAuth.currentUser } returns mockUser
            stubAuthSignIn(success = true)
            stubFirestoreUserDoc(uid = "uid-1", userName = "Bob")

            viewModel.loginUser("bob@test.com", "pass")

            assertThat(viewModel.uiState.value.currentUser).isEqualTo(mockUser)
        }

    // ── loginUser — failure paths ─────────────────────────────────────────────

    @Test
    fun `loginUser auth failure - emits ShowToast event`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubAuthSignIn(success = false, exception = Exception("Wrong credentials"))

            viewModel.events.test {
                viewModel.loginUser("bad@test.com", "wrong")

                val toast = awaitItem() as AuthUiEvent.ShowToast
                assertThat(toast.message).isEqualTo("Invalid email or password")

                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `loginUser auth failure - sets errorMessage and isLoading false`() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubAuthSignIn(success = false, exception = Exception("Auth error"))

            viewModel.loginUser("bad@test.com", "wrong")

            val state = viewModel.uiState.value
            assertThat(state.isLoading).isFalse()
            assertThat(state.errorMessage).isEqualTo("Auth error")
        }

    @Test
    fun `loginUser firestore failure - emits ShowToast with invalid credentials message`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val mockUser = mockk<FirebaseUser>(relaxed = true) { every { uid } returns "uid-2" }
            every { firebaseAuth.currentUser } returns mockUser
            stubAuthSignIn(success = true)
            stubFirestoreUserDocFails(uid = "uid-2", exception = Exception("Network error"))

            viewModel.events.test {
                viewModel.loginUser("test@test.com", "pass")

                val toast = awaitItem() as AuthUiEvent.ShowToast
                assertThat(toast.message).isEqualTo("Invalid email or password")

                cancelAndIgnoreRemainingEvents()
            }
        }

    // ── clearToastMessage ────────────────────────────────────────────────────

    @Test
    fun `clearToastMessage - nullifies the toastMessage in state`() = runTest {
        // Seed a toastMessage via resetPassword (simpler 1-level callback)
        val resetTask = mockk<Task<Void>>(relaxed = true)
        val resetSlot = slot<OnCompleteListener<Void>>()
        every { firebaseAuth.sendPasswordResetEmail(any()) } returns resetTask
        every { resetTask.addOnCompleteListener(capture(resetSlot)) } answers {
            val completedTask = mockk<Task<Void>>(relaxed = true) {
                every { isSuccessful } returns true
            }
            resetSlot.captured.onComplete(completedTask)
            resetTask
        }

        viewModel.resetPassword("user@test.com")
        assertThat(viewModel.uiState.value.toastMessage).isNotNull()

        viewModel.clearToastMessage()
        assertThat(viewModel.uiState.value.toastMessage).isNull()
    }

    // ── resetPassword ────────────────────────────────────────────────────────

    @Test
    fun `resetPassword success - sets toastMessage with confirmation`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val resetTask = mockk<Task<Void>>(relaxed = true)
            val resetSlot = slot<OnCompleteListener<Void>>()
            every { firebaseAuth.sendPasswordResetEmail(any()) } returns resetTask
            every { resetTask.addOnCompleteListener(capture(resetSlot)) } answers {
                val task = mockk<Task<Void>>(relaxed = true) {
                    every { isSuccessful } returns true
                }
                resetSlot.captured.onComplete(task)
                resetTask
            }

            viewModel.resetPassword("user@test.com")

            assertThat(viewModel.uiState.value.toastMessage)
                .isEqualTo("Password reset email sent.")
        }

    @Test
    fun `resetPassword failure - sets toastMessage with error description`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val resetTask = mockk<Task<Void>>(relaxed = true)
            val resetSlot = slot<OnCompleteListener<Void>>()
            every { firebaseAuth.sendPasswordResetEmail(any()) } returns resetTask
            every { resetTask.addOnCompleteListener(capture(resetSlot)) } answers {
                val task = mockk<Task<Void>>(relaxed = true) {
                    every { isSuccessful } returns false
                    every { exception?.localizedMessage } returns "No user found"
                }
                resetSlot.captured.onComplete(task)
                resetTask
            }

            viewModel.resetPassword("unknown@test.com")

            assertThat(viewModel.uiState.value.toastMessage).isNotNull()
        }

    // ── deleteUserAccount ─────────────────────────────────────────────────────

    @Test
    fun `deleteUserAccount - emits ShowToast when no current user`() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { firebaseAuth.currentUser } returns null

            viewModel.events.test {
                viewModel.deleteUserAccount("any-pass")

                val toast = awaitItem() as AuthUiEvent.ShowToast
                assertThat(toast.message).isEqualTo("User not found")

                cancelAndIgnoreRemainingEvents()
            }
        }
}
