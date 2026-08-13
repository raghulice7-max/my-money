package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    // Transactions
    @Query("SELECT * FROM transactions ORDER BY dateMillis DESC, id DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    @Delete
    suspend fun deleteTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)

    // Budgets
    @Query("SELECT * FROM budgets")
    fun getAllBudgets(): Flow<List<BudgetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: BudgetEntity)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudgetByCategory(category: String)

    // Fuel Entries
    @Query("SELECT * FROM fuel_entries ORDER BY dateMillis DESC, id DESC")
    fun getAllFuelEntries(): Flow<List<FuelEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelEntry(fuelEntry: FuelEntryEntity): Long

    @Delete
    suspend fun deleteFuelEntry(fuelEntry: FuelEntryEntity)

    @Query("DELETE FROM fuel_entries WHERE id = :id")
    suspend fun deleteFuelEntryById(id: Int)

    // Pending SMS Transactions
    @Query("SELECT * FROM pending_sms_transactions ORDER BY dateMillis DESC")
    fun getAllPendingTransactions(): Flow<List<PendingSmsTransactionEntity>>

    @Query("SELECT * FROM pending_sms_transactions")
    suspend fun getPendingTransactionsSync(): List<PendingSmsTransactionEntity>

    @Query("SELECT * FROM transactions")
    suspend fun getRecentTransactionsSync(): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE amount = :amount AND dateMillis = :dateMillis")
    suspend fun findTransactionByAmountAndDate(amount: Double, dateMillis: Long): List<TransactionEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingTransaction(pending: PendingSmsTransactionEntity): Long

    @Query("DELETE FROM pending_sms_transactions WHERE id = :id")
    suspend fun deletePendingTransactionById(id: Int)

    @Delete
    suspend fun deletePendingTransaction(pending: PendingSmsTransactionEntity)

    // Investments
    @Query("SELECT * FROM investments ORDER BY name ASC")
    fun getAllInvestments(): Flow<List<InvestmentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(investment: InvestmentEntity)

    @Delete
    suspend fun deleteInvestment(investment: InvestmentEntity)

    @Query("DELETE FROM investments WHERE id = :id")
    suspend fun deleteInvestmentById(id: Int)

    // Goals
    @Query("SELECT * FROM goals ORDER BY targetAmount DESC")
    fun getAllGoals(): Flow<List<GoalEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: GoalEntity)

    @Delete
    suspend fun deleteGoal(goal: GoalEntity)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun deleteGoalById(id: Int)

    // Backup & Restore Bulk operations
    @Query("DELETE FROM transactions")
    suspend fun clearTransactions()

    @Query("DELETE FROM budgets")
    suspend fun clearBudgets()

    @Query("DELETE FROM fuel_entries")
    suspend fun clearFuelEntries()

    @Query("DELETE FROM pending_sms_transactions")
    suspend fun clearPendingSmsTransactions()

    @Query("DELETE FROM investments")
    suspend fun clearInvestments()

    @Query("DELETE FROM goals")
    suspend fun clearGoals()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactionsBulk(list: List<TransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgetsBulk(list: List<BudgetEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFuelEntriesBulk(list: List<FuelEntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingSmsTransactionsBulk(list: List<PendingSmsTransactionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestmentsBulk(list: List<InvestmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoalsBulk(list: List<GoalEntity>)

    // Recurring Reminders
    @Query("SELECT * FROM recurring_reminders ORDER BY dayOfMonth ASC")
    fun getAllRecurringReminders(): Flow<List<RecurringReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringReminder(reminder: RecurringReminderEntity)

    @Delete
    suspend fun deleteRecurringReminder(reminder: RecurringReminderEntity)

    @Query("DELETE FROM recurring_reminders WHERE id = :id")
    suspend fun deleteRecurringReminderById(id: Long)

    @Query("DELETE FROM recurring_reminders")
    suspend fun clearRecurringReminders()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringRemindersBulk(list: List<RecurringReminderEntity>)

    // Reminder Payment History
    @Query("SELECT * FROM reminder_payments WHERE reminderId = :reminderId ORDER BY paidDate DESC")
    fun getPaymentHistoryForReminder(reminderId: Long): Flow<List<ReminderPaymentEntity>>

    @Query("SELECT * FROM reminder_payments ORDER BY paidDate DESC")
    fun getAllReminderPayments(): Flow<List<ReminderPaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderPayment(payment: ReminderPaymentEntity): Long

    @Delete
    suspend fun deleteReminderPayment(payment: ReminderPaymentEntity)

    @Query("DELETE FROM reminder_payments WHERE id = :id")
    suspend fun deleteReminderPaymentById(id: Long)

    @Query("DELETE FROM reminder_payments WHERE reminderId = :reminderId")
    suspend fun deletePaymentsForReminder(reminderId: Long)

    @Query("DELETE FROM reminder_payments")
    suspend fun clearReminderPayments()
}
