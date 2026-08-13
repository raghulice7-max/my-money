package com.example.notification

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

object ReminderNotificationScheduler {

    fun scheduleReminderWork(
        context: Context,
        reminderName: String,
        dayOfMonth: Int,
        amount: Double
    ) {
        val workData = workDataOf(
            "reminder_name" to reminderName,
            "day_of_month" to dayOfMonth,
            "amount" to amount
        )

        val workRequest = PeriodicWorkRequestBuilder<ReminderNotificationWorker>(
            24, TimeUnit.HOURS
        )
            .setInputData(workData)
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(false)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "reminder_work_$reminderName",
            ExistingPeriodicWorkPolicy.UPDATE,
            workRequest
        )
    }

    fun cancelReminderWork(context: Context, reminderName: String) {
        WorkManager.getInstance(context).cancelUniqueWork("reminder_work_$reminderName")
    }
}
