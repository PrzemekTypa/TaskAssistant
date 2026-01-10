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
    val rewards: List<Reward> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val userPoints: Int = 0,
    val successMessage: String? = null
)

class ChildDashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChildUiState())
    val uiState: StateFlow<ChildUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val currentUserId = auth.currentUser?.uid
    private var currentParentId: String = ""

    private var totalEarnedPoints = 0
    private var totalSpentPoints = 0

    init {
        fetchUserDataAndRewards()
        fetchMyTasks()
        fetchMyRedemptions()
    }

    private fun fetchUserDataAndRewards() {
        if (currentUserId == null) return

        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                val parentId = document.getString("parentId")
                if (parentId != null) {
                    currentParentId = parentId
                    fetchRewards(parentId)
                }
            }
    }

    private fun fetchRewards(parentId: String) {
        db.collection("rewards")
            .whereEqualTo("parentId", parentId)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                val rewardsList = value?.documents?.map { doc ->
                    Reward(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        cost = doc.getLong("cost")?.toInt() ?: 0,
                        parentId = doc.getString("parentId") ?: ""
                    )
                } ?: emptyList()

                _uiState.update { it.copy(rewards = rewardsList) }
            }
    }

    private fun fetchMyTasks() {
        if (currentUserId == null) return

        db.collection("tasks")
            .whereEqualTo("assignedToId", currentUserId)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

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


                totalEarnedPoints = tasksList.filter { it.status == "approved" }.sumOf { it.points }

                recalculatePoints()
                _uiState.update { it.copy(tasks = tasksList) }
            }
    }

    private fun fetchMyRedemptions() {
        if (currentUserId == null) return

        db.collection("redemptions")
            .whereEqualTo("childId", currentUserId)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                val spent = value?.documents?.sumOf { doc ->
                    doc.getLong("cost")?.toInt() ?: 0
                } ?: 0

                totalSpentPoints = spent
                recalculatePoints()
            }
    }

    private fun recalculatePoints() {
        _uiState.update {
            it.copy(userPoints = totalEarnedPoints - totalSpentPoints)
        }
    }

    fun redeemReward(reward: Reward) {
        if (currentUserId == null) return

        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                val freshParentId = document.getString("parentId")

                if (freshParentId.isNullOrEmpty()) {
                    _uiState.update { it.copy(error = "Błąd: Nie masz przypisanego rodzica! Poproś go o połączenie konta.") }
                    return@addOnSuccessListener
                }


                if (_uiState.value.userPoints < reward.cost) {
                    _uiState.update { it.copy(error = "Za mało punktów!") }
                    return@addOnSuccessListener
                }


                val redemption = Redemption(
                    childId = currentUserId,
                    parentId = freshParentId,
                    rewardTitle = reward.title,
                    cost = reward.cost,
                    status = "pending",
                    timestamp = System.currentTimeMillis()
                )

                db.collection("redemptions").add(redemption)
                    .addOnSuccessListener {
                        _uiState.update { it.copy(successMessage = "Kupiono: ${reward.title}!") }
                    }
                    .addOnFailureListener { e ->
                        _uiState.update { it.copy(error = "Błąd zakupu: ${e.message}") }
                    }
            }
            .addOnFailureListener {
                _uiState.update { it.copy(error = "Błąd połączenia z siecią") }
            }
    }

    fun markTaskAsDone(taskId: String) {
        db.collection("tasks").document(taskId)
            .update("status", "pending")
    }

    fun onMessageShown() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }
}