package com.example.taskassistant.ui.login

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoginViewModelTest {

    private val mockAuth: FirebaseAuth = mockk(relaxed = true)
    private val mockDb: FirebaseFirestore = mockk(relaxed = true)
    private val mockTask: Task<AuthResult> = mockk(relaxed = true)

    private lateinit var viewModel: LoginViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = LoginViewModel(auth = mockAuth, db = mockDb)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loginShouldSetErrorWhenFirebaseFails() = runTest {
        // 1. Arrange (Przygotowanie)
        viewModel.onEmailChange("test@test.pl")
        viewModel.onPasswordChange("wrong")

        every { mockAuth.signInWithEmailAndPassword(any(), any()) } returns mockTask
        every { mockTask.isSuccessful } returns false
        every { mockTask.exception } returns Exception("Złe hasło")

        val slot = slot<OnCompleteListener<AuthResult>>()
        every { mockTask.addOnCompleteListener(capture(slot)) } returns mockTask

        // 2. Act (Działanie)
        viewModel.login()
        testDispatcher.scheduler.advanceUntilIdle()

        if (slot.isCaptured) {
            slot.captured.onComplete(mockTask)
        }

        // 3. Assert (Weryfikacja)
        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertFalse(state.isLoginSuccessful)
        assertEquals("Złe hasło", state.error)
    }
}