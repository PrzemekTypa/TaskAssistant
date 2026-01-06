package com.example.taskassistant.ui.dashboard


data class Task(
    val id: String = "",
    val title: String = "",
    val points: Int = 0,
    val status: String = "todo", // todo, pending, approved
    val assignedToId: String = "",
    val assignedToEmail: String = ""
)


data class KidItem(val id: String, val email: String)