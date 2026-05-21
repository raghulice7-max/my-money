package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = FinanceRepository(database.financeDao())

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

    init {
        // Pre-populate database with default items if empty
        viewModelScope.launch {
            val existingTx = repository.allTransactions.first()
            if (existingTx.isEmpty()) {
                prepopulateDatabase()
            }

            // Seed sample pending SMS transaction to make the review flow instantly interactable and previewable
            val existingPending = repository.allPendingTransactions.first()
            if (existingPending.isEmpty()) {
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

        val cal = Calendar.getInstance()

        // Default transactions
        // Income
        cal.add(Calendar.DAY_OF_YEAR, -5)
        repository.insertTransaction(
            TransactionEntity(
                amount = 75000.0,
                type = "INCOME",
                category = "Salary",
                payeeOrSource = "Acme Corp Salary",
                dateMillis = cal.timeInMillis,
                note = "Monthly salary direct deposit"
            )
        )

        // Expenses
        cal.add(Calendar.DAY_OF_YEAR, 1)
        repository.insertTransaction(
            TransactionEntity(
                amount = 2500.0,
                type = "EXPENSE",
                category = "Rent / Utilities",
                payeeOrSource = "City Power & Water",
                dateMillis = cal.timeInMillis,
                note = "Electricity and water bill"
            )
        )

        cal.add(Calendar.DAY_OF_YEAR, 1)
        repository.insertTransaction(
            TransactionEntity(
                amount = 3500.0,
                type = "EXPENSE",
                category = "Groceries",
                payeeOrSource = "Supermarket Fresh",
                dateMillis = cal.timeInMillis,
                note = "Weekly grocery stocking"
            )
        )

        cal.add(Calendar.DAY_OF_YEAR, 1)
        repository.insertTransaction(
            TransactionEntity(
                amount = 1800.0,
                type = "EXPENSE",
                category = "Food & Dining",
                payeeOrSource = "Bella Italia Restaurant",
                dateMillis = cal.timeInMillis,
                note = "Dinner with family"
            )
        )

        cal.add(Calendar.DAY_OF_YEAR, 1)
        repository.insertTransaction(
            TransactionEntity(
                amount = 4500.0,
                type = "EXPENSE",
                category = "Shopping",
                payeeOrSource = "Apparel Mall",
                dateMillis = cal.timeInMillis,
                note = "New autumn coat"
            )
        )

        // Fuel Entries & corresponding Fuel Transactions for Car
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val fuelId = repository.insertFuelEntry(
            FuelEntryEntity(
                amountPaid = 2800.0,
                volumeLiters = 31.0,
                odometerReading = 12500.0,
                dateMillis = cal.timeInMillis,
                note = "Gas station fuel refuel",
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
                note = "[Fuel Log #$fuelId] Refueled (Car) 31.0L at Odometer 12,500.0 km"
            )
        )

        cal.add(Calendar.DAY_OF_YEAR, 1)
        val fuelId2 = repository.insertFuelEntry(
            FuelEntryEntity(
                amountPaid = 3200.0,
                volumeLiters = 35.0,
                odometerReading = 13100.0,
                dateMillis = cal.timeInMillis,
                note = "National Highway Hub",
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
                note = "[Fuel Log #$fuelId2] Refueled (Car) 35.0L at Odometer 13,100.0 km"
            )
        )

        // Let's seed a Bike refuel log too to highlight the split feature beautifully!
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val fuelId3 = repository.insertFuelEntry(
            FuelEntryEntity(
                amountPaid = 400.0,
                volumeLiters = 4.2,
                odometerReading = 4100.0,
                dateMillis = cal.timeInMillis,
                note = "City Fuels Bike Station",
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
                note = "[Fuel Log #$fuelId3] Refueled (Bike) 4.2L at Odometer 4,100.0 km"
            )
        )
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
}
