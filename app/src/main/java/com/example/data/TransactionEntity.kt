package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String, // "INCOME", "EXPENSE", "FUEL"
    val category: String, // "Salary", "Food", "Groceries", "Fuel", "Shopping", "Entertainment", "Utilities", "Other"
    val payeeOrSource: String, // Recipient or Source of money
    val dateMillis: Long,
    val note: String
)
