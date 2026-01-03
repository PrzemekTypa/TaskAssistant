package com.example.taskassistant.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState

data class ChildTask(val title: String, val points: Int, val isDone: Boolean)
data class RewardItem(val title: String, val cost: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDashboardScreen(
    onLogout: () -> Unit,
    viewModel: ChildDashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Moje Zadania", "Nagrody", "Profil")


    val currentUser = FirebaseAuth.getInstance().currentUser
    val userEmail = currentUser?.email ?: "Użytkownik"
    val userName = userEmail.substringBefore("@")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cześć, $userName!") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, title ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        label = { Text(title) },
                        icon = {
                            when (title) {
                                "Moje Zadania" -> Icon(Icons.Default.Home, contentDescription = null)
                                "Nagrody" -> Icon(Icons.Default.Favorite, contentDescription = null)
                                "Profil" -> Icon(Icons.Default.Face, contentDescription = null)
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> ChildTasksTab(viewModel)
                1 -> ChildRewardsTab(uiState.userPoints)
                2 -> ChildProfileTab(onLogout, currentUser?.email ?: "")
            }
        }
    }
}

@Composable
fun ChildTasksTab(viewModel: ChildDashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.tasks.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Brak zadań na dziś! Odpoczywaj. 😊", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text("Twoje zadania:", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
            }
            items(uiState.tasks) { task ->
                ChildTaskCard(task, onDone = { viewModel.markTaskAsDone(task.id) })
            }
        }
    }
}

@Composable
fun ChildTaskCard(task: Task, onDone: () -> Unit) {
    val isPending = task.status == "pending"
    val isApproved = task.status == "approved"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when {
                isApproved -> Color(0xFFE8F5E9)
                isPending -> Color(0xFFFFF3E0)
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("${task.points} pkt", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            when {
                isApproved -> Icon(Icons.Default.CheckCircle, "Gotowe", tint = Color(0xFF43A047))
                isPending -> Text("Czeka na sprawdzenie", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                else -> Button(onClick = onDone) {
                    Text("Zrobione")
                }
            }
        }
    }
}

@Composable
fun ChildRewardsTab(points: Int) {
    val rewards = listOf(
        RewardItem("Godzina grania", 100),
        RewardItem("Wyjście na lody", 250),
        RewardItem("Kino", 500)
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Twoje punkty: $points", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(24.dp))
        Text("Na co zbieramy?", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        rewards.forEach { reward ->
            Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Row(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(reward.title, style = MaterialTheme.typography.bodyLarge)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${reward.cost}", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ChildProfileTab(onLogout: () -> Unit, email: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(16.dp))
        Text(email, style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Wyloguj się")
        }
    }
}