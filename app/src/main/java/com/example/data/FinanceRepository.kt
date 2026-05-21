package com.example.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val financeDao: FinanceDao) {
    val allTransactions: Flow<List<TransactionEntity>> = financeDao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = financeDao.getAllBudgets()
    val allFuelEntries: Flow<List<FuelEntryEntity>> = financeDao.getAllFuelEntries()
    val allPendingTransactions: Flow<List<PendingSmsTransactionEntity>> = financeDao.getAllPendingTransactions()

    suspend fun insertTransaction(transaction: TransactionEntity) {
        financeDao.insertTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        financeDao.deleteTransaction(transaction)
    }

    suspend fun deleteTransactionById(id: Int) {
        financeDao.deleteTransactionById(id)
    }

    suspend fun insertBudget(budget: BudgetEntity) {
        financeDao.insertBudget(budget)
    }

    suspend fun deleteBudgetByCategory(category: String) {
        financeDao.deleteBudgetByCategory(category)
    }

    suspend fun insertFuelEntry(fuelEntry: FuelEntryEntity): Long {
        return financeDao.insertFuelEntry(fuelEntry)
    }

    suspend fun deleteFuelEntry(fuelEntry: FuelEntryEntity) {
        financeDao.deleteFuelEntry(fuelEntry)
    }

    suspend fun deleteFuelEntryById(id: Int) {
        financeDao.deleteFuelEntryById(id)
    }

    suspend fun insertPendingTransaction(pending: PendingSmsTransactionEntity): Long {
        return financeDao.insertPendingTransaction(pending)
    }

    suspend fun deletePendingTransactionById(id: Int) {
        financeDao.deletePendingTransactionById(id)
    }

    suspend fun deletePendingTransaction(pending: PendingSmsTransactionEntity) {
        financeDao.deletePendingTransaction(pending)
    }
}
