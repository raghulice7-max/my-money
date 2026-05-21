package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "fuel_entries")
data class FuelEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amountPaid: Double,
    val volumeLiters: Double,
    val odometerReading: Double,
    val dateMillis: Long,
    val note: String,
    val vehicleType: String = "CAR" // "CAR" or "BIKE"
)
