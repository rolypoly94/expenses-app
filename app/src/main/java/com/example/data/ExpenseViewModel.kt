package com.example.data

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ExpenseViewModel(
    application: Application,
    private val repository: ExpenseRepository
) : AndroidViewModel(application) {

    // Central categories data
    val categories = mapOf(
        "Life Expenses" to listOf(
            "Groceries", "Eating Out", "Electricity", "Internet/Phone",
            "Rent", "Household", "Transport", "Kids", "Shopping", "Healthcare", "Other"
        ),
        "Travel" to listOf(
            "Travel — Flights", "Travel — Lodging", "Travel — Food",
            "Travel — Activities", "Travel — Local Transport"
        ),
        "JUNA" to listOf(
            "JUNA — Raw Materials", "JUNA — Packaging", "JUNA — Equipment",
            "JUNA — Marketing", "JUNA — Shipping", "JUNA — Other"
        )
    )

    fun bucketFor(category: String): String {
        return when {
            category.startsWith("Travel —") -> "Travel"
            category.startsWith("JUNA —") -> "JUNA"
            else -> "Life Expenses"
        }
    }

    // Settings / Setup profile
    val setupProfile: StateFlow<SetupProfile?> = repository.setupProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Raw expenses
    val allExpenses: StateFlow<List<Expense>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI filters
    val selectedMonth = MutableStateFlow(thisMonthKey())
    val selectedCategory = MutableStateFlow("all")
    val searchQuery = MutableStateFlow("")
    val txDisplayLimit = MutableStateFlow(10)

    fun thisMonthKey(): String {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        return sdf.format(Date())
    }

    fun previousMonthOf(monthKey: String): String? {
        if (monthKey == "all") return null
        return try {
            val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = sdf.parse(monthKey) ?: return null
            val cal = Calendar.getInstance()
            cal.time = date
            cal.add(Calendar.MONTH, -1)
            sdf.format(cal.time)
        } catch (e: Exception) {
            null
        }
    }

    fun formatMonthLabel(monthKey: String): String {
        if (monthKey == "all") return "All time"
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM", Locale.US)
            val date = sdfInput.parse(monthKey) ?: return monthKey
            val sdfOutput = SimpleDateFormat("MMMM yyyy", Locale.US)
            sdfOutput.format(date)
        } catch (e: Exception) {
            monthKey
        }
    }

    fun formatDateShort(dateStr: String): String {
        return try {
            val sdfInput = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val date = sdfInput.parse(dateStr) ?: return dateStr
            val sdfOutput = SimpleDateFormat("dd MMM", Locale.US)
            sdfOutput.format(date)
        } catch (e: Exception) {
            dateStr
        }
    }

    // Settle calculations — recomputes when either expenses or the profile changes,
    // so the balance is correct on cold start before names have loaded.
    val runningBalance: StateFlow<Double> = combine(allExpenses, setupProfile) { list, profile ->
        val u1 = profile?.user1Name ?: ""
        val u2 = profile?.user2Name ?: ""
        list.filter { !it.settled }.sumOf { calculateBalanceChange(it, u1, u2) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val unsettledCount: StateFlow<Int> = allExpenses.map { list ->
        list.count { !it.settled && it.split != "Personal" }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private fun calculateBalanceChange(expense: Expense, u1: String, u2: String): Double {
        if (expense.split == "Personal") return 0.0
        val isU1Payer = expense.paidBy == u1
        return when {
            expense.split == "50/50" -> {
                if (isU1Payer) expense.amount / 2.0 else -expense.amount / 2.0
            }
            expense.split == "$u1 owes full" -> {
                if (isU1Payer) 0.0 else -expense.amount
            }
            expense.split == "$u2 owes full" -> {
                if (isU1Payer) expense.amount else 0.0
            }
            expense.split.startsWith("Custom") -> {
                val pct = expense.split.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
                val change = expense.amount * (pct / 100.0)
                if (isU1Payer) change else -change
            }
            else -> 0.0
        }
    }

    // Get MRU Categories
    fun getMRUPending(expenses: List<Expense>): List<String> {
        return expenses.take(30)
            .groupBy { it.category }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(4)
            .map { it.key }
    }

    // Get 6-month trends
    fun getTrendMonths(expenses: List<Expense>, bucket: String): List<TrendMonth> {
        val monthShort = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val now = Calendar.getInstance()
        
        // Find the earliest expense month
        val earliestKey = expenses.minOfOrNull { it.date.substring(0, 7) }
        
        val list = mutableListOf<TrendMonth>()
        for (i in 5 downTo 0) {
            val cal = Calendar.getInstance()
            cal.add(Calendar.MONTH, -i)
            val key = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            
            if (earliestKey != null && key < earliestKey) {
                continue
            }
            
            val label = monthShort[cal.get(Calendar.MONTH)]
            val sumAmount = expenses.filter { 
                it.date.substring(0, 7) == key && bucketFor(it.category) == bucket 
            }.sumOf { it.amount }
            
            list.add(TrendMonth(key, label, sumAmount, isCurrent = (i == 0)))
        }
        
        if (list.isEmpty()) {
            val key = thisMonthKey()
            val label = monthShort[now.get(Calendar.MONTH)]
            val sumAmount = expenses.filter { 
                it.date.substring(0, 7) == key && bucketFor(it.category) == bucket 
            }.sumOf { it.amount }
            list.add(TrendMonth(key, label, sumAmount, isCurrent = true))
        }
        
        return list
    }

    // Save profile setup
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

    // Add Expense
    fun addExpense(
        date: String,
        description: String,
        category: String,
        tag: String?,
        amount: Double,
        paidBy: String,
        split: String,
        photoUri: Uri? = null,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val ok = runCatching {
                val localPath = photoUri?.let { saveReceiptImage(it) }
                repository.insertExpense(
                    Expense(
                        date = date,
                        description = description,
                        category = category,
                        tag = tag?.takeIf { it.isNotBlank() },
                        amount = amount,
                        paidBy = paidBy,
                        split = split,
                        attachmentUri = localPath
                    )
                )
            }.isSuccess
            onResult(ok)
        }
    }

    // Update Expense
    fun updateExpense(
        id: Long,
        date: String,
        description: String,
        category: String,
        tag: String?,
        amount: Double,
        paidBy: String,
        split: String,
        photoUri: Uri? = null,
        removeAttachment: Boolean = false,
        existingAttachment: String? = null,
        onResult: (Boolean) -> Unit = {}
    ) {
        viewModelScope.launch {
            val ok = runCatching {
                var localPath = existingAttachment
                if (removeAttachment || photoUri != null) {
                    existingAttachment?.let { deleteLocalFile(it) }
                    localPath = null
                }
                if (photoUri != null) {
                    localPath = saveReceiptImage(photoUri)
                }
                repository.updateExpense(
                    Expense(
                        id = id,
                        date = date,
                        description = description,
                        category = category,
                        tag = tag?.takeIf { it.isNotBlank() },
                        amount = amount,
                        paidBy = paidBy,
                        split = split,
                        attachmentUri = localPath
                    )
                )
            }.isSuccess
            onResult(ok)
        }
    }

    // Delete Expense
    fun deleteExpense(expense: Expense, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching {
                expense.attachmentUri?.let { deleteLocalFile(it) }
                repository.deleteExpenseById(expense.id)
            }.isSuccess
            onResult(ok)
        }
    }

    // Settle Up
    fun settleUp(onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val ok = runCatching { repository.settleAllUnsettled() }.isSuccess
            onResult(ok)
        }
    }

    private suspend fun deleteLocalFile(path: String) = withContext(Dispatchers.IO) {
        runCatching {
            val file = File(path)
            if (file.exists()) file.delete()
        }
    }

    // Helper file copier — runs off the main thread.
    private suspend fun saveReceiptImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val context = getApplication<Application>()
            val receiptsDir = File(context.filesDir, "receipts").apply { if (!exists()) mkdirs() }
            val destFile = File(receiptsDir, "receipt_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output -> input.copyTo(output) }
            }
            destFile.absolutePath
        }.getOrNull()
    }
}

data class TrendMonth(
    val key: String,
    val label: String,
    val amount: Double,
    val isCurrent: Boolean
)
