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

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPendingTransaction(pending: PendingSmsTransactionEntity): Long

    @Query("DELETE FROM pending_sms_transactions WHERE id = :id")
    suspend fun deletePendingTransactionById(id: Int)

    @Delete
    suspend fun deletePendingTransaction(pending: PendingSmsTransactionEntity)
}
