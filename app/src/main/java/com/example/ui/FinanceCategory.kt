package com.example.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class CategoryStyle(
    val name: String,
    val color: Color,
    val icon: ImageVector,
    val isExpense: Boolean
)

object FinanceCategory {
    val SALARY = "Salary"
    val INVESTMENT = "Investment"
    val SAVINGS = "Savings"
    val GIFT = "Gift"
    val BUSINESS_FREELANCE = "Business & Freelance"
    val FOOD_DINING = "Food & Dining"
    val GROCERIES = "Groceries"
    val FUEL_GAS = "Fuel / Gas"
    val SHOPPING = "Shopping"
    val ENTERTAINMENT = "Entertainment"
    val RENT_UTILITIES = "Rent / Utilities"
    val MEDICAL_HEALTH = "Medical & Health"
    val RECHARGE_BILL = "Recharge & Bills"
    val TRAVEL_CAB = "Travel & Cab"
    val EDUCATION = "Education & Fees"
    val SUBSCRIPTIONS = "Subscriptions"
    val INSURANCE_TAX = "Insurance & Taxes"
    val OTHER = "Other"

    val categories = listOf(
        CategoryStyle(SALARY, Color(0xFF4CAF50), Icons.Default.AttachMoney, false),
        CategoryStyle(INVESTMENT, Color(0xFF009688), Icons.Default.TrendingUp, false),
        CategoryStyle(SAVINGS, Color(0xFF3F51B5), Icons.Default.Savings, false),
        CategoryStyle(GIFT, Color(0xFFFF9800), Icons.Default.CardGiftcard, false),
        CategoryStyle(BUSINESS_FREELANCE, Color(0xFF00C853), Icons.Default.Computer, false),
        CategoryStyle(FOOD_DINING, Color(0xFFFF5722), Icons.Default.Restaurant, true),
        CategoryStyle(GROCERIES, Color(0xFFFFC107), Icons.Default.ShoppingCart, true),
        CategoryStyle(FUEL_GAS, Color(0xFF2196F3), Icons.Default.LocalGasStation, true),
        CategoryStyle(SHOPPING, Color(0xFF9C27B0), Icons.Default.LocalMall, true),
        CategoryStyle(ENTERTAINMENT, Color(0xFF00BCD4), Icons.Default.LocalActivity, true),
        CategoryStyle(RENT_UTILITIES, Color(0xFF3F51B5), Icons.Default.Home, true),
        CategoryStyle(MEDICAL_HEALTH, Color(0xFFEF5350), Icons.Default.MedicalServices, true),
        CategoryStyle(RECHARGE_BILL, Color(0xFF26A69A), Icons.Default.PhoneAndroid, true),
        CategoryStyle(TRAVEL_CAB, Color(0xFF03A9F4), Icons.Default.Commute, true),
        CategoryStyle(EDUCATION, Color(0xFF8D6E63), Icons.Default.School, true),
        CategoryStyle(SUBSCRIPTIONS, Color(0xFFAB47BC), Icons.Default.Subscriptions, true),
        CategoryStyle(INSURANCE_TAX, Color(0xFF607D8B), Icons.Default.Security, true),
        CategoryStyle(INVESTMENT, Color(0xFF009688), Icons.Default.TrendingUp, true),
        CategoryStyle(SAVINGS, Color(0xFF3F51B5), Icons.Default.Savings, true),
        CategoryStyle(OTHER, Color(0xFF9E9E9E), Icons.Default.Category, true)
    )

    fun getStyleFor(name: String): CategoryStyle {
        return categories.find { it.name.lowercase() == name.lowercase() }
            ?: CategoryStyle(name, Color(0xFF9E9E9E), Icons.Default.Category, true)
    }
}
