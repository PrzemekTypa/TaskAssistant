package com.example.taskassistant.ui.dashboard

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class ChildUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val userPoints: Int = 0
)

class ChildDashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChildUiState())
    val uiState: StateFlow<ChildUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = auth.currentUser?.uid

    init {
        fetchMyTasks()
    }

    private fun fetchMyTasks() {
        if (currentUserId == null) return

        _uiState.update { it.copy(isLoading = true) }


        db.collection("tasks")
            .whereEqualTo("assignedToId", currentUserId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    _uiState.update { it.copy(error = error.message, isLoading = false) }
                    return@addSnapshotListener
                }

                val tasksList = value?.documents?.map { doc ->
                    Task(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        points = doc.getLong("points")?.toInt() ?: 0,
                        status = doc.getString("status") ?: "todo",
                        assignedToId = doc.getString("assignedToId") ?: "",
                        assignedToEmail = doc.getString("assignedToEmail") ?: ""
                    )
                } ?: emptyList()


                val totalPoints = tasksList.filter { it.status == "approved" }.sumOf { it.points }

                _uiState.update {
                    it.copy(
                        tasks = tasksList,
                        userPoints = totalPoints,
                        isLoading = false
                    )
                }
            }
    }

    fun markTaskAsDone(taskId: String) {
        db.collection("tasks").document(taskId)
            .update("status", "pending")
            .addOnFailureListener { e ->
                _uiState.update { it.copy(error = "Błąd: ${e.message}") }
            }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(error = null) }
    }
}