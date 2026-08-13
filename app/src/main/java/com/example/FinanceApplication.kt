package com.example

import android.app.Application
import com.example.notification.ReminderNotificationHelper

class FinanceApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderNotificationHelper.createNotificationChannel(this)
    }
}
