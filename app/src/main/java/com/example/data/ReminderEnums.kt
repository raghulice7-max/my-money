package com.example.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

enum class ReminderType(
    val code: String,
    val displayName: String,
    val themeColor: Color,
    val icon: ImageVector
) {
    SIP("SIP", "SIP Investment", Color(0xFF9C27B0), Icons.Default.TrendingUp),
    RD("RD", "Recurring Deposit", Color(0xFF2E7D32), Icons.Default.Savings),
    BILL("BILL", "Bill / Utility", Color(0xFF0288D1), Icons.Default.ReceiptLong),
    OTHER("OTHER", "Other Commitment", Color(0xFFE65100), Icons.Default.NotificationsActive);

    companion object {
        fun fromCode(code: String): ReminderType {
            return values().find { it.code.equals(code, ignoreCase = true) } ?: SIP
        }
    }
}

enum class ReminderCategory(
    val code: String,
    val displayName: String
) {
    INVESTMENT("Investment", "Investment"),
    SAVINGS("Savings", "Savings"),
    GROCERIES("Groceries", "Groceries"),
    FUEL("Fuel / Gas", "Fuel / Gas"),
    SHOPPING("Shopping", "Shopping"),
    ENTERTAINMENT("Entertainment", "Entertainment"),
    BILLS("Recharge & Bills", "Recharge & Bills"),
    OTHER("Other", "Other");

    companion object {
        fun fromCode(code: String): ReminderCategory {
            return values().find { it.code.equals(code, ignoreCase = true) } ?: INVESTMENT
        }
    }
}
