package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sms_transactions")
data class PendingSmsTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val type: String,               // "INCOME" or "EXPENSE"
    val senderAddress: String,      // Bank sender ID (e.g. AXISBK, HDFCBK, GPAY)
    val messageBody: String,
    val dateMillis: Long,
    val initialCategory: String,    // Prefilled suggestion category
    val payeeOrMerchant: String     // Parsed merchant/recipient or source name
)
