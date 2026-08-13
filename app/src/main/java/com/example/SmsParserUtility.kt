package com.example

import android.util.Log
import java.text.Normalizer
import java.util.regex.Pattern

object SmsParserUtility {
    private const val TAG = "SmsParserUtility"

    private fun logDebug(tag: String, message: String) {
        try {
            Log.d(tag, message)
        } catch (e: Throwable) {
            println("[$tag] $message")
        }
    }

    data class ParsedResult(
        val amount: Double,
        val type: String, // "EXPENSE" or "INCOME"
        val guessedCategory: String,
        val payeeOrMerchant: String
    )

    /**
     * Deep normalize stylized/mathematical alphanumeric Unicode styles into standard ASCII.
     */
    fun normalizeUnicode(input: String?): String {
        if (input == null) return ""
        
        // 1. NFKD normalization solves standard compatibility variations
        val decomposed = try {
            Normalizer.normalize(input, Normalizer.Form.NFKD)
        } catch (e: Exception) {
            input
        }

        // 2. Map mathematical alphanumeric symbols U+1D400..U+1D7FF to standard alphanumeric
        val sb = StringBuilder()
        var i = 0
        val len = decomposed.length
        while (i < len) {
            val codePoint = decomposed.codePointAt(i)
            val charCount = Character.charCount(codePoint)

            val replacement = getStandardAlphanumericChar(codePoint)
            if (replacement != null) {
                sb.append(replacement)
            } else {
                if (charCount == 1) {
                    sb.append(decomposed[i])
                } else {
                    sb.append(decomposed[i])
                    sb.append(decomposed[i + 1])
                }
            }
            i += charCount
        }
        return sb.toString()
    }

    private fun getStandardAlphanumericChar(codePoint: Int): Char? {
        // Uppercase blocks (A-Z)
        val uppercaseRanges = listOf(
            0x1D400..0x1D419, // Bold
            0x1D434..0x1D44D, // Italic
            0x1D468..0x1D481, // Bold Italic
            0x1D4D0..0x1D4E9, // Bold Script
            0x1D504..0x1D51D, // Fraktur
            0x1D538..0x1D551, // Double-struck
            0x1D56C..0x1D585, // Bold Fraktur
            0x1D5A0..0x1D5B9, // Sans-serif Regular
            0x1D5CA..0x1D5E3, // Sans-serif Italic
            0x1D5D4..0x1D5ED, // Sans-serif Bold
            0x1D608..0x1D621, // Sans-serif Bold Italic or Italic
            0x1D63C..0x1D655, // Sans-serif Bold Italic
            0x1D670..0x1D689  // Monospace
        )

        for (range in uppercaseRanges) {
            if (codePoint in range) {
                return ('A'.code + (codePoint - range.first)).toChar()
            }
        }

        // Lowercase blocks (a-z)
        val lowercaseRanges = listOf(
            0x1D41A..0x1D433, // Bold
            0x1D44E..0x1D467, // Italic
            0x1D482..0x1D49B, // Bold Italic
            0x1D4EA..0x1D503, // Bold Script
            0x1D51E..0x1D537, // Fraktur
            0x1D552..0x1D56B, // Double-struck
            0x1D586..0x1D59F, // Bold Fraktur
            0x1D5BA..0x1D5D3, // Sans-serif Regular
            0x1D5E4..0x1D5FD, // Sans-serif Italic / Bold Italic
            0x1D5EE..0x1D607, // Sans-serif Bold
            0x1D622..0x1D63B, // Sans-serif Italic
            0x1D656..0x1D66F, // Sans-serif Bold Italic
            0x1D68A..0x1D6A3  // Monospace
        )

        for (range in lowercaseRanges) {
            if (codePoint in range) {
                return ('a'.code + (codePoint - range.first)).toChar()
            }
        }

        // Digits blocks (0-9)
        val digitRanges = listOf(
            0x1D7CE..0x1D7D7, // Bold
            0x1D7D8..0x1D7E1, // Double-struck
            0x1D7E2..0x1D7EB, // Sans-serif
            0x1D7EC..0x1D7F5, // Sans-serif Bold
            0x1D7F6..0x1D7FF  // Monospace
        )

        for (range in digitRanges) {
            if (codePoint in range) {
                return ('0'.code + (codePoint - range.first)).toChar()
            }
        }

        return null
    }

    /**
     * Parses the SMS message body. Logs detailed validation/parsing check results.
     */
    fun parseSms(body: String): ParsedResult? {
        if (body.isBlank()) {
            logDebug(TAG, "Validation failed: Message body is blank.")
            return null
        }

        val cleanBody = normalizeUnicode(body)
        
        // Normalize line breaks, non-breaking spaces \u00A0, and extra whitespaces to clean single spaces
        val normalized = cleanBody.replace("\u00A0", " ")
                             .replace("\r", " ")
                             .replace("\n", " ")
                             .replace(Regex("\\s+"), " ")
                             .trim()

        val lower = normalized.lowercase()

        // Skip OTP or standard authorization code alerts
        if (lower.contains("otp") || lower.contains("verification code") || lower.contains("security code") || lower.contains("one time password") || lower.contains("one-time password")) {
            logDebug(TAG, "Validation failed: Identified as OTP/verification alert. SMS: '$normalized'")
            return null
        }

        // Core transaction keyword filter
        val hasCoreKeywords = lower.contains("spent") || lower.contains("spend") ||
                             lower.contains("debit") || lower.contains("debited") ||
                             lower.contains("credit") || lower.contains("credited") || lower.contains("cridet")
        if (!hasCoreKeywords) {
            logDebug(TAG, "Validation failed: No core transactional keywords (spent, debited, credited, etc.) found. SMS: '$normalized'")
            return null
        }

        // Clean standard passive credit terms to avoid misinterpreting "credit card" or "limit" as INCOME
        var checkIncomeBody = lower
        checkIncomeBody = checkIncomeBody.replace("credit card", "card")
        checkIncomeBody = checkIncomeBody.replace("credit limit", "limit")
        checkIncomeBody = checkIncomeBody.replace("credit bal", "bal")
        checkIncomeBody = checkIncomeBody.replace("credit availed", "availed")
        checkIncomeBody = checkIncomeBody.replace("available credit", "avail_bal")
        checkIncomeBody = checkIncomeBody.replace("credit score", "score")

        // Passive non-transaction alerts check: "available balance", "outstanding", etc.
        val hasPassiveKeywords = lower.contains("statement") ||
                                 lower.contains("minimum due") ||
                                 lower.contains("outstanding") ||
                                 lower.contains("bill summary") ||
                                 lower.contains("payment due") ||
                                 lower.contains("due date")
                                 
        val hasActiveVerbs = lower.contains("spent") ||
                             lower.contains("debited") ||
                             lower.contains("credited") ||
                             lower.contains("paid") ||
                             lower.contains("payment of") ||
                             lower.contains("received") ||
                             lower.contains("sent") ||
                             lower.contains("withdrawn") ||
                             lower.contains("deposit") ||
                             lower.contains("added") ||
                             lower.contains("refund") ||
                             lower.contains("transfer")

        if (hasPassiveKeywords && !hasActiveVerbs) {
            logDebug(TAG, "Validation failed: Contains passive account balance keywords with no active transaction verbs. SMS: '$normalized'")
            return null
        }

        // Common transaction words to scan to determine expense vs income
        val isExpense = lower.contains("debit") || 
                        lower.contains("debited") ||
                        lower.contains("spend") || 
                        lower.contains("spent") || 
                        lower.contains("paid") || 
                        lower.contains("payment") || 
                        lower.contains("pay") || 
                        lower.contains("sent") || 
                        lower.contains("txn") ||
                        lower.contains("trxn") ||
                        lower.contains("trx") ||
                        lower.contains("used") ||
                        lower.contains("transaction") ||
                        lower.contains("transfer") ||
                        lower.contains("withdrawn") ||
                        lower.contains("withdraw") ||
                        lower.contains("withdrew") ||
                        lower.contains("charge") ||
                        lower.contains("purchase") ||
                        lower.contains("bought")
                        
        val isIncome = checkIncomeBody.contains("credit") || 
                        checkIncomeBody.contains("credited") || 
                        lower.contains("receive") || 
                        lower.contains("received") || 
                        lower.contains("deposit") ||
                        lower.contains("deposited") ||
                        lower.contains("reimburse") ||
                        lower.contains("refund") ||
                        lower.contains("salary") ||
                        lower.contains("added")

        val isSbiCardSpend = lower.contains("spent") && (lower.contains("sbi credit card") || lower.contains("sbicard") || lower.contains("sbi card") || lower.contains("ending with"))
        val forceExpense = isSbiCardSpend

        if (!isExpense && !isIncome && !forceExpense) {
            logDebug(TAG, "Validation failed: Could not confidently classify as EXPENSE or INCOME. SMS: '$normalized'")
            return null
        }

        // Filter out failed, declined, or cancelled messages to avoid false entries
        if (lower.contains("failed") || lower.contains("declined") || lower.contains("cancelled") || lower.contains("unsuccessful")) {
            logDebug(TAG, "Validation failed: Transaction flagged as failed, declined, or unsuccessful. SMS: '$normalized'")
            return null
        }

        // Determine correct category and transaction type
        val finalType = when {
            forceExpense -> "EXPENSE"
            isIncome && !isExpense -> "INCOME"
            isExpense && !isIncome -> "EXPENSE"
            // Disambiguate if both triggers matched (e.g. "spent Rs 10 on credit card, available credit is...")
            isExpense && isIncome -> {
                if (lower.contains("credited") || lower.contains("refund") || lower.contains("received") || lower.contains("salary") || lower.contains("deposited")) {
                    "INCOME"
                } else {
                    "EXPENSE"
                }
            }
            else -> "EXPENSE"
        }

        // MULTI-FALLBACK AMOUNT EXTRACTION
        var amount: Double? = null

        // Pattern 1: Standard Indian bank prefix (e.g., "Rs. 500", "Rs 500.00", "INR 200", "₹ 450", "Rs.500")
        val prefixRegex = Pattern.compile("(?:rs\\.?|inr|₹|re\\.?|rupees?)\\s*([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
        val prefixMatcher = prefixRegex.matcher(normalized)
        if (prefixMatcher.find()) {
            val amountStr = prefixMatcher.group(1)?.replace(",", "")
            amount = amountStr?.toDoubleOrNull()
        }

        // Dedicated SBI Card / generic Card custom pattern (e.g., "Rs.55.00 spent on your SBI Credit Card", "Rs.1,470.00 spent")
        if (amount == null || isSbiCardSpend) {
            val sbiPattern = Pattern.compile("(?:rs\\.?|inr|₹|re\\.?|rupees?)\\s*([\\d,]+(?:\\.\\d{1,2})?)\\s+spent", Pattern.CASE_INSENSITIVE)
            val sbiMatcher = sbiPattern.matcher(normalized)
            if (sbiMatcher.find()) {
                val amountStr = sbiMatcher.group(1)?.replace(",", "")
                val parsedSbiAmount = amountStr?.toDoubleOrNull()
                if (parsedSbiAmount != null) {
                    amount = parsedSbiAmount
                }
            }
        }

        // Pattern 2: Postfix currency (e.g., "500 Rs", "200 INR", "100 Rupees")
        if (amount == null) {
            val postfixRegex = Pattern.compile("([\\d,]+(?:\\.\\d{1,2})?)\\s*(?:rs\\.?|inr|₹|re\\.?|rupees?)", Pattern.CASE_INSENSITIVE)
            val postfixMatcher = postfixRegex.matcher(normalized)
            if (postfixMatcher.find()) {
                val amountStr = postfixMatcher.group(1)?.replace(",", "")
                amount = amountStr?.toDoubleOrNull()
            }
        }

        // Pattern 3: Clear Transaction Verb followed by bare digits (e.g., "spent 500", "debited 2000", "sent 20.00")
        if (amount == null) {
            val verbRegex = Pattern.compile("(?:spent|spent\\s+of|debited|debited\\s+for|credited|credited\\s+with|paid|paid\\s+to|sent|received|transfer\\s+of|amt\\.?|amount\\.?|amount\\s+of)\\s+([\\d,]+(?:\\.\\d{1,2})?)", Pattern.CASE_INSENSITIVE)
            val verbMatcher = verbRegex.matcher(normalized)
            if (verbMatcher.find()) {
                val amountStr = verbMatcher.group(1)?.replace(",", "")
                amount = amountStr?.toDoubleOrNull()
            }
        }

        if (amount == null) {
            logDebug(TAG, "Validation failed: Could not successfully match any valid transaction amounts. SMS: '$normalized'")
            return null
        }

        // MERCHANT / PAYEE EXTRACTION
        var payee = "Merchant"
        val atRegex = Pattern.compile("at\\s+([A-Za-z0-9\\s\\-&\\.\\'_\\*/@]+)", Pattern.CASE_INSENSITIVE)
        val toRegex = Pattern.compile("to\\s+([A-Za-z0-9\\s\\-&\\.\\'_\\*/@]+)", Pattern.CASE_INSENSITIVE)
        val fromRegex = Pattern.compile("from\\s+([A-Za-z0-9\\s\\-&\\.\\'_\\*/@]+)", Pattern.CASE_INSENSITIVE)
        val infoRegex = Pattern.compile("info[:*\\s]+([A-Za-z0-9\\s\\-&\\.\\'_\\*/@]+)", Pattern.CASE_INSENSITIVE)

        if (atRegex.matcher(normalized).let { if (it.find()) { payee = it.group(1) ?: "Merchant"; true } else false }) {
            // Found
        } else if (toRegex.matcher(normalized).let { if (it.find()) { payee = it.group(1) ?: "Merchant"; true } else false }) {
            // Found
        } else if (fromRegex.matcher(normalized).let { if (it.find()) { payee = it.group(1) ?: "Merchant"; true } else false }) {
            // Found
        } else if (infoRegex.matcher(normalized).let { if (it.find()) { payee = it.group(1) ?: "Merchant"; true } else false }) {
            // Found
        }

        // Clean trailing transaction jargon
        val stopWords = listOf("ref", "on", "txn", "trxn", "is", "using", "date", "via", "at", "to", "card", "closing", "avail", "bal", "limit", "by", "for", "with")
        var cleanPayee = payee.trim()
        for (word in stopWords) {
            if (cleanPayee.lowercase().contains(" $word")) {
                cleanPayee = cleanPayee.substring(0, cleanPayee.lowercase().indexOf(" $word")).trim()
            }
        }
        
        // Strip UPI handles and formatting terms like VPA, @upi, @okaxis etc.
        cleanPayee = cleanPayee.replace(Regex("(?i)\\bVPA\\b"), "")
                           .replace(Regex("@[A-Za-z0-9\\.\\-_]+"), "")
                           .replace(Regex("[^A-Za-z0-9\\s\\-&\\.\\'_/]"), "")
                           .trim()

        if (cleanPayee.isBlank()) {
            cleanPayee = if (finalType == "EXPENSE") "Merchant Spend" else "External Credit"
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
            lower.contains("medical") || lower.contains("health") || lower.contains("hospital") || lower.contains("pharmacy") || lower.contains("doctor") || lower.contains("medicine") || lower.contains("apollo") -> {
                category = "Medical & Health"
            }
            lower.contains("uber") || lower.contains("ola") || lower.contains("cab") || lower.contains("commute") || lower.contains("metro") || lower.contains("railway") || lower.contains("irctc") || lower.contains("travel") -> {
                category = "Travel & Cab"
            }
            lower.contains("salary") || lower.contains("stipend") || lower.contains("payslip") || lower.contains("employer") -> {
                category = "Salary"
            }
            lower.contains("saving") || lower.contains("fixed deposit") || lower.contains("fd") -> {
                category = "Savings"
            }
            lower.contains("invest") || lower.contains("mutual fund") || lower.contains("sip ") || lower.contains("stocks") || lower.contains("shares") || lower.contains("groww") || lower.contains("zerodha") -> {
                category = "Investment"
            }
        }

        // Apply dynamic learned rule correction if we have one
        val learned = getLearnedCategory(cleanPayee)
        if (learned != null) {
            category = learned
        }

        logDebug(TAG, "Validation passed: Successfully parsed transaction. Amount: $amount, Type: $finalType, Payee: $cleanPayee")
        return ParsedResult(
            amount = amount,
            type = finalType,
            guessedCategory = category,
            payeeOrMerchant = cleanPayee
        )
    }

    private val learnedRules = java.util.concurrent.ConcurrentHashMap<String, String>()

    fun initRules(context: android.content.Context) {
        try {
            val sp = context.getSharedPreferences("sms_parser_learned_rules", android.content.Context.MODE_PRIVATE)
            sp.all.forEach { (k, v) ->
                if (v is String) {
                    learnedRules[k] = v
                }
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Error initializing rules", e)
        }
    }

    fun learnRule(context: android.content.Context, payee: String, category: String) {
        val cleanKey = payee.lowercase().trim()
        if (cleanKey.isNotEmpty()) {
            learnedRules[cleanKey] = category
            try {
                val sp = context.getSharedPreferences("sms_parser_learned_rules", android.content.Context.MODE_PRIVATE)
                sp.edit().putString(cleanKey, category).apply()
            } catch (e: Throwable) {
                Log.e(TAG, "Error learning rule", e)
            }
        }
    }

    fun getLearnedCategory(payee: String): String? {
        return learnedRules[payee.lowercase().trim()]
    }
}
