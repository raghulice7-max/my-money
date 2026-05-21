package com.example

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.PendingSmsTransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.regex.Pattern

class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
            for (sms in messages) {
                val body = sms.messageBody ?: continue
                val sender = sms.originatingAddress ?: "Unknown"

                Log.d("SmsReceiver", "Received SMS from $sender: $body")

                // Parse SMS body
                val parsedTx = parseSms(body)
                if (parsedTx != null) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            db.financeDao().insertPendingTransaction(
                                PendingSmsTransactionEntity(
                                    amount = parsedTx.amount,
                                    type = parsedTx.type,
                                    senderAddress = sender,
                                    messageBody = body,
                                    dateMillis = System.currentTimeMillis(),
                                    initialCategory = parsedTx.guessedCategory,
                                    payeeOrMerchant = parsedTx.payeeOrMerchant
                                )
                            )
                            Log.d("SmsReceiver", "Successfully saved pending transaction for ${parsedTx.amount}")
                        } catch (e: Exception) {
                            Log.e("SmsReceiver", "Error saving pending transaction", e)
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }

    data class ParsedSms(
        val amount: Double,
        val type: String, // "EXPENSE" or "INCOME"
        val guessedCategory: String,
        val payeeOrMerchant: String
    )

    fun parseSms(body: String): ParsedSms? {
        val lower = body.lowercase()

        // Skip standard authorization code / OTP lines
        if (lower.contains("otp") || lower.contains("verification code") || lower.contains("security code") || lower.contains("one time password")) {
            return null
        }

        // Common transaction words to scan
        val isExpense = lower.contains("debited") || 
                        lower.contains("spent") || 
                        lower.contains("paid") || 
                        lower.contains("sent") || 
                        lower.contains("txn of") ||
                        lower.contains("transferred to") ||
                        lower.contains("withdrawn") ||
                        lower.contains("charge")
                        
        val isIncome = lower.contains("credited") || 
                       lower.contains("received") || 
                       lower.contains("deposited") ||
                       lower.contains("reimbursement")

        // Must be a clear credit/debit to avoid spam SMS
        if (!isExpense && !isIncome) {
            return null
        }

        // Indian bank regex styles: "Rs 500", "Rs. 1,000.00", "INR 200"
        val amountRegex = Pattern.compile("(?:rs\\.?|inr)\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
        val matcher = amountRegex.matcher(body)
        if (!matcher.find()) {
            return null
        }

        val amountStr = matcher.group(1)?.replace(",", "") ?: return null
        val amount = amountStr.toDoubleOrNull() ?: return null

        // Merchant identification
        var payee = "Merchant"
        val atRegex = Pattern.compile("at\\s+([A-Za-z0-9\\s\\-&]+)", Pattern.CASE_INSENSITIVE)
        val toRegex = Pattern.compile("to\\s+([A-Za-z0-9\\s\\-&]+)", Pattern.CASE_INSENSITIVE)
        val infoRegex = Pattern.compile("info[:*\\s]+([A-Za-z0-9\\s\\-&]+)", Pattern.CASE_INSENSITIVE)

        if (atRegex.matcher(body).let { if (it.find()) { payee = it.group(1) ?: "Merchant"; true } else false }) {
            // Found
        } else if (toRegex.matcher(body).let { if (it.find()) { payee = it.group(1) ?: "Merchant"; true } else false }) {
            // Found
        } else if (infoRegex.matcher(body).let { if (it.find()) { payee = it.group(1) ?: "Merchant"; true } else false }) {
            // Found
        }

        // Clean trailing transaction jargon
        val stopWords = listOf("ref", "on", "txn", "is", "using", "date", "via", "at", "to", "card", "closing")
        var cleanPayee = payee.trim()
        for (word in stopWords) {
            if (cleanPayee.lowercase().contains(" $word")) {
                cleanPayee = cleanPayee.substring(0, cleanPayee.lowercase().indexOf(" $word")).trim()
            }
        }
        cleanPayee = cleanPayee.replace(Regex("[^A-Za-z0-9\\s\\-&]"), "").trim()
        if (cleanPayee.isBlank()) {
            cleanPayee = if (isExpense) "Merchant Spend" else "External Credit"
        }

        // Category parsing based on keywords
        var category = "Other"
        when {
            lower.contains("swiggy") || lower.contains("zomato") || lower.contains("restaurant") || lower.contains("cafe") || lower.contains("hotel") || lower.contains("pizza") || lower.contains("food") -> {
                category = "Food & Dining"
            }
            lower.contains("supermarket") || lower.contains("grocer") || lower.contains("reliance fresh") || lower.contains("dmart") || lower.contains("bigbasket") || lower.contains("blinkit") || lower.contains("groceries") -> {
                category = "Groceries"
            }
            lower.contains("petrol") || lower.contains("fuel") || lower.contains("diesel") || lower.contains("shell") || lower.contains("hpcl") || lower.contains("bpcl") || lower.contains("refuel") -> {
                category = "Fuel / Gas"
            }
            lower.contains("amazon") || lower.contains("flipkart") || lower.contains("myntra") || lower.contains("ajio") || lower.contains("shopping") || lower.contains("apparel") || lower.contains("clothing") -> {
                category = "Shopping"
            }
            lower.contains("netflix") || lower.contains("prime video") || lower.contains("spotify") || lower.contains("cinema") || lower.contains("movie") || lower.contains("hotstar") || lower.contains("entertainment") -> {
                category = "Entertainment"
            }
            lower.contains("jio") || lower.contains("airtel") || lower.contains("recharge") || lower.contains("bill") || lower.contains("electricity") || lower.contains("broadband") -> {
                category = "Recharge & Bills"
            }
            lower.contains("medical") || lower.contains("health") || lower.contains("hospital") || lower.contains("pharmacy") || lower.contains("doctor") || lower.contains("medicine") -> {
                category = "Medical & Health"
            }
            lower.contains("uber") || lower.contains("ola") || lower.contains("cab") || lower.contains("commute") || lower.contains("metro") || lower.contains("railway") || lower.contains("irctc") || lower.contains("travel") -> {
                category = "Travel & Cab"
            }
            lower.contains("salary") || lower.contains("stipend") || lower.contains("payslip") -> {
                category = "Salary"
            }
        }

        return ParsedSms(
            amount = amount,
            type = if (isIncome) "INCOME" else "EXPENSE",
            guessedCategory = category,
            payeeOrMerchant = cleanPayee
        )
    }
}
