package com.example.data

import kotlinx.coroutines.flow.Flow

class ExpenseRepository(private val expenseDao: ExpenseDao) {
    val setupProfile: Flow<SetupProfile?> = expenseDao.getSetupProfile()
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    suspend fun saveSetupProfile(profile: SetupProfile) {
        expenseDao.insertSetupProfile(profile)
    }

    suspend fun insertExpense(expense: Expense): Long {
        return expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: Expense) {
        expenseDao.updateExpense(expense)
    }

    suspend fun deleteExpenseById(id: Long) {
        expenseDao.deleteExpenseById(id)
    }

    suspend fun settleAllUnsettled() {
        expenseDao.settleAllUnsettled()
    }
}
