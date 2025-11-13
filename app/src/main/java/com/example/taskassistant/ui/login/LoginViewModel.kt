package com.example.taskassistant.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
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
    val isLoginSuccessful: Boolean = false
)


class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()


    fun onEmailChange(newValue: String) {
        _uiState.update { it.copy(email = newValue, error = null) }
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

        _uiState.update { it.copy(isLoading = true, error = null) }

        viewModelScope.launch {
            auth.signInWithEmailAndPassword(currentState.email, currentState.password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        _uiState.update { it.copy(isLoading = false, isLoginSuccessful = true) }
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
}