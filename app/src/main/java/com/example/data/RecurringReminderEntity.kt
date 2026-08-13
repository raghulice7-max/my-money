package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

@Entity(tableName = "recurring_reminders")
data class RecurringReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val amount: Double,
    val dayOfMonth: Int,
    val type: String = "SIP",
    val lastPaidDate: LocalDate? = null,
    val category: String = "Investment",
    val notes: String = ""
) {
    val typeEnum: ReminderType
        get() = ReminderType.fromCode(type)

    val categoryEnum: ReminderCategory
        get() = ReminderCategory.fromCode(category)
}
