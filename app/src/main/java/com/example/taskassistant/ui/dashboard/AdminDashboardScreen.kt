package com.example.taskassistant.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.widget.Toast
import androidx.compose.material.icons.filled.Face
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit,
    viewModel: AdminDashboardViewModel = viewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showAddTaskDialog by remember { mutableStateOf(false) }
    val tabs = listOf("Zadania", "Dzieci", "Nagrody", "Ustawienia")

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

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

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Panel Rodzica") }
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
                                "Zadania" -> Icon(Icons.Default.List, contentDescription = null)
                                "Dzieci" -> Icon(Icons.Default.Person, contentDescription = null)
                                "Nagrody" -> Icon(Icons.Default.CardGiftcard, contentDescription = null)
                                "Ustawienia" -> Icon(Icons.Default.Settings, contentDescription = null)
                            }
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            if (selectedTab == 0) {
                FloatingActionButton(onClick = { showAddTaskDialog = true }) {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> TasksTab(viewModel)
                1 -> KidsTab(viewModel)
                2 -> RewardsTab()
                3 -> SettingsTab(onLogout = onLogout)
            }
        }
        if (showAddTaskDialog) {
            AddTaskDialog(
                viewModel = viewModel,
                onDismiss = { showAddTaskDialog = false }
            )
        }
    }
}

@Composable
fun TasksTab(viewModel: AdminDashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.tasksList.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Brak zadań. Kliknij + aby dodać.", color = Color.Gray)
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.tasksList) { task ->
                TaskCard(task, viewModel)
            }
        }
    }
}

@Composable
fun KidsTab(viewModel: AdminDashboardViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Twoje Dzieci", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))


        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Dodaj nowe dziecko", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = uiState.addChildEmail,
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = { Text("Email dziecka") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.addChild() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Połącz konto")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))


        if (uiState.kidsList.isEmpty()) {
            Text("Brak połączonych kont", color = Color.Gray)
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(uiState.kidsList) { kid ->
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Face, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(kid.email, style = MaterialTheme.typography.bodyLarge)
                            }


                            IconButton(onClick = { viewModel.removeChild(kid.id) }) {
                                Icon(
                                    androidx.compose.material.icons.Icons.Default.Delete,
                                    contentDescription = "Usuń",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RewardsTab() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Tu będzie lista nagród", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Dodaj nagrodę")
        }
    }
}

@Composable
fun SettingsTab(onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
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

@Composable
fun TaskCard(task: Task, viewModel: AdminDashboardViewModel) {
    val statusColor = when(task.status) {
        "approved" -> Color(0xFF81C784)
        "pending" -> Color(0xFF64B5F6)
        else -> Color(0xFFFFB74D)
    }

    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text("Dla: ${task.assignedToEmail}", style = MaterialTheme.typography.bodySmall)
                    Text("${task.points} pkt", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }


                Icon(
                    imageVector = if (task.status == "approved") Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = statusColor
                )
            }


            if (task.status == "pending") {
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.approveTask(task.id) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF43A047))
                ) {
                    Text("Zatwierdź i daj punkty")
                }
            }
        }
    }
}

@Composable
fun AddTaskDialog(viewModel: AdminDashboardViewModel, onDismiss: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    var title by remember { mutableStateOf("") }
    var points by remember { mutableStateOf("10") }
    var selectedChildId by remember { mutableStateOf(uiState.kidsList.firstOrNull()?.id ?: "") }

    Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Nowe Zadanie", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Co zrobić?") })
                OutlinedTextField(value = points, onValueChange = { points = it }, label = { Text("Punkty") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))

                Spacer(modifier = Modifier.height(16.dp))
                Text("Dla kogo?")

                if (uiState.kidsList.isEmpty()) {
                    Text("Brak dzieci! Dodaj je w zakładce Dzieci.", color = Color.Red)
                } else {
                    uiState.kidsList.forEach { kid ->
                        Row(
                            Modifier.fillMaxWidth().selectable(selected = (kid.id == selectedChildId), onClick = { selectedChildId = kid.id }).padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = (kid.id == selectedChildId), onClick = { selectedChildId = kid.id })
                            Text(kid.email)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        val assignedEmail = uiState.kidsList.find { it.id == selectedChildId }?.email ?: ""
                        viewModel.addTask(title, points.toIntOrNull() ?: 0, selectedChildId, assignedEmail)
                        onDismiss()
                    },
                    enabled = title.isNotBlank() && selectedChildId.isNotBlank()
                ) {
                    Text("Zapisz")
                }
            }
        }
    }
}