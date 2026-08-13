package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.data.AppDatabase
import com.example.data.ReminderPaymentEntity
import com.example.data.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_PAID = "com.example.notification.ACTION_MARK_PAID"
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_NAME = "extra_reminder_name"
        const val EXTRA_REMINDER_AMOUNT = "extra_reminder_amount"
    }

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == ACTION_MARK_PAID) {
            val reminderId = intent.getLongExtra(EXTRA_REMINDER_ID, -1L)
            val reminderName = intent.getStringExtra(EXTRA_REMINDER_NAME) ?: "Reminder"
            val amount = intent.getDoubleExtra(EXTRA_REMINDER_AMOUNT, 0.0)

            if (reminderId != -1L) {
                val pendingResult = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val db = AppDatabase.getDatabase(context)
                        val dao = db.financeDao()

                        val today = LocalDate.now()
                        // 1. Fetch reminder list
                        val reminderList = dao.getAllRecurringReminders().firstOrNull() ?: emptyList()
                        val matchingReminder = reminderList.find { it.id == reminderId }

                        if (matchingReminder != null) {
                            val updated = matchingReminder.copy(lastPaidDate = today)
                            dao.insertRecurringReminder(updated)

                            // 2. Insert payment record
                            dao.insertReminderPayment(
                                ReminderPaymentEntity(
                                    reminderId = reminderId,
                                    paidDate = today,
                                    amount = amount
                                )
                            )

                            // 3. Insert transaction log
                            dao.insertTransaction(
                                TransactionEntity(
                                    amount = amount,
                                    type = "EXPENSE",
                                    category = matchingReminder.category,
                                    payeeOrSource = reminderName,
                                    note = "Paid from notification action",
                                    dateMillis = System.currentTimeMillis()
                                )
                            )
                        }

                        // Dismiss notification
                        NotificationManagerCompat.from(context).cancel(reminderId.toInt())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
