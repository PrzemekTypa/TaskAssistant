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

data class Reward(
    val id: String = "",
    val title: String = "",
    val cost: Int = 0,
    val parentId: String = ""
)

data class Redemption(
    val id: String = "",
    val childId: String = "",
    val parentId: String = "",
    val rewardTitle: String = "",
    val cost: Int = 0,
    val status: String = "pending",
    val timestamp: Long = System.currentTimeMillis()
)