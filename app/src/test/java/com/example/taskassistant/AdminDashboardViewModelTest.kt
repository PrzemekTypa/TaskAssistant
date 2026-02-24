package com.example.taskassistant.ui.dashboard

import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AdminDashboardViewModelTest {

    private val mockAuth: FirebaseAuth = mockk(relaxed = true)
    private val mockDb: FirebaseFirestore = mockk(relaxed = true)
    private val mockCollection: CollectionReference = mockk(relaxed = true)
    private val mockTask: Task<DocumentReference> = mockk(relaxed = true)

    private lateinit var viewModel: AdminDashboardViewModel
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = AdminDashboardViewModel(auth = mockAuth, db = mockDb)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun addTaskShouldSetSuccessMessageOnSuccessfulSave() = runTest {
        // 1. Arrange (Przygotowanie)
        val title = "Wyniesienie śmieci"
        val points = 50
        val childId = "child123"
        val childEmail = "dziecko@test.pl"

        every { mockDb.collection("tasks") } returns mockCollection
        every { mockCollection.add(any()) } returns mockTask

        val successSlot = slot<OnSuccessListener<in DocumentReference>>()
        every { mockTask.addOnSuccessListener(capture(successSlot)) } answers {
            successSlot.captured.onSuccess(mockk())
            mockTask
        }
        every { mockTask.addOnFailureListener(any()) } returns mockTask

        // 2. Act (Działanie)
        viewModel.addTask(title, points, childId, childEmail)
        testDispatcher.scheduler.advanceUntilIdle()

        // 3. Assert (Weryfikacja)
        val state = viewModel.uiState.value
        assertEquals("Zadanie dodane!", state.successMessage)
    }
}