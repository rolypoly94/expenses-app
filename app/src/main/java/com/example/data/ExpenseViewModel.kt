package com.example.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ExpenseViewModel(
    private val repository: ExpenseRepository
) : ViewModel() {

    val setupProfile: StateFlow<SetupProfile?> = repository.setupProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedMonth = MutableStateFlow(thisMonthKey())
    val selectedCategory = MutableStateFlow("all")
    val searchQuery = MutableStateFlow("")
    val txDisplayLimit = MutableStateFlow(10)

    // Recomputes when either expenses or the profile changes, so the balance
    // is correct on cold start before names have loaded.
    val runningBalance: StateFlow<Double> = combine(allExpenses, setupProfile) { list, profile ->
        val u1 = profile?.user1Name ?: ""
        val u2 = profile?.user2Name ?: ""
        list.filter { !it.settled }.sumOf { calculateBalanceChange(it, u1, u2) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val unsettledCount: StateFlow<Int> = allExpenses.map { list ->
        list.count { !it.settled && it.split != "Personal" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun saveSetup(user1: String, user2: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching {
                repository.saveSetupProfile(
                    SetupProfile(user1Name = user1, user2Name = user2, isSetup = true)
                )
            }.isSuccess
            onResult(ok)
        }
    }

    fun addExpense(
        date: String,
        description: String,
        category: String,
        tag: String?,
        amount: Double,
        paidBy: String,
        split: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val ok = runCatching {
                repository.insertExpense(
                    Expense(
                        date = date,
                        description = description,
                        category = category,
                        tag = tag?.takeIf { it.isNotBlank() },
                        amount = amount,
                        paidBy = paidBy,
                        split = split
                    )
                )
            }.isSuccess
            onResult(ok)
        }
    }

    fun updateExpense(
        id: Long,
        date: String,
        description: String,
        category: String,
        tag: String?,
        amount: Double,
        paidBy: String,
        split: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val ok = runCatching {
                repository.updateExpense(
                    Expense(
                        id = id,
                        date = date,
                        description = description,
                        category = category,
                        tag = tag?.takeIf { it.isNotBlank() },
                        amount = amount,
                        paidBy = paidBy,
                        split = split
                    )
                )
            }.isSuccess
            onResult(ok)
        }
    }

    fun deleteExpense(expense: Expense, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching { repository.deleteExpenseById(expense.id) }.isSuccess
            onResult(ok)
        }
    }

    fun settleUp(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching { repository.settleAllUnsettled() }.isSuccess
            onResult(ok)
        }
    }
}
