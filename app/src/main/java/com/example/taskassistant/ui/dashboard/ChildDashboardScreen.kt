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
                1 -> ChildRewardsTab(viewModel)
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
            items(
                items = uiState.tasks,
                key = { task -> task.id }
            ) { task ->
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
fun ChildRewardsTab(viewModel: ChildDashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Dostępne środki", style = MaterialTheme.typography.titleMedium)
                Text(
                    "${uiState.userPoints} pkt",
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        Text("Sklepik z nagrodami:", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.rewards.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Rodzic nie dodał jeszcze nagród :(", color = Color.Gray)
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = uiState.rewards,
                    key = { it.id }
                ) { reward ->
                    val canAfford = uiState.userPoints >= reward.cost

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (canAfford) MaterialTheme.colorScheme.surface else Color.LightGray.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(reward.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("${reward.cost}", fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = { viewModel.redeemReward(reward) },
                                enabled = canAfford,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (canAfford) MaterialTheme.colorScheme.primary else Color.Gray
                                )
                            ) {
                                Text(if (canAfford) "Kup" else "Brakuje")
                            }
                        }
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