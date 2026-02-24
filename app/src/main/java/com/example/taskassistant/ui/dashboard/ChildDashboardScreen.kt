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
import android.widget.Toast
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.LaunchedEffect
import com.example.taskassistant.ui.camera.CameraScreen
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.List


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildDashboardScreen(
    onLogout: () -> Unit,
    viewModel: ChildDashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.startListening()
    }

    LaunchedEffect(uiState.error, uiState.successMessage) {
        uiState.error?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onMessageShown()
        }
        uiState.successMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.onMessageShown()
        }
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Moje Zadania", "Nagrody", "Ustawienia")


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
                                "Ustawienia" -> Icon(Icons.Default.Settings, contentDescription = null)
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
                2 -> ChildSettingsTab(onLogout,currentUser?.email ?: "", uiState, viewModel)
            }
        }
    }
}

@Composable
fun ChildTasksTab(viewModel: ChildDashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    var cameraTaskId by remember { mutableStateOf<String?>(null) }


    if (cameraTaskId != null) {
        CameraScreen(
            taskId = cameraTaskId!!,
            onPhotoCaptured = { photoUri ->
                viewModel.submitTaskWithPhoto(cameraTaskId!!, photoUri)
                cameraTaskId = null
            },
            onDismiss = { cameraTaskId = null }
        )
        return
    }

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
                ChildTaskCard(
                    task = task,
                    onOpenCamera = { taskId -> cameraTaskId = taskId }
                )
            }
        }
    }
}

@Composable
fun ChildTaskCard(
    task: Task,
    onOpenCamera: (String) -> Unit
) {
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
                isPending -> Text("⏳ Czeka", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                else -> Button(onClick = { onOpenCamera(task.id) }) {
                    Text("📸 Zrób zdjęcie")
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
fun ChildSettingsTab(
    onLogout: () -> Unit,
    email: String,
    uiState: ChildUiState,
    viewModel: ChildDashboardViewModel
) {
    var showHistoryDialog by remember { mutableStateOf(false) }
    if (showHistoryDialog) {
        Dialog(
            onDismissRequest = { showHistoryDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Historia transakcji", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showHistoryDialog = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Zamknij")
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.redemptionsHistory.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Brak historii punktów.", color = Color.Gray)
                        }
                    } else {
                        LazyColumn {
                            items(uiState.redemptionsHistory) { redemption ->
                                val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                                val dateString = sdf.format(Date(redemption.timestamp))

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(text = redemption.rewardTitle, fontWeight = FontWeight.Bold)
                                            Text(text = dateString, fontSize = 12.sp, color = Color.Gray)

                                            val statusText = if (redemption.status == "pending") "Oczekuje na wydanie" else "Wydane"
                                            val statusColor = if (redemption.status == "pending") Color(0xFFFFA500) else Color(0xFF4CAF50)
                                            Text(text = statusText, fontSize = 12.sp, color = statusColor, fontWeight = FontWeight.SemiBold)
                                        }
                                        Text(
                                            text = "-${redemption.cost} pkt",
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Icon(androidx.compose.material.icons.Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(80.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text(email, style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (uiState.pendingParentId != null) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Masz zaproszenie do połączenia z kontem Rodzica!", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { viewModel.acceptParentInvite() }) {
                                Text("Akceptuj")
                            }
                            OutlinedButton(onClick = { viewModel.rejectParentInvite() }) {
                                Text("Odrzuć")
                            }
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { showHistoryDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Icon(Icons.Default.List, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Zobacz historię transakcji")
            }
        }

        item {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Wyloguj się")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}