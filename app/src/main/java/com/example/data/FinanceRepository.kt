package com.example.data

import kotlinx.coroutines.flow.Flow

class FinanceRepository(private val financeDao: FinanceDao) {
    val allTransactions: Flow<List<TransactionEntity>> = financeDao.getAllTransactions()
    val allBudgets: Flow<List<BudgetEntity>> = financeDao.getAllBudgets()
    val allFuelEntries: Flow<List<FuelEntryEntity>> = financeDao.getAllFuelEntries()
    val allPendingTransactions: Flow<List<PendingSmsTransactionEntity>> = financeDao.getAllPendingTransactions()
    val allInvestments: Flow<List<InvestmentEntity>> = financeDao.getAllInvestments()
    val allGoals: Flow<List<GoalEntity>> = financeDao.getAllGoals()
    val allRecurringReminders: Flow<List<RecurringReminderEntity>> = financeDao.getAllRecurringReminders()
    val allReminderPayments: Flow<List<ReminderPaymentEntity>> = financeDao.getAllReminderPayments()

    fun getPaymentHistoryForReminder(reminderId: Long): Flow<List<ReminderPaymentEntity>> {
        return financeDao.getPaymentHistoryForReminder(reminderId)
    }

    suspend fun insertReminderPayment(payment: ReminderPaymentEntity): Long {
        return financeDao.insertReminderPayment(payment)
    }

    suspend fun deleteReminderPayment(payment: ReminderPaymentEntity) {
        financeDao.deleteReminderPayment(payment)
    }

    suspend fun deleteReminderPaymentById(id: Long) {
        financeDao.deleteReminderPaymentById(id)
    }

    suspend fun deletePaymentsForReminder(reminderId: Long) {
        financeDao.deletePaymentsForReminder(reminderId)
    }

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

    suspend fun insertInvestment(investment: InvestmentEntity) {
        financeDao.insertInvestment(investment)
    }

    suspend fun deleteInvestment(investment: InvestmentEntity) {
        financeDao.deleteInvestment(investment)
    }

    suspend fun deleteInvestmentById(id: Int) {
        financeDao.deleteInvestmentById(id)
    }

    suspend fun insertGoal(goal: GoalEntity) {
        financeDao.insertGoal(goal)
    }

    suspend fun deleteGoal(goal: GoalEntity) {
        financeDao.deleteGoal(goal)
    }

    suspend fun deleteGoalById(id: Int) {
        financeDao.deleteGoalById(id)
    }

    suspend fun insertRecurringReminder(reminder: RecurringReminderEntity) {
        financeDao.insertRecurringReminder(reminder)
    }

    suspend fun deleteRecurringReminder(reminder: RecurringReminderEntity) {
        financeDao.deleteRecurringReminder(reminder)
    }

    suspend fun deleteRecurringReminderById(id: Long) {
        financeDao.deleteRecurringReminderById(id)
    }

    suspend fun clearAllTransactions() {
        financeDao.clearTransactions()
    }

    suspend fun clearAllData() {
        financeDao.clearTransactions()
        financeDao.clearBudgets()
        financeDao.clearFuelEntries()
        financeDao.clearPendingSmsTransactions()
        financeDao.clearInvestments()
        financeDao.clearGoals()
        financeDao.clearRecurringReminders()
        financeDao.clearReminderPayments()
    }

    suspend fun insertAllDataBulk(
        transactions: List<TransactionEntity>,
        budgets: List<BudgetEntity>,
        fuelEntries: List<FuelEntryEntity>,
        pendingSms: List<PendingSmsTransactionEntity>,
        investments: List<InvestmentEntity>,
        goals: List<GoalEntity>
    ) {
        if (transactions.isNotEmpty()) financeDao.insertTransactionsBulk(transactions)
        if (budgets.isNotEmpty()) financeDao.insertBudgetsBulk(budgets)
        if (fuelEntries.isNotEmpty()) financeDao.insertFuelEntriesBulk(fuelEntries)
        if (pendingSms.isNotEmpty()) financeDao.insertPendingSmsTransactionsBulk(pendingSms)
        if (investments.isNotEmpty()) financeDao.insertInvestmentsBulk(investments)
        if (goals.isNotEmpty()) financeDao.insertGoalsBulk(goals)
    }
}
