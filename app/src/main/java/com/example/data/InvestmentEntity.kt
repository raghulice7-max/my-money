package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // "Stocks", "Mutual Funds", "Savings/FD", "Gold", "Crypto", "Other"
    val investedAmount: Double,
    val currentValue: Double,
    val dateMillis: Long,
    val note: String = ""
)
