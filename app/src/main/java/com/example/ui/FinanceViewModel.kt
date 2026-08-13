package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.util.Calendar

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = FinanceRepository(database.financeDao())

    private val sharedPrefs = application.getSharedPreferences("finance_prefs", android.content.Context.MODE_PRIVATE)

    val userNameFlow = kotlinx.coroutines.flow.MutableStateFlow(sharedPrefs.getString("user_name", "Explorer") ?: "Explorer")
    val userEmailFlow = kotlinx.coroutines.flow.MutableStateFlow(sharedPrefs.getString("user_email", "raghulice7@gmail.com") ?: "raghulice7@gmail.com")
    val userWantsSummaryFlow = kotlinx.coroutines.flow.MutableStateFlow(sharedPrefs.getBoolean("user_wants_summary", true))

    val appThemeFlow = kotlinx.coroutines.flow.MutableStateFlow(sharedPrefs.getString("app_theme", "SYSTEM") ?: "SYSTEM")
    val appFontFamilyFlow = kotlinx.coroutines.flow.MutableStateFlow(sharedPrefs.getString("app_font_family", "DEFAULT") ?: "DEFAULT")
    val appFontSizeFlow = kotlinx.coroutines.flow.MutableStateFlow(sharedPrefs.getString("app_font_size", "NORMAL") ?: "NORMAL")

    fun updateProfile(name: String, email: String, wantsSummary: Boolean) {
        sharedPrefs.edit()
            .putString("user_name", name)
            .putString("user_email", email)
            .putBoolean("user_wants_summary", wantsSummary)
            .apply()
        userNameFlow.value = name
        userEmailFlow.value = email
        userWantsSummaryFlow.value = wantsSummary
    }

    fun updateTheme(theme: String) {
        sharedPrefs.edit().putString("app_theme", theme).apply()
        appThemeFlow.value = theme
    }

    fun updateFontFamily(fontFamily: String) {
        sharedPrefs.edit().putString("app_font_family", fontFamily).apply()
        appFontFamilyFlow.value = fontFamily
    }

    fun updateFontSize(fontSize: String) {
        sharedPrefs.edit().putString("app_font_size", fontSize).apply()
        appFontSizeFlow.value = fontSize
    }

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val budgets: StateFlow<List<BudgetEntity>> = repository.allBudgets
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val fuelEntries: StateFlow<List<FuelEntryEntity>> = repository.allFuelEntries
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pendingTransactions: StateFlow<List<PendingSmsTransactionEntity>> = repository.allPendingTransactions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val investments: StateFlow<List<InvestmentEntity>> = repository.allInvestments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val goals: StateFlow<List<GoalEntity>> = repository.allGoals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recurringReminders: StateFlow<List<RecurringReminderEntity>> = repository.allRecurringReminders
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Pre-populate database with default items if empty
        viewModelScope.launch {
            val existingTx = repository.allTransactions.first()
            if (existingTx.isEmpty()) {
                prepopulateDatabase()
            }

            val existingReminders = repository.allRecurringReminders.first()
            if (existingReminders.isEmpty()) {
                repository.insertRecurringReminder(
                    RecurringReminderEntity(
                        name = "HDFC Index Mutual Fund SIP",
                        amount = 5000.0,
                        dayOfMonth = 5,
                        type = "SIP",
                        lastPaidDate = null,
                        category = "Investment",
                        notes = "Monthly equity investment"
                    )
                )
                repository.insertRecurringReminder(
                    RecurringReminderEntity(
                        name = "SBI Recurring Deposit (RD)",
                        amount = 3000.0,
                        dayOfMonth = 15,
                        type = "RD",
                        lastPaidDate = null,
                        category = "Savings",
                        notes = "Emergency cash reserve"
                    )
                )
            }

            // Seed sample pending SMS transactions ONLY ONCE using a persistent preference flag
            val hasSeededSms = sharedPrefs.getBoolean("pending_sms_seeded", false)
            if (!hasSeededSms) {
                repository.insertPendingTransaction(
                    PendingSmsTransactionEntity(
                        amount = 450.0,
                        type = "EXPENSE",
                        senderAddress = "HDFCBK",
                        messageBody = "Alert: Your HDFC Bank Card ending 1234 was spent for INR 450.00 at SWIGGY on 21-May-2026. Txn id 948210.",
                        dateMillis = System.currentTimeMillis() - 3600000, // 1 hour ago
                        initialCategory = "Food & Dining",
                        payeeOrMerchant = "Swiggy"
                    )
                )
                repository.insertPendingTransaction(
                    PendingSmsTransactionEntity(
                        amount = 120.0,
                        type = "EXPENSE",
                        senderAddress = "SBI-UPI",
                        messageBody = "Txn of Rs. 120.00 on SBI A/c XX721 to Chai Tapri Ref: 629158.",
                        dateMillis = System.currentTimeMillis() - 7200000, // 2 hours ago
                        initialCategory = "Other",
                        payeeOrMerchant = "Chai Tapri"
                    )
                )
                sharedPrefs.edit().putBoolean("pending_sms_seeded", true).apply()
            }
        }
    }

    private suspend fun prepopulateDatabase() {
        // Default budgets in INR
        repository.insertBudget(BudgetEntity("TOTAL", 45000.0))
        repository.insertBudget(BudgetEntity("Food & Dining", 10000.0))
        repository.insertBudget(BudgetEntity("Groceries", 6000.0))
        repository.insertBudget(BudgetEntity("Fuel / Gas", 8000.0))
        repository.insertBudget(BudgetEntity("Shopping", 8000.0))

        // Seed 3 past months + current month (total 4 months of records)
        for (i in 3 downTo 0) {
            seedMonthData(i)
        }
    }

    private suspend fun seedMonthData(monthsAgo: Int) {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -monthsAgo)
        val monthLabel = getMonthName(cal.get(Calendar.MONTH))

        // 1st of the month: Salary (INCOME)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        repository.insertTransaction(
            TransactionEntity(
                amount = 75000.0,
                type = "INCOME",
                category = "Salary",
                payeeOrSource = "Acme Corp Salary",
                dateMillis = cal.timeInMillis,
                note = "Monthly salary deposit for $monthLabel"
            )
        )

        // 3rd of the month: House Rent
        cal.set(Calendar.DAY_OF_MONTH, 3)
        repository.insertTransaction(
            TransactionEntity(
                amount = 12000.0,
                type = "EXPENSE",
                category = "Rent / Utilities",
                payeeOrSource = "Avenue Heights Rent",
                dateMillis = cal.timeInMillis,
                note = "Apartment Rent for $monthLabel"
            )
        )

        // 5th of the month: Electricity & Water Utilities
        cal.set(Calendar.DAY_OF_MONTH, 5)
        repository.insertTransaction(
            TransactionEntity(
                amount = 2500.0,
                type = "EXPENSE",
                category = "Rent / Utilities",
                payeeOrSource = "City Power & Water",
                dateMillis = cal.timeInMillis,
                note = "$monthLabel Utility bill"
            )
        )

        // 7th of the month: Car Refuel 1
        cal.set(Calendar.DAY_OF_MONTH, 7)
        val carOdo1 = 11300.0 + (3 - monthsAgo) * 1200.0
        val fuelId1 = repository.insertFuelEntry(
            FuelEntryEntity(
                amountPaid = 2800.0,
                volumeLiters = 31.0,
                odometerReading = carOdo1,
                dateMillis = cal.timeInMillis,
                note = "Highway Petrol Pump",
                vehicleType = "CAR"
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                amount = 2800.0,
                type = "EXPENSE",
                category = "Fuel / Gas",
                payeeOrSource = "Car Refuel",
                dateMillis = cal.timeInMillis,
                note = "[Fuel Log #$fuelId1] Refueled (Car) 31.0L at Odometer $carOdo1 km ($monthLabel)"
            )
        )

        // 10th of the month: Weekly Groceries
        cal.set(Calendar.DAY_OF_MONTH, 10)
        repository.insertTransaction(
            TransactionEntity(
                amount = 3200.0,
                type = "EXPENSE",
                category = "Groceries",
                payeeOrSource = "Supermarket Fresh",
                dateMillis = cal.timeInMillis,
                note = "Monthly grocery stock 1"
            )
        )

        // 14th of the month: Dinner / Food outing
        cal.set(Calendar.DAY_OF_MONTH, 14)
        repository.insertTransaction(
            TransactionEntity(
                amount = 1800.0,
                type = "EXPENSE",
                category = "Food & Dining",
                payeeOrSource = "Bella Italia Restaurant",
                dateMillis = cal.timeInMillis,
                note = "Weekend dinner"
            )
        )

        // 18th of the month: Shopping / Apparel
        cal.set(Calendar.DAY_OF_MONTH, 18)
        repository.insertTransaction(
            TransactionEntity(
                amount = 4500.0,
                type = "EXPENSE",
                category = "Shopping",
                payeeOrSource = "Fashion Arcade Hub",
                dateMillis = cal.timeInMillis,
                note = "Apparel shopping"
            )
        )

        // 20th of the month: Bike Refuel
        cal.set(Calendar.DAY_OF_MONTH, 20)
        val bikeOdo = 3800.0 + (3 - monthsAgo) * 300.0
        val fuelId2 = repository.insertFuelEntry(
            FuelEntryEntity(
                amountPaid = 400.0,
                volumeLiters = 4.2,
                odometerReading = bikeOdo,
                dateMillis = cal.timeInMillis,
                note = "Express fuels local pump",
                vehicleType = "BIKE"
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                amount = 400.0,
                type = "EXPENSE",
                category = "Fuel / Gas",
                payeeOrSource = "Bike Refuel",
                dateMillis = cal.timeInMillis,
                note = "[Fuel Log #$fuelId2] Refueled (Bike) 4.2L at Odometer $bikeOdo km ($monthLabel)"
            )
        )

        // 22nd of the month: Car Refuel 2
        cal.set(Calendar.DAY_OF_MONTH, 22)
        val carOdo2 = carOdo1 + 450.0
        val fuelId3 = repository.insertFuelEntry(
            FuelEntryEntity(
                amountPaid = 3200.0,
                volumeLiters = 35.0,
                odometerReading = carOdo2,
                dateMillis = cal.timeInMillis,
                note = "Highway Petrol Pump",
                vehicleType = "CAR"
            )
        )
        repository.insertTransaction(
            TransactionEntity(
                amount = 3200.0,
                type = "EXPENSE",
                category = "Fuel / Gas",
                payeeOrSource = "Car Refuel",
                dateMillis = cal.timeInMillis,
                note = "[Fuel Log #$fuelId3] Refueled (Car) 35.0L at Odometer $carOdo2 km ($monthLabel)"
            )
        )

        // 25th of the month: Weekly Groceries 2
        cal.set(Calendar.DAY_OF_MONTH, 25)
        repository.insertTransaction(
            TransactionEntity(
                amount = 3500.0,
                type = "EXPENSE",
                category = "Groceries",
                payeeOrSource = "Local Corner Store",
                dateMillis = cal.timeInMillis,
                note = "Monthly grocery stock 2"
            )
        )
    }

    private fun getMonthName(month: Int): String {
        return listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")[month]
    }

    fun addTransaction(
        amount: Double,
        type: String,
        category: String,
        payeeOrSource: String,
        note: String,
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    amount = amount,
                    type = type,
                    category = category,
                    payeeOrSource = payeeOrSource,
                    dateMillis = dateMillis,
                    note = note
                )
            )
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.insertTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)

            // If the transaction is a fuel log, delete the corresponding fuel log entry if linked!
            if (transaction.category == "Fuel / Gas" && transaction.note.contains("[Fuel Log #")) {
                try {
                    val idStr = transaction.note.substringAfter("[Fuel Log #").substringBefore("]")
                    val id = idStr.toIntOrNull()
                    if (id != null) {
                        repository.deleteFuelEntryById(id)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun clearAllTransactions() {
        viewModelScope.launch {
            repository.clearAllTransactions()
        }
    }

    fun addFuelEntry(
        amountPaid: Double,
        volumeLiters: Double,
        odometerReading: Double,
        note: String,
        vehicleType: String,
        dateMillis: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            // 1. Insert Fuel entry
            val fuelEntryId = repository.insertFuelEntry(
                FuelEntryEntity(
                    amountPaid = amountPaid,
                    volumeLiters = volumeLiters,
                    odometerReading = odometerReading,
                    dateMillis = dateMillis,
                    note = note,
                    vehicleType = vehicleType
                )
            )

            // 2. Insert matching transaction
            val modeName = if (vehicleType == "BIKE") "Bike" else "Car"
            val transactionNote = "[Fuel Log #$fuelEntryId] Refueled ($modeName) ${volumeLiters}L at Odometer $odometerReading"
            repository.insertTransaction(
                TransactionEntity(
                    amount = amountPaid,
                    type = "EXPENSE",
                    category = "Fuel / Gas",
                    payeeOrSource = "$modeName Refuel",
                    dateMillis = dateMillis,
                    note = if (note.isBlank()) transactionNote else "$note ($transactionNote)"
                )
            )
        }
    }

    fun updateFuelEntry(
        id: Int,
        amountPaid: Double,
        volumeLiters: Double,
        odometerReading: Double,
        note: String,
        vehicleType: String,
        dateMillis: Long
    ) {
        viewModelScope.launch {
            val updatedEntry = FuelEntryEntity(
                id = id,
                amountPaid = amountPaid,
                volumeLiters = volumeLiters,
                odometerReading = odometerReading,
                dateMillis = dateMillis,
                note = note,
                vehicleType = vehicleType
            )
            repository.insertFuelEntry(updatedEntry)

            // Find matching transaction by checking notes prefix [Fuel Log #ID]
            val txs = repository.allTransactions.first()
            val match = txs.find { it.category == "Fuel / Gas" && it.note.contains("[Fuel Log #$id]") }
            if (match != null) {
                val modeName = if (vehicleType == "BIKE") "Bike" else "Car"
                val transactionNote = "[Fuel Log #$id] Refueled ($modeName) ${volumeLiters}L at Odometer $odometerReading"
                repository.insertTransaction(
                    match.copy(
                        amount = amountPaid,
                        payeeOrSource = "$modeName Refuel",
                        dateMillis = dateMillis,
                        note = if (note.isBlank()) transactionNote else "$note ($transactionNote)"
                    )
                )
            }
        }
    }

    fun deleteFuelEntry(fuelEntry: FuelEntryEntity) {
        viewModelScope.launch {
            repository.deleteFuelEntry(fuelEntry)

            // Find matching transaction by checking notes prefix [Fuel Log #ID]
            val txs = repository.allTransactions.first()
            val match = txs.find { it.category == "Fuel / Gas" && it.note.contains("[Fuel Log #${fuelEntry.id}]") }
            if (match != null) {
                repository.deleteTransaction(match)
            }
        }
    }

    fun setBudget(category: String, limitAmount: Double) {
        viewModelScope.launch {
            repository.insertBudget(
                BudgetEntity(
                    category = category,
                    limitAmount = limitAmount
                )
            )
        }
    }

    fun deleteBudget(category: String) {
        viewModelScope.launch {
            repository.deleteBudgetByCategory(category)
        }
    }

    fun addPendingTransaction(
        amount: Double,
        type: String,
        senderAddress: String,
        messageBody: String,
        initialCategory: String,
        payeeOrMerchant: String
    ) {
        viewModelScope.launch {
            repository.insertPendingTransaction(
                PendingSmsTransactionEntity(
                    amount = amount,
                    type = type,
                    senderAddress = senderAddress,
                    messageBody = messageBody,
                    dateMillis = System.currentTimeMillis(),
                    initialCategory = initialCategory,
                    payeeOrMerchant = payeeOrMerchant
                )
            )
        }
    }

    fun approvePendingTransaction(
        pending: PendingSmsTransactionEntity,
        category: String,
        payeeOrMerchant: String,
        amount: Double,
        note: String
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    amount = amount,
                    type = pending.type,
                    category = category,
                    payeeOrSource = payeeOrMerchant,
                    dateMillis = pending.dateMillis,
                    note = note
                )
            )
            repository.deletePendingTransaction(pending)
        }
    }

    fun discardPendingTransaction(pending: PendingSmsTransactionEntity) {
        viewModelScope.launch {
            repository.deletePendingTransaction(pending)
        }
    }

    fun syncSmsInbox(context: android.content.Context) {
        viewModelScope.launch {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val smsUri = android.net.Uri.parse("content://sms/inbox")
                    val contentResolver = context.contentResolver
                    val cursor = contentResolver.query(
                        smsUri,
                        arrayOf("address", "body", "date"),
                        null,
                        null,
                        "date DESC LIMIT 500"
                    )

                    val currentPendingList = pendingTransactions.value
                    val currentTransactionsList = transactions.value

                    val existingBodies = (currentPendingList.map { it.messageBody } + currentTransactionsList.map { it.note }).toSet()

                    val receiver = com.example.SmsReceiver()
                    cursor?.use { c ->
                        val addressOrdinal = c.getColumnIndex("address")
                        val bodyOrdinal = c.getColumnIndex("body")
                        val dateOrdinal = c.getColumnIndex("date")

                        if (addressOrdinal != -1 && bodyOrdinal != -1 && dateOrdinal != -1) {
                            while (c.moveToNext()) {
                                val sender = c.getString(addressOrdinal) ?: "Unknown"
                                val body = c.getString(bodyOrdinal) ?: ""
                                val dateMillis = c.getLong(dateOrdinal)

                                if (body.isNotBlank()) {
                                    val bodyLower = body.lowercase()
                                    if (bodyLower.contains("otp") || bodyLower.contains("verification code")) continue

                                    val parsedTx = receiver.parseSms(body)
                                    if (parsedTx != null) {
                                        val isDuplicate = existingBodies.contains(body) ||
                                                currentPendingList.any { it.amount == parsedTx.amount && Math.abs(it.dateMillis - dateMillis) < 5000 } ||
                                                currentTransactionsList.any { it.amount == parsedTx.amount && Math.abs(it.dateMillis - dateMillis) < 300000 }

                                        if (!isDuplicate) {
                                            repository.insertPendingTransaction(
                                                PendingSmsTransactionEntity(
                                                    amount = parsedTx.amount,
                                                    type = parsedTx.type,
                                                    senderAddress = sender,
                                                    messageBody = body,
                                                    dateMillis = dateMillis,
                                                    initialCategory = parsedTx.guessedCategory,
                                                    payeeOrMerchant = parsedTx.payeeOrMerchant
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FinanceViewModel", "Error scanning SMS inbox", e)
                }
            }
        }
    }

    // Investment Operations
    fun addInvestment(name: String, type: String, investedAmount: Double, currentValue: Double, dateMillis: Long, note: String = "") {
        viewModelScope.launch {
            repository.insertInvestment(
                InvestmentEntity(
                    name = name,
                    type = type,
                    investedAmount = investedAmount,
                    currentValue = currentValue,
                    dateMillis = dateMillis,
                    note = note
                )
            )
        }
    }

    fun updateInvestment(investment: InvestmentEntity) {
        viewModelScope.launch {
            repository.insertInvestment(investment)
        }
    }

    fun deleteInvestment(investment: InvestmentEntity) {
        viewModelScope.launch {
            repository.deleteInvestment(investment)
        }
    }

    // Goal Operations
    fun addGoal(name: String, targetAmount: Double, currentAmount: Double, targetDateMillis: Long, category: String, note: String = "") {
        viewModelScope.launch {
            repository.insertGoal(
                GoalEntity(
                    name = name,
                    targetAmount = targetAmount,
                    currentAmount = currentAmount,
                    targetDateMillis = targetDateMillis,
                    category = category,
                    note = note
                )
            )
        }
    }

    fun updateGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.insertGoal(goal)
        }
    }

    fun deleteGoal(goal: GoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    fun contributeToGoal(goal: GoalEntity, amount: Double) {
        viewModelScope.launch {
            val updated = goal.copy(currentAmount = goal.currentAmount + amount)
            repository.insertGoal(updated)
            
            // Also optional, we can create an EXPENSE transaction in category "Savings" or "Investment" if they want,
            // but let's keep it clean as direct goal contribution first, and let them choose.
        }
    }

    val reminderPayments: StateFlow<List<ReminderPaymentEntity>> = repository.allReminderPayments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Recurring Reminders Operations
    fun addRecurringReminder(name: String, amount: Double, dayOfMonth: Int, type: String, category: String, notes: String = "") {
        viewModelScope.launch {
            repository.insertRecurringReminder(
                RecurringReminderEntity(
                    name = name,
                    amount = amount,
                    dayOfMonth = dayOfMonth,
                    type = type,
                    lastPaidDate = null,
                    category = category,
                    notes = notes
                )
            )
            com.example.notification.ReminderNotificationScheduler.scheduleReminderWork(
                getApplication(),
                name,
                dayOfMonth,
                amount
            )
        }
    }

    fun updateRecurringReminder(reminder: RecurringReminderEntity) {
        viewModelScope.launch {
            repository.insertRecurringReminder(reminder)
            com.example.notification.ReminderNotificationScheduler.scheduleReminderWork(
                getApplication(),
                reminder.name,
                reminder.dayOfMonth,
                reminder.amount
            )
        }
    }

    fun deleteRecurringReminder(reminder: RecurringReminderEntity) {
        viewModelScope.launch {
            repository.deleteRecurringReminder(reminder)
            repository.deletePaymentsForReminder(reminder.id)
            com.example.notification.ReminderNotificationScheduler.cancelReminderWork(
                getApplication(),
                reminder.name
            )
        }
    }

    fun payRecurringReminder(
        reminder: RecurringReminderEntity,
        onPaymentLogged: (paymentId: Long, previousLastPaidDate: java.time.LocalDate?) -> Unit = { _, _ -> }
    ) {
        viewModelScope.launch {
            val today = java.time.LocalDate.now()
            val previousDate = reminder.lastPaidDate

            // 1. Mark as paid
            val updated = reminder.copy(lastPaidDate = today)
            repository.insertRecurringReminder(updated)

            // 2. Record payment history entity
            val paymentId = repository.insertReminderPayment(
                ReminderPaymentEntity(
                    reminderId = reminder.id,
                    paidDate = today,
                    amount = reminder.amount
                )
            )

            // 3. Log transaction expense
            repository.insertTransaction(
                TransactionEntity(
                    amount = reminder.amount,
                    type = "EXPENSE",
                    category = reminder.category,
                    payeeOrSource = reminder.name,
                    note = "Recurring ${reminder.type} payment",
                    dateMillis = System.currentTimeMillis()
                )
            )

            onPaymentLogged(paymentId, previousDate)
        }
    }

    fun undoPayRecurringReminder(
        reminder: RecurringReminderEntity,
        paymentId: Long,
        previousLastPaidDate: java.time.LocalDate?
    ) {
        viewModelScope.launch {
            // Revert lastPaidDate on reminder
            val reverted = reminder.copy(lastPaidDate = previousLastPaidDate)
            repository.insertRecurringReminder(reverted)

            // Delete payment record
            repository.deleteReminderPaymentById(paymentId)
        }
    }

    suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val txs = transactions.value
        val budg = budgets.value
        val fuel = fuelEntries.value
        val pending = pendingTransactions.value
        val invs = investments.value
        val gls = goals.value

        val backup = AppBackupData(
            transactions = txs,
            budgets = budg,
            fuelEntries = fuel,
            pendingTransactions = pending,
            investments = invs,
            goals = gls
        )

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
        val adapter = moshi.adapter(AppBackupData::class.java)
        adapter.toJson(backup)
    }

    suspend fun importBackupJson(jsonString: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val moshi = Moshi.Builder()
                .add(com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory())
                .build()

            var transactionsList: List<TransactionEntity>? = null
            var budgetsList: List<BudgetEntity>? = null
            var fuelEntriesList: List<FuelEntryEntity>? = null
            var pendingSmsList: List<PendingSmsTransactionEntity>? = null
            var investmentsList: List<InvestmentEntity>? = null
            var goalsList: List<GoalEntity>? = null

            // Fallback 1: Try full AppBackupData parsing
            try {
                val adapter = moshi.adapter(AppBackupData::class.java)
                val backup = adapter.fromJson(jsonString)
                if (backup != null) {
                    transactionsList = backup.transactions
                    budgetsList = backup.budgets
                    fuelEntriesList = backup.fuelEntries
                    pendingSmsList = backup.pendingTransactions
                    investmentsList = backup.investments
                    goalsList = backup.goals
                }
            } catch (e: Exception) {
                android.util.Log.e("FinanceViewModel", "Backup direct parse failed, trying fallbacks", e)
            }

            // Fallback 2: Try parsing as generic Map to extract specific arrays
            if (transactionsList == null) {
                try {
                    val mapType = com.squareup.moshi.Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
                    val mapAdapter = moshi.adapter<Map<String, Any>>(mapType)
                    val rawMap = mapAdapter.fromJson(jsonString)
                    if (rawMap != null) {
                        if (rawMap.containsKey("transactions")) {
                            val txJsonStr = moshi.adapter(Any::class.java).toJson(rawMap["transactions"])
                            val txListType = com.squareup.moshi.Types.newParameterizedType(List::class.java, TransactionEntity::class.java)
                            transactionsList = moshi.adapter<List<TransactionEntity>>(txListType).fromJson(txJsonStr)
                        }
                        if (rawMap.containsKey("budgets")) {
                            val bgJsonStr = moshi.adapter(Any::class.java).toJson(rawMap["budgets"])
                            val bgListType = com.squareup.moshi.Types.newParameterizedType(List::class.java, BudgetEntity::class.java)
                            budgetsList = moshi.adapter<List<BudgetEntity>>(bgListType).fromJson(bgJsonStr)
                        }
                        if (rawMap.containsKey("fuelEntries")) {
                            val flJsonStr = moshi.adapter(Any::class.java).toJson(rawMap["fuelEntries"])
                            val flListType = com.squareup.moshi.Types.newParameterizedType(List::class.java, FuelEntryEntity::class.java)
                            fuelEntriesList = moshi.adapter<List<FuelEntryEntity>>(flListType).fromJson(flJsonStr)
                        }
                        if (rawMap.containsKey("pendingTransactions")) {
                            val ptJsonStr = moshi.adapter(Any::class.java).toJson(rawMap["pendingTransactions"])
                            val ptListType = com.squareup.moshi.Types.newParameterizedType(List::class.java, PendingSmsTransactionEntity::class.java)
                            pendingSmsList = moshi.adapter<List<PendingSmsTransactionEntity>>(ptListType).fromJson(ptJsonStr)
                        }
                        if (rawMap.containsKey("investments")) {
                            val invJsonStr = moshi.adapter(Any::class.java).toJson(rawMap["investments"])
                            val invListType = com.squareup.moshi.Types.newParameterizedType(List::class.java, InvestmentEntity::class.java)
                            investmentsList = moshi.adapter<List<InvestmentEntity>>(invListType).fromJson(invJsonStr)
                        }
                        if (rawMap.containsKey("goals")) {
                            val glJsonStr = moshi.adapter(Any::class.java).toJson(rawMap["goals"])
                            val glListType = com.squareup.moshi.Types.newParameterizedType(List::class.java, GoalEntity::class.java)
                            goalsList = moshi.adapter<List<GoalEntity>>(glListType).fromJson(glJsonStr)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FinanceViewModel", "Fallback 2 parsing failed", e)
                }
            }

            // Fallback 3: Try parsing raw JSON array of transactions directly: [{"id": 405, ...}]
            if (transactionsList == null) {
                try {
                    val txListType = com.squareup.moshi.Types.newParameterizedType(List::class.java, TransactionEntity::class.java)
                    transactionsList = moshi.adapter<List<TransactionEntity>>(txListType).fromJson(jsonString)
                } catch (e: Exception) {
                    android.util.Log.e("FinanceViewModel", "Fallback 3 parsing failed", e)
                }
            }

            // If we successfully retrieved at least one populated list, import it!
            if (transactionsList != null || budgetsList != null || fuelEntriesList != null || pendingSmsList != null || investmentsList != null || goalsList != null) {
                repository.clearAllData()
                repository.insertAllDataBulk(
                    transactions = transactionsList ?: emptyList(),
                    budgets = budgetsList ?: emptyList(),
                    fuelEntries = fuelEntriesList ?: emptyList(),
                    pendingSms = pendingSmsList ?: emptyList(),
                    investments = investmentsList ?: emptyList(),
                    goals = goalsList ?: emptyList()
                )
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class AppBackupData(
    val transactions: List<TransactionEntity>? = emptyList(),
    val budgets: List<BudgetEntity>? = emptyList(),
    val fuelEntries: List<FuelEntryEntity>? = emptyList(),
    val pendingTransactions: List<PendingSmsTransactionEntity>? = emptyList(),
    val investments: List<InvestmentEntity>? = emptyList(),
    val goals: List<GoalEntity>? = emptyList()
)

