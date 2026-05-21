package com.example.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ===== Static configuration =====

val CATEGORIES: Map<String, List<String>> = mapOf(
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

fun bucketFor(category: String): String = when {
    category.startsWith("Travel —") -> "Travel"
    category.startsWith("JUNA —") -> "JUNA"
    else -> "Life Expenses"
}

// ===== Date helpers =====

fun getCurrentDateISO(): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

fun thisMonthKey(): String =
    SimpleDateFormat("yyyy-MM", Locale.US).format(Date())

fun previousMonthOf(monthKey: String): String? {
    if (monthKey == "all") return null
    return runCatching {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.US)
        val parsed = sdf.parse(monthKey) ?: return null
        val cal = Calendar.getInstance().apply {
            time = parsed
            add(Calendar.MONTH, -1)
        }
        sdf.format(cal.time)
    }.getOrNull()
}

fun formatMonthLabel(monthKey: String): String {
    if (monthKey == "all") return "All time"
    return runCatching {
        val parsed = SimpleDateFormat("yyyy-MM", Locale.US).parse(monthKey)!!
        SimpleDateFormat("MMMM yyyy", Locale.US).format(parsed)
    }.getOrDefault(monthKey)
}

fun formatDateShort(dateStr: String): String =
    runCatching {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(dateStr)!!
        SimpleDateFormat("dd MMM", Locale.US).format(parsed)
    }.getOrDefault(dateStr)

// ===== Currency formatters =====

private val InLocale: Locale = Locale.forLanguageTag("en-IN")

fun fmtAmount(n: Double): String {
    val r = kotlin.math.round(n)
    return "₹" + String.format(InLocale, "%,.0f", kotlin.math.abs(r))
}

fun fmtCompact(n: Double): String {
    val r = kotlin.math.round(n).toLong()
    return when {
        r == 0L -> ""
        r >= 100000L -> String.format(Locale.US, "%.1fL", r / 100000.0).replace(".0", "")
        r >= 10000L -> "${kotlin.math.round(r / 1000.0).toLong()}K"
        r >= 1000L -> String.format(Locale.US, "%.1fK", r / 1000.0).replace(".0", "")
        else -> r.toString()
    }
}

// ===== Balance & filtering =====

fun calculateBalanceChange(expense: Expense, u1: String, u2: String): Double {
    if (expense.split == "Personal") return 0.0
    val isU1Payer = expense.paidBy == u1
    return when {
        expense.split == "50/50" ->
            if (isU1Payer) expense.amount / 2.0 else -expense.amount / 2.0
        expense.split == "$u1 owes full" ->
            if (isU1Payer) 0.0 else -expense.amount
        expense.split == "$u2 owes full" ->
            if (isU1Payer) expense.amount else 0.0
        expense.split.startsWith("Custom") -> {
            val pct = expense.split.filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
            val change = expense.amount * (pct / 100.0)
            if (isU1Payer) change else -change
        }
        else -> 0.0
    }
}

fun calculateBucketTotals(expenses: List<Expense>, monthKey: String): Map<String, Double> {
    val totals = mutableMapOf("Life Expenses" to 0.0, "Travel" to 0.0, "JUNA" to 0.0)
    expenses.forEach { e ->
        val m = e.date.substring(0, 7)
        if (monthKey == "all" || m == monthKey) {
            val b = bucketFor(e.category)
            totals[b] = (totals[b] ?: 0.0) + e.amount
        }
    }
    return totals
}

fun getTrendTextForBucket(expenses: List<Expense>, monthKey: String, bucket: String): String {
    if (monthKey == "all") return ""
    val pm = previousMonthOf(monthKey) ?: return ""

    val thisMonthAmt = expenses.filter {
        it.date.substring(0, 7) == monthKey && bucketFor(it.category) == bucket
    }.sumOf { it.amount }
    val lastMonthAmt = expenses.filter {
        it.date.substring(0, 7) == pm && bucketFor(it.category) == bucket
    }.sumOf { it.amount }

    return when {
        lastMonthAmt == 0.0 && thisMonthAmt == 0.0 -> "—"
        lastMonthAmt == 0.0 -> "New"
        else -> {
            val pct = kotlin.math.round(((thisMonthAmt - lastMonthAmt) / lastMonthAmt) * 100).toInt()
            when {
                pct > 0 -> "↑ $pct% vs last"
                pct < 0 -> "↓ ${kotlin.math.abs(pct)}% vs last"
                else -> "— same as last"
            }
        }
    }
}

data class TrendMonth(val key: String, val label: String, val amount: Double, val isCurrent: Boolean)

fun getTrendMonths(expenses: List<Expense>, bucket: String): List<TrendMonth> {
    val monthShort = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    val now = Calendar.getInstance()
    val earliestKey = expenses.minOfOrNull { it.date.substring(0, 7) }

    val list = mutableListOf<TrendMonth>()
    for (i in 5 downTo 0) {
        val cal = Calendar.getInstance().apply { add(Calendar.MONTH, -i) }
        val key = String.format(
            Locale.US, "%04d-%02d",
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1
        )
        if (earliestKey != null && key < earliestKey) continue

        val sumAmount = expenses.filter {
            it.date.substring(0, 7) == key && bucketFor(it.category) == bucket
        }.sumOf { it.amount }
        list.add(TrendMonth(key, monthShort[cal.get(Calendar.MONTH)], sumAmount, isCurrent = (i == 0)))
    }

    if (list.isEmpty()) {
        val key = thisMonthKey()
        val sumAmount = expenses.filter {
            it.date.substring(0, 7) == key && bucketFor(it.category) == bucket
        }.sumOf { it.amount }
        list.add(TrendMonth(key, monthShort[now.get(Calendar.MONTH)], sumAmount, isCurrent = true))
    }
    return list
}

fun getMRUPending(expenses: List<Expense>): List<String> =
    expenses.take(30)
        .groupBy { it.category }
        .mapValues { it.value.size }
        .entries
        .sortedByDescending { it.value }
        .take(4)
        .map { it.key }

fun filterExpenses(
    expenses: List<Expense>,
    monthKey: String,
    categoryKey: String,
    searchQuery: String
): List<Expense> {
    var list = expenses

    if (monthKey != "all") {
        list = list.filter { it.date.substring(0, 7) == monthKey }
    }

    list = when {
        categoryKey == "personal" -> list.filter { it.split == "Personal" }
        categoryKey == "shared" -> list.filter { it.split != "Personal" }
        categoryKey.startsWith("bucket:") -> {
            val b = categoryKey.substring(7)
            list.filter { bucketFor(it.category) == b }
        }
        categoryKey != "all" -> list.filter { it.category == categoryKey }
        else -> list
    }

    if (searchQuery.isNotEmpty()) {
        val q = searchQuery.trim().lowercase()
        list = list.filter {
            it.description.lowercase().contains(q) || (it.tag?.lowercase()?.contains(q) ?: false)
        }
    }

    return list.sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.id })
}
