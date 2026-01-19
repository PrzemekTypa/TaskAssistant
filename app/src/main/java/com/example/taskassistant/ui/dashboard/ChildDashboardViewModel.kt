package com.example.taskassistant.ui.dashboard

import android.util.Log
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
        Log.d("FIREBASE_LOG", "Init ViewModelu Dziecka. UserID: $currentUserId")
        startListeningToUserAndRewards()
        startListeningToTasks()
        startListeningToRedemptions()
    }

    private fun startListeningToUserAndRewards() {
        if (currentUserId == null) return

        db.collection("users").document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("FIREBASE_LOG", "Błąd nasłuchu usera: ${error.message}")
                    return@addSnapshotListener
                }

                val parentId = snapshot?.getString("parentId")
                Log.d("FIREBASE_LOG", "Znaleziono ParentID: $parentId")

                if (!parentId.isNullOrEmpty()) {
                    currentParentId = parentId
                    startListeningToRewards(parentId)
                }
            }
    }

    private fun startListeningToRewards(parentId: String) {
        db.collection("rewards")
            .whereEqualTo("parentId", parentId)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                val rewardsList = value?.documents?.mapNotNull { doc ->
                    Reward(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        cost = doc.getLong("cost")?.toInt() ?: 0,
                        parentId = doc.getString("parentId") ?: ""
                    )
                } ?: emptyList()

                Log.d("FIREBASE_LOG", "Pobrano nagrody: ${rewardsList.size}")
                _uiState.update { it.copy(rewards = rewardsList) }
            }
    }

    private fun startListeningToTasks() {
        if (currentUserId == null) return

        db.collection("tasks")
            .whereEqualTo("assignedToId", currentUserId)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Log.e("FIREBASE_LOG", "Błąd zadań: ${error.message}")
                    return@addSnapshotListener
                }

                val tasksList = value?.documents?.mapNotNull { doc ->
                    Task(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        points = doc.getLong("points")?.toInt() ?: 0,
                        status = doc.getString("status") ?: "todo",
                        assignedToId = doc.getString("assignedToId") ?: "",
                        assignedToEmail = doc.getString("assignedToEmail") ?: ""
                    )
                } ?: emptyList()

                Log.d("FIREBASE_LOG", "Pobrano zadania: ${tasksList.size}")

                totalEarnedPoints = tasksList.filter { it.status == "approved" }.sumOf { it.points }

                recalculatePoints()
                _uiState.update { it.copy(tasks = tasksList) }
            }
    }

    private fun startListeningToRedemptions() {
        if (currentUserId == null) return

        db.collection("redemptions")
            .whereEqualTo("childId", currentUserId)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                val spent = value?.documents?.sumOf { doc ->
                    doc.getLong("cost")?.toInt() ?: 0
                } ?: 0

                Log.d("FIREBASE_LOG", "Suma wydatków: $spent")
                totalSpentPoints = spent
                recalculatePoints()
            }
    }

    private fun recalculatePoints() {
        val balance = totalEarnedPoints - totalSpentPoints
        Log.d("FIREBASE_LOG", "Przeliczam punkty: $totalEarnedPoints - $totalSpentPoints = $balance")
        _uiState.update {
            it.copy(userPoints = balance)
        }
    }

    fun redeemReward(reward: Reward) {
        val userId = currentUserId ?: return

        if (currentParentId.isEmpty()) {
            _uiState.update { it.copy(error = "Błąd: Brak połączonego rodzica!") }
            return
        }

        if (_uiState.value.userPoints < reward.cost) {
            _uiState.update { it.copy(error = "Za mało punktów!") }
            return
        }

        val redemptionData = hashMapOf(
            "childId" to userId,
            "parentId" to currentParentId,
            "rewardTitle" to reward.title,
            "cost" to reward.cost,
            "status" to "pending",
            "timestamp" to System.currentTimeMillis()
        )

        db.collection("redemptions").add(redemptionData)
            .addOnSuccessListener {
                _uiState.update { it.copy(successMessage = "Kupiono: ${reward.title}!") }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(error = "Błąd zakupu: ${e.message}") }
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