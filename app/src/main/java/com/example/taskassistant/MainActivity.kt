package com.example.taskassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.taskassistant.ui.login.LoginScreen
import com.example.taskassistant.ui.dashboard.AdminDashboardScreen
import com.example.taskassistant.ui.dashboard.ChildDashboardScreen
import com.example.taskassistant.ui.register.RegisterScreen
import com.example.taskassistant.ui.theme.TaskAssistantTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            TaskAssistantTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val mainViewModel: MainViewModel = viewModel()
                    val currentScreen by mainViewModel.currentScreen.collectAsState()

                    when (currentScreen) {
                        AppScreen.LOADING -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                        AppScreen.LOGIN -> {
                            LoginScreen(
                                onLoginSuccess = {
                                    mainViewModel.checkUserSession()
                                },
                                onNavigateToRegister = {
                                    mainViewModel.navigateTo(AppScreen.REGISTER)
                                },
                                onNavigateToChildDashboard = {
                                    mainViewModel.checkUserSession()
                                }
                            )
                        }
                        AppScreen.REGISTER -> {
                            RegisterScreen(
                                onRegisterSuccess = {
                                    mainViewModel.navigateTo(AppScreen.LOGIN)
                                },
                                onBackToLogin = {
                                    mainViewModel.navigateTo(AppScreen.LOGIN)
                                }
                            )
                        }
                        AppScreen.ADMIN_DASHBOARD -> {
                            AdminDashboardScreen(
                                onLogout = {
                                    FirebaseAuth.getInstance().signOut()
                                    mainViewModel.checkUserSession()
                                }
                            )
                        }
                        AppScreen.CHILD_DASHBOARD -> {
                            ChildDashboardScreen(
                                onLogout = {
                                    FirebaseAuth.getInstance().signOut()
                                    mainViewModel.checkUserSession()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}