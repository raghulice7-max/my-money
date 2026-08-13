package com.example

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.PendingSmsTransactionEntity
import com.example.data.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TransactionNotificationListener : NotificationListenerService() {

    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        try {
            val packageName = sbn.packageName ?: ""
            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return

            // Extract text contents from notification
            val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

            // Combine text to get complete details
            val fullBody = if (bigText.length > text.length) bigText else text
            if (fullBody.isBlank()) return

            Log.d("NotificationListener", "Intercepted notification from $packageName [Title: $title, Body: $fullBody]")

            // Only parse messages from messaging clients, Google Messages (RCS), bank/UPI notifications, etc.
            val isTargetPackage = packageName == "com.google.android.apps.messaging" ||
                    packageName == "com.samsung.android.messaging" ||
                    packageName.contains("message") ||
                    packageName.contains("sms") ||
                    packageName.contains("mms") ||
                    packageName.contains("pay") ||
                    packageName.contains("bank") ||
                    packageName.contains("wallet")

            if (!isTargetPackage) return

            val receiver = SmsReceiver()
            val parsedTx = receiver.parseSms(fullBody)

            if (parsedTx != null) {
                Log.d("NotificationListener", "Successfully parsed notification: amount=${parsedTx.amount}, merchant=${parsedTx.payeeOrMerchant}")

                scope.launch {
                    try {
                        val db = AppDatabase.getDatabase(applicationContext)
                        val dao = db.financeDao()

                        val currentPending = dao.getPendingTransactionsSync()
                        val currentTransactions = dao.getRecentTransactionsSync()

                        val currentTime = System.currentTimeMillis()

                        // Prevent duplicate entry if the transaction is already tracked
                        val isDuplicate = currentPending.any { 
                            it.amount == parsedTx.amount && 
                            (it.messageBody == fullBody || Math.abs(it.dateMillis - currentTime) < 15000) 
                        } || currentTransactions.any { 
                            it.amount == parsedTx.amount && 
                            Math.abs(it.dateMillis - currentTime) < 30000 
                        }

                        if (!isDuplicate) {
                            dao.insertPendingTransaction(
                                PendingSmsTransactionEntity(
                                    amount = parsedTx.amount,
                                    type = parsedTx.type,
                                    senderAddress = title.ifBlank { packageName },
                                    messageBody = fullBody,
                                    dateMillis = currentTime,
                                    initialCategory = parsedTx.guessedCategory,
                                    payeeOrMerchant = parsedTx.payeeOrMerchant
                                )
                            )
                            Log.d("NotificationListener", "Inserted newly intercepted transaction to pending list for approval: ${parsedTx.amount}")
                        } else {
                            Log.d("NotificationListener", "Transaction of amount ${parsedTx.amount} is a duplicate, skipping insertion.")
                        }
                    } catch (e: Exception) {
                        Log.e("NotificationListener", "Error saving pending transaction from notification", e)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("NotificationListener", "Error processing notification", e)
        }
    }
}
