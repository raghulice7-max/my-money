package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val targetDateMillis: Long,
    val category: String, // "Vehicle", "Home", "Travel", "Education", "Savings", "Emergency", "Other"
    val note: String = ""
)
