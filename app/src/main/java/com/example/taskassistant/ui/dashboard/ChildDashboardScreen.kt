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

data class ChildTask(val title: String, val points: Int, val isDone: Boolean)
data class RewardItem(val title: String, val cost: Int)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDashboardScreen(
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Moje Zadania", "Nagrody", "Profil")

    // Pobieramy e-mail i wycinamy nazwę przed małpą
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
                0 -> ChildTasksTab()
                1 -> ChildRewardsTab()
                2 -> ChildProfileTab(onLogout, userEmail)
            }
        }
    }
}

@Composable
fun ChildTasksTab() {
    val tasks = listOf(
        ChildTask("Pościel łóżko", 10, false),
        ChildTask("Odrób lekcje", 20, false),
        ChildTask("Wyprowadź psa", 15, true)
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text("Do zrobienia dzisiaj:", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(tasks) { task ->
            ChildTaskCard(task)
        }
    }
}

@Composable
fun ChildTaskCard(task: ChildTask) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (task.isDone) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Text("${task.points} pkt", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (task.isDone) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF43A047))
            } else {
                Button(onClick = { }) {
                    Text("Zrobione")
                }
            }
        }
    }
}

@Composable
fun ChildRewardsTab() {
    val rewards = listOf(
        RewardItem("Godzina grania", 100),
        RewardItem("Wyjście na lody", 250),
        RewardItem("Kino", 500)
    )

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Twoje punkty: 45", style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
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