package com.example.taskassistant.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val selectedRole: String = "child",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isRegistrationSuccessful: Boolean = false
)

class RegisterViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    fun onEmailChange(newValue: String) {
        _uiState.update { it.copy(email = newValue, error = null) }
    }

    fun onPasswordChange(newValue: String) {
        _uiState.update { it.copy(password = newValue, error = null) }
    }

    fun onRoleChange(newRole: String) {
        _uiState.update { it.copy(selectedRole = newRole) }
    }

    fun register() {
        val state = _uiState.value

        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.update { it.copy(error = "Wypełnij wszystkie pola") }
            return
        }

        if (state.password.length < 6) {
            _uiState.update { it.copy(error = "Hasło musi mieć min. 6 znaków") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            auth.createUserWithEmailAndPassword(state.email, state.password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        if (userId != null) {
                            saveUserRole(userId, state.selectedRole)
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = task.exception?.localizedMessage ?: "Błąd rejestracji"
                            )
                        }
                    }
                }
        }
    }

    private fun saveUserRole(userId: String, role: String) {
        val userMap = hashMapOf(
            "role" to role,
            "email" to _uiState.value.email
        )

        db.collection("users").document(userId).set(userMap)
            .addOnSuccessListener {
                auth.signOut()
                _uiState.update { it.copy(isLoading = false, isRegistrationSuccessful = true) }
            }
            .addOnFailureListener { e ->
                _uiState.update { it.copy(isLoading = false, error = "Błąd zapisu roli: ${e.message}") }
            }
    }

    fun onRegistrationSuccessHandled() {
        _uiState.update { it.copy(isRegistrationSuccessful = false) }
    }
}