package com.example.taskassistant.ui.dashboard

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class AdminUiState(
    val kidsList: List<KidItem> = emptyList(),
    val tasksList: List<Task> = emptyList(),
    val rewardsList: List<Reward> = emptyList(),
    val addChildEmail: String = "",
    val redemptionsList: List<Redemption> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class AdminDashboardViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()
    private var kidsListener: ListenerRegistration? = null
    private var tasksListener: ListenerRegistration? = null
    private var rewardsListener: ListenerRegistration? = null
    private var redemptionsListener: ListenerRegistration? = null

    fun startListening() {
        val currentUserId = auth.currentUser?.uid ?: return

        stopListening()

        kidsListener = db.collection("users")
            .whereEqualTo("parentId", currentUserId)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                val kids = value?.documents?.map { doc ->
                    KidItem(
                        id = doc.id,
                        email = doc.getString("email") ?: "Brak emaila"
                    )
                } ?: emptyList()

                _uiState.update { it.copy(kidsList = kids) }
            }

        tasksListener = db.collection("tasks")
            .whereEqualTo("parentId", currentUserId)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                val tasks = value?.documents?.map { doc ->
                    Task(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        points = doc.getLong("points")?.toInt() ?: 0,
                        status = doc.getString("status") ?: "todo",
                        assignedToId = doc.getString("assignedToId") ?: "",
                        assignedToEmail = doc.getString("assignedToEmail") ?: "",
                        photoUrl = doc.getString("photoUrl") ?: "",
                        submittedAt = doc.getLong("submittedAt") ?: 0L
                    )
                } ?: emptyList()

                val sortedTasks = tasks.sortedWith(
                    compareBy<Task> {
                        when(it.status) {
                            "pending" -> 0
                            "todo" -> 1
                            else -> 2
                        }
                    }.thenByDescending { it.submittedAt }
                )

                _uiState.update { it.copy(tasksList = sortedTasks) }
            }

        rewardsListener = db.collection("rewards")
            .whereEqualTo("parentId", currentUserId)
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                val rewards = value?.documents?.map { doc ->
                    Reward(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        cost = doc.getLong("cost")?.toInt() ?: 0,
                        parentId = doc.getString("parentId") ?: ""
                    )
                } ?: emptyList()

                _uiState.update { it.copy(rewardsList = rewards) }
            }

        redemptionsListener = db.collection("redemptions")
            .whereEqualTo("parentId", currentUserId)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { value, error ->
                if (error != null) return@addSnapshotListener

                val purchases = value?.documents?.mapNotNull { doc ->
                    Redemption(
                        id = doc.id,
                        childId = doc.getString("childId") ?: "",
                        parentId = doc.getString("parentId") ?: "",
                        rewardTitle = doc.getString("rewardTitle") ?: "",
                        cost = doc.getLong("cost")?.toInt() ?: 0,
                        status = doc.getString("status") ?: "pending"
                    )
                } ?: emptyList()

                _uiState.update { it.copy(redemptionsList = purchases) }
            }
    }

    fun stopListening() {
        kidsListener?.remove()
        tasksListener?.remove()
        rewardsListener?.remove()
        redemptionsListener?.remove()
    }

    override fun onCleared() {
        super.onCleared()
        stopListening()
    }

    fun onEmailChange(newEmail: String) {
        _uiState.update { it.copy(addChildEmail = newEmail, error = null, successMessage = null) }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    fun addChild() {
        val currentUserId = auth.currentUser?.uid ?: return
        val emailToAdd = _uiState.value.addChildEmail.trim()
        if (emailToAdd.isBlank()) {
            _uiState.update { it.copy(error = "Wpisz email dziecka") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }

        db.collection("users")
            .whereEqualTo("email", emailToAdd)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    _uiState.update { it.copy(isLoading = false, error = "Nie znaleziono użytkownika o tym emailu") }
                } else {
                    val childDoc = documents.first()
                    val role = childDoc.getString("role")

                    if (role == "child") {
                        linkChildToParent(childDoc.id, currentUserId)
                    } else {
                        _uiState.update { it.copy(isLoading = false, error = "Ten użytkownik nie jest kontem dziecka") }
                    }
                }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isLoading = false, error = "Błąd szukania: ${e.message}") }
            }
    }

    fun removeChild(childId: String) {
        _uiState.update { it.copy(isLoading = true) }

        db.collection("users").document(childId)
            .update("parentId", null)
            .addOnSuccessListener {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Usunięto dziecko z listy"
                    )
                }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isLoading = false, error = "Błąd usuwania: ${e.message}") }
            }
    }

    private fun linkChildToParent(childId: String, currentUserId: String) {
        db.collection("users").document(childId)
            .update("pendingParentId", currentUserId)
            .addOnSuccessListener {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Wysłano zaproszenie do dziecka!",
                        addChildEmail = ""
                    )
                }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isLoading = false, error = "Błąd wysyłania: ${e.message}") }
            }
    }

    fun addTask(title: String, points: Int, assignedChildId: String, assignedChildEmail: String) {
        val currentUserId = auth.currentUser?.uid ?: return

        if (points <= 0) {
            _uiState.update { it.copy(error = "Liczba punktów musi być większa od zera!") }
            return
        }

        if (assignedChildId == "ALL") {
            val kids = _uiState.value.kidsList
            if (kids.isEmpty()) {
                _uiState.update { it.copy(error = "Nie masz dodanych żadnych dzieci!") }
                return
            }

            val batch = db.batch()

            kids.forEach { kid ->
                val newDocRef = db.collection("tasks").document()
                val newTask = hashMapOf(
                    "title" to title,
                    "points" to points,
                    "status" to "todo",
                    "parentId" to currentUserId,
                    "assignedToId" to kid.id,
                    "assignedToEmail" to kid.email,
                    "createdAt" to System.currentTimeMillis()
                )
                batch.set(newDocRef, newTask)
            }

            batch.commit()
                .addOnSuccessListener {
                    _uiState.update { it.copy(successMessage = "Zadanie wysłane do wszystkich dzieci!") }
                }
                .addOnFailureListener { e ->
                    _uiState.update { it.copy(error = "Błąd wysyłania: ${e.message}") }
                }

        } else {
            val newTask = hashMapOf(
                "title" to title,
                "points" to points,
                "status" to "todo",
                "parentId" to currentUserId,
                "assignedToId" to assignedChildId,
                "assignedToEmail" to assignedChildEmail,
                "createdAt" to System.currentTimeMillis()
            )

            db.collection("tasks").add(newTask)
                .addOnSuccessListener {
                    _uiState.update { it.copy(successMessage = "Zadanie dodane!") }
                }
                .addOnFailureListener { e ->
                    _uiState.update { it.copy(error = "Błąd dodawania: ${e.message}") }
                }
        }
    }

    fun approveTask(taskId: String, childId: String, points: Int) {
        val taskRef = db.collection("tasks").document(taskId)

        _uiState.update { it.copy(isLoading = true) }

        taskRef.update("status", "approved")
            .addOnSuccessListener {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Zatwierdzono! Dodano $points pkt."
                    )
                }
            }
            .addOnFailureListener { e ->
                _uiState.update {
                    it.copy(isLoading = false, error = "Błąd: ${e.message}")
                }
            }
    }

    fun deleteTask(taskId: String) {
        db.collection("tasks").document(taskId)
            .delete()
            .addOnSuccessListener {
                _uiState.update { it.copy(successMessage = "Zadanie zostało usunięte") }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(error = "Błąd usuwania: ${e.message}") }
            }
    }

    fun addReward(title: String, cost: Int) {
        val currentUserId = auth.currentUser?.uid ?: return

        val newReward = Reward(
            title = title,
            cost = cost,
            parentId = currentUserId
        )

        db.collection("rewards").add(newReward)
            .addOnSuccessListener {
                _uiState.update { it.copy(successMessage = "Nagroda dodana!") }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(error = "Błąd: ${e.message}") }
            }
    }

    fun deleteReward(rewardId: String) {
        db.collection("rewards").document(rewardId).delete()
            .addOnSuccessListener {
                _uiState.update { it.copy(successMessage = "Nagroda usunięta") }
            }
    }

    fun markRedemptionAsDelivered(purchaseId: String) {
        db.collection("redemptions").document(purchaseId)
            .update("status", "completed")
            .addOnSuccessListener {
                _uiState.update { it.copy(successMessage = "Nagroda wydana!") }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(error = "Błąd: ${e.message}") }
            }
    }

}