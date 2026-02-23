package com.example.taskassistant

import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AppScreen {
    LOADING, LOGIN, REGISTER, ADMIN_DASHBOARD, CHILD_DASHBOARD
}

class MainViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _currentScreen = MutableStateFlow(AppScreen.LOADING)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    init {
        checkUserSession()
    }

    fun checkUserSession() {
        val user = auth.currentUser
        if (user != null) {
            db.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val role = document.getString("role")
                    if (role == "parent") {
                        _currentScreen.update { AppScreen.ADMIN_DASHBOARD }
                    } else {
                        _currentScreen.update { AppScreen.CHILD_DASHBOARD }
                    }
                }
                .addOnFailureListener {
                    auth.signOut()
                    _currentScreen.update { AppScreen.LOGIN }
                }
        } else {
            _currentScreen.update { AppScreen.LOGIN }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.update { screen }
    }
}