package com.example.taskassistant.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoginSuccessful: Boolean = false,
    val userRole: String? = null,
    val resetPasswordMessage: String? = null
)

class LoginViewModel(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(newValue: String) {
        _uiState.update { it.copy(email = newValue, error = null, resetPasswordMessage = null) }
    }

    fun onPasswordChange(newValue: String) {
        _uiState.update { it.copy(password = newValue, error = null) }
    }

    fun login() {
        val currentState = _uiState.value

        if (currentState.email.isBlank() || currentState.password.isBlank()) {
            _uiState.update { it.copy(error = "Email i hasło nie mogą być puste") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null, resetPasswordMessage = null) }

        viewModelScope.launch {
            auth.signInWithEmailAndPassword(currentState.email, currentState.password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        if (user != null) {
                            if (user.isEmailVerified) {
                                fetchUserRole(user.uid)
                            } else {
                                auth.signOut()
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        error = "Potwierdź swój adres e-mail, klikając w link wysłany na Twoją skrzynkę."
                                    )
                                }
                            }
                        } else {
                            _uiState.update { it.copy(isLoading = false, error = "Błąd pobierania ID użytkownika") }
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                error = task.exception?.localizedMessage ?: "Błąd logowania"
                            )
                        }
                    }
                }
        }
    }

    fun resetPassword() {
        val email = _uiState.value.email
        if (email.isBlank()) {
            _uiState.update { it.copy(error = "Wpisz swój e-mail wyżej, aby zresetować hasło") }
            return
        }

        _uiState.update { it.copy(isLoading = true, error = null, resetPasswordMessage = null) }

        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        resetPasswordMessage = "Wysłano link do resetu hasła na e-mail: $email"
                    )
                }
            }
            .addOnFailureListener { e ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Błąd podczas wysyłania linku"
                    )
                }
            }
    }

    private fun fetchUserRole(userId: String) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isLoginSuccessful = true,
                            userRole = role
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, error = "Brak profilu użytkownika w bazie") }
                }
            }
            .addOnFailureListener { exception ->
                _uiState.update { it.copy(isLoading = false, error = "Błąd bazy: ${exception.message}") }
            }
    }

    fun onLoginSuccessHandled() {
        _uiState.update { it.copy(isLoginSuccessful = false) }
    }
}