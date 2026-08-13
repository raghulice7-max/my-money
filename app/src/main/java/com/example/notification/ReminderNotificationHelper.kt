package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.util.formatInrCurrency

object ReminderNotificationHelper {

    const val CHANNEL_ID = "sip_rd_reminders_channel"
    private const val CHANNEL_NAME = "SIP & RD Payment Reminders"
    private const val CHANNEL_DESC = "Notifications for due SIP, RD, and recurring bill payments"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun showPaymentDueNotification(
        context: Context,
        reminderId: Long,
        reminderName: String,
        amount: Double
    ) {
        createNotificationChannel(context)

        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action Intent to Mark Paid directly from Notification
        val payIntent = Intent(context, ReminderActionReceiver::class.java).apply {
            action = ReminderActionReceiver.ACTION_MARK_PAID
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_ID, reminderId)
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_NAME, reminderName)
            putExtra(ReminderActionReceiver.EXTRA_REMINDER_AMOUNT, amount)
        }
        val payPendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt() + 10000,
            payIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val formattedAmt = formatInrCurrency(amount)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Payment Due: $reminderName")
            .setContentText("Reminder to pay $formattedAmt due today.")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Your $reminderName payment of $formattedAmt is due today. Mark as paid to log it directly into your expense tracker."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_save,
                "Mark as Paid",
                payPendingIntent
            )

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(reminderId.toInt(), builder.build())
        } catch (e: SecurityException) {
            // Permission for POST_NOTIFICATIONS may be required on Android 13+
            e.printStackTrace()
        }
    }
}
