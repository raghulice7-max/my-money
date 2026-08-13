package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "reminder_payments")
data class ReminderPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val reminderId: Long,
    val paidDate: LocalDate,
    val amount: Double
)
