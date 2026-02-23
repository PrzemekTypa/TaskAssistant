package com.example.taskassistant.ui.dashboard

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.taskassistant.storage.StorageHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.messaging.FirebaseMessaging
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
    val successMessage: String? = null,
    val pendingParentId: String? = null
)

class ChildDashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ChildUiState())
    val uiState: StateFlow<ChildUiState> = _uiState.asStateFlow()

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val messaging = FirebaseMessaging.getInstance()
    private var currentUserId: String? = null

    private var currentParentId: String = ""
    private var totalEarnedPoints = 0
    private var totalSpentPoints = 0

    private var userListener: ListenerRegistration? = null
    private var rewardsListener: ListenerRegistration? = null
    private var tasksListener: ListenerRegistration? = null
    private var redemptionsListener: ListenerRegistration? = null

    fun startListening() {
        currentUserId = auth.currentUser?.uid
        if (currentUserId == null) return

        stopListening()

        Log.d("FIREBASE_LOG", "Start nasłuchu Dziecka. UserID: $currentUserId")
        startListeningToUserAndRewards()
        startListeningToTasks()
        startListeningToRedemptions()
    }

    fun stopListening() {
        userListener?.remove()
        rewardsListener?.remove()
        tasksListener?.remove()
        redemptionsListener?.remove()
    }

    private fun startListeningToUserAndRewards() {
        val userId = currentUserId ?: return

        userListener = db.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val parentId = snapshot?.getString("parentId")
                val pendingParentId = snapshot?.getString("pendingParentId")

                _uiState.update { it.copy(pendingParentId = pendingParentId) }

                if (!parentId.isNullOrEmpty()) {
                    currentParentId = parentId
                    startListeningToRewards(parentId)
                } else {
                    currentParentId = ""
                    startListeningToRewards(null)
                }
            }
    }

    fun startListeningToRewards(parentId: String?) {
        rewardsListener?.remove()

        if (parentId.isNullOrEmpty()) {
            _uiState.update { it.copy(rewards = emptyList()) }
            return
        }

        rewardsListener = db.collection("rewards")
            .whereEqualTo("parentId", parentId)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _uiState.update { it.copy(error = "Błąd pobierania nagród: ${e.message}") }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val rewardsList = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Reward::class.java)?.copy(id = doc.id)
                    }
                    _uiState.update { it.copy(rewards = rewardsList) }
                }
            }
    }

    private fun startListeningToTasks() {
        val userId = currentUserId ?: return

        tasksListener = db.collection("tasks")
            .whereEqualTo("assignedToId", userId)
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
        val userId = currentUserId ?: return

        redemptionsListener = db.collection("redemptions")
            .whereEqualTo("childId", userId)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                val redemptions = value?.documents?.mapNotNull { doc ->
                    Redemption(
                        id = doc.id,
                        childId = doc.getString("childId") ?: "",
                        parentId = doc.getString("parentId") ?: "",
                        rewardTitle = doc.getString("rewardTitle") ?: "",
                        cost = doc.getLong("cost")?.toInt() ?: 0,
                        status = doc.getString("status") ?: "pending",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()
                    )
                } ?: emptyList()

                totalSpentPoints = redemptions
                    .filter { it.status == "pending" || it.status == "completed" }
                    .sumOf { it.cost }

                recalculatePoints()
            }
    }

    private fun recalculatePoints() {
        _uiState.update {
            it.copy(userPoints = totalEarnedPoints - totalSpentPoints)
        }
    }

    fun markTaskAsDone(taskId: String) {
        _uiState.update { it.copy(isLoading = true) }

        db.collection("tasks").document(taskId)
            .update(
                mapOf(
                    "status" to "pending",
                    "submittedAt" to System.currentTimeMillis()
                )
            )
            .addOnSuccessListener {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "✅ Zadanie wysłane do zatwierdzenia!"
                    )
                }
                sendNotificationToParent(taskId)
            }
            .addOnFailureListener { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Błąd: ${e.message}"
                    )
                }
            }
    }

    fun submitTaskWithPhoto(taskId: String, photoUri: Uri) {
        _uiState.update { it.copy(isLoading = true) }

        StorageHelper.uploadTaskPhoto(
            taskId,
            photoUri,
            onSuccess = { photoUrl ->
                db.collection("tasks").document(taskId).update(
                    mapOf(
                        "status" to "pending",
                        "photoUrl" to photoUrl,
                        "submittedAt" to System.currentTimeMillis()
                    )
                ).addOnSuccessListener {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "✅ Zdjęcie wysłane! Czekamy na zatwierdzenie 📸"
                        )
                    }
                    sendNotificationToParent(taskId)
                }.addOnFailureListener { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Błąd zapisu: ${e.message}"
                        )
                    }
                }
            },
            onFailure = { exception ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Błąd uploadowania: ${exception.message}"
                    )
                }
            }
        )
    }

    fun redeemReward(reward: Reward) {
        if (_uiState.value.userPoints < reward.cost) {
            _uiState.update { it.copy(error = "Brakuje Ci punktów!") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        val redemption = Redemption(
            childId = currentUserId ?: "",
            parentId = currentParentId,
            rewardTitle = reward.title,
            cost = reward.cost,
            status = "pending",
            timestamp = System.currentTimeMillis()
        )

        db.collection("redemptions").add(redemption)
            .addOnSuccessListener {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "🎉 Nagroda zarezerwowana! Czekaj na dostawę!"
                    )
                }
                sendNotificationToParentRedemption(reward.title)
            }
            .addOnFailureListener { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Błąd: ${e.message}"
                    )
                }
            }
    }

    private fun sendNotificationToParent(taskId: String) {
        Log.d("FIREBASE_LOG", "Notyfikacja do rodzica: Zadanie $taskId czeka na zatwierdzenie (pending)")
    }

    private fun sendNotificationToParentRedemption(rewardTitle: String) {
        Log.d("FIREBASE_LOG", "Notyfikacja do rodzica: Dziecko chce nagrodę $rewardTitle")
    }

    fun acceptParentInvite() {
        val userId = currentUserId ?: return
        val pendingId = _uiState.value.pendingParentId ?: return

        _uiState.update { it.copy(isLoading = true) }

        val updates = mapOf(
            "parentId" to pendingId,
            "pendingParentId" to null
        )

        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener {
                _uiState.update { it.copy(isLoading = false, successMessage = "Połączono z kontem rodzica!") }
                startListeningToRewards(pendingId)
            }
    }

    fun rejectParentInvite() {
        val userId = currentUserId ?: return
        db.collection("users").document(userId).update("pendingParentId", null)
    }

    fun onMessageShown() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }
}