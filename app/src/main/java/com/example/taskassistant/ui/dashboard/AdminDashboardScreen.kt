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

data class TaskItem(val title: String, val status: String, val color: Color)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onLogout: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Zadania", "Dzieci", "Nagrody", "Ustawienia")

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
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> TasksTab()
                1 -> KidsTab()
                2 -> RewardsTab()
                3 -> SettingsTab(onLogout = onLogout)
            }
        }
    }
}

@Composable
fun TasksTab() {
    val tasks = listOf(
        TaskItem("Wyrzuć śmieci", "Oczekuje", Color(0xFFFFB74D)),
        TaskItem("Posprzątaj pokój", "Zatwierdzone", Color(0xFF81C784)),
        TaskItem("Odrób lekcje", "Oczekuje", Color(0xFFFFB74D))
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Text(
                text = "Zadania do akceptacji",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        items(tasks) { task ->
            TaskCard(task)
        }
    }
}

@Composable
fun KidsTab() {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Tu będzie lista dzieci", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Dodaj dziecko")
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
fun TaskCard(task: TaskItem) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = task.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(text = "Status: ${task.status}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Icon(
                imageVector = if (task.status == "Zatwierdzone") Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = task.color
            )
        }
    }
}