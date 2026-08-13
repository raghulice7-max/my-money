package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.PendingSmsTransactionEntity
import com.example.data.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        try {
            if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
                if (messages.isEmpty()) return

                val sender = messages[0].originatingAddress ?: "Unknown"
                val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }

                if (fullBody.isBlank()) return

                Log.d("SmsReceiver", "Received SMS from $sender: $fullBody")

                // Parse SMS body
                val parsedTx = parseSms(fullBody)
                if (parsedTx != null) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            val dao = db.financeDao()
                            val currentTime = System.currentTimeMillis()

                            val currentPending = dao.getPendingTransactionsSync()
                            val currentTransactions = dao.getRecentTransactionsSync()

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
                                        senderAddress = sender,
                                        messageBody = fullBody,
                                        dateMillis = currentTime,
                                        initialCategory = parsedTx.guessedCategory,
                                        payeeOrMerchant = parsedTx.payeeOrMerchant
                                    )
                                )
                                Log.d("SmsReceiver", "Successfully saved transaction to pending list for approval: ${parsedTx.amount}")
                            } else {
                                Log.d("SmsReceiver", "Duplicate SMS transaction skipped: ${parsedTx.amount}")
                            }
                        } catch (e: Exception) {
                            Log.e("SmsReceiver", "Error saving SMS transaction", e)
                        } finally {
                            try {
                                pendingResult.finish()
                            } catch (un: Exception) {
                                Log.e("SmsReceiver", "Error finishing pending result", un)
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error inside SmsReceiver onReceive", e)
        }
    }

    data class ParsedSms(
        val amount: Double,
        val type: String, // "EXPENSE" or "INCOME"
        val guessedCategory: String,
        val payeeOrMerchant: String
    )

    private fun normalizeUnicode(input: String): String {
        return SmsParserUtility.normalizeUnicode(input)
    }

    fun parseSms(body: String): ParsedSms? {
        val result = SmsParserUtility.parseSms(body) ?: return null
        return ParsedSms(
            amount = result.amount,
            type = result.type,
            guessedCategory = result.guessedCategory,
            payeeOrMerchant = result.payeeOrMerchant
        )
    }
}
