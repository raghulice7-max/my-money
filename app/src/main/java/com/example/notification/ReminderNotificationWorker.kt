package com.example.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.data.AppDatabase
import kotlinx.coroutines.flow.firstOrNull
import java.time.LocalDate

class ReminderNotificationWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val reminderName = inputData.getString("reminder_name") ?: return Result.failure()
            val dayOfMonth = inputData.getInt("day_of_month", -1)
            val amount = inputData.getDouble("amount", 0.0)

            if (dayOfMonth == -1) return Result.failure()

            val db = AppDatabase.getDatabase(context)
            val dao = db.financeDao()

            val allReminders = dao.getAllRecurringReminders().firstOrNull() ?: emptyList()
            val reminder = allReminders.find { it.name == reminderName }

            if (reminder != null) {
                val today = LocalDate.now()
                val isPaidThisMonth = reminder.lastPaidDate?.let {
                    it.year == today.year && it.month == today.month
                } ?: false

                if (!isPaidThisMonth && today.dayOfMonth == dayOfMonth) {
                    ReminderNotificationHelper.showPaymentDueNotification(
                        context,
                        reminder.id,
                        reminder.name,
                        reminder.amount
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }
}
