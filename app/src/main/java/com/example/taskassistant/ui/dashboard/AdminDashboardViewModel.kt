package com.example.taskassistant.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class KidItem(val id: String, val email: String)

data class Task(
    val id: String = "",
    val title: String = "",
    val points: Int = 0,
    val status: String = "todo", // todo, pending, approved
    val assignedToId: String = "",
    val assignedToEmail: String = ""
)

data class AdminUiState(
    val kidsList: List<KidItem> = emptyList(),
    val tasksList: List<Task> = emptyList(),
    val addChildEmail: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

class AdminDashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AdminUiState())
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid

    init {
        fetchKids()
        fetchTasks()
    }

    fun onEmailChange(newEmail: String) {
        _uiState.update { it.copy(addChildEmail = newEmail, error = null, successMessage = null) }
    }

    fun onMessageShown() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    fun addChild() {
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
                        linkChildToParent(childDoc.id)
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
                fetchKids()
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isLoading = false, error = "Błąd usuwania: ${e.message}") }
            }
    }

    private fun linkChildToParent(childId: String) {
        if (currentUserId == null) return

        db.collection("users").document(childId)
            .update("parentId", currentUserId)
            .addOnSuccessListener {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        successMessage = "Dodano dziecko pomyślnie!",
                        addChildEmail = ""
                    )
                }
                fetchKids()
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isLoading = false, error = "Błąd łączenia: ${e.message}") }
            }
    }

    private fun fetchKids() {
        if (currentUserId == null) return

        db.collection("users")
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
    }
    fun addTask(title: String, points: Int, assignedChildId: String, assignedChildEmail: String) {
        if (currentUserId == null) return

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

    fun approveTask(taskId: String) {
        db.collection("tasks").document(taskId)
            .update("status", "approved")
            .addOnSuccessListener {
                _uiState.update { it.copy(successMessage = "Zadanie zatwierdzone! Punkty przyznane.") }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(error = "Błąd: ${e.message}") }
            }
    }

    private fun fetchTasks() {
        if (currentUserId == null) return

        db.collection("tasks")
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
                        assignedToEmail = doc.getString("assignedToEmail") ?: ""
                    )
                } ?: emptyList()

                _uiState.update { it.copy(tasksList = tasks) }
            }
    }

}