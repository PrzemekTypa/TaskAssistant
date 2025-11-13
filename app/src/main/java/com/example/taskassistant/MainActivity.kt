package com.example.taskassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.taskassistant.ui.login.LoginScreen
import com.example.taskassistant.ui.dashboard.AdminDashboardScreen
import com.example.taskassistant.ui.theme.TaskAssistantTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskAssistantTheme {
                Surface(modifier = Modifier.fillMaxSize()) {

                    var currentScreen by remember { mutableStateOf("login") }

                    when (currentScreen) {
                        "login" -> {
                            LoginScreen(
                                onLoginSuccess = {
                                    currentScreen = "admin_dashboard"
                                }
                            )
                        }
                        "admin_dashboard" -> {
                            AdminDashboardScreen(
                                onLogout = {
                                    FirebaseAuth.getInstance().signOut()
                                    currentScreen = "login"
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}