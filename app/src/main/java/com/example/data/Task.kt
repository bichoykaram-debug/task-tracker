package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String,
    val title: String,
    val details: String,
    val requester: String,
    val date: String,
    val dueDate: String,
    val priority: String, // "low", "medium", "high"
    val notes: String,
    val status: String,   // "inProgress", "onHold", "completed"
    val createdAt: Long,
    val completedDate: String?
)
