package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // format "YYYY-MM-DD"
    val description: String,
    val category: String,
    val tag: String? = null,
    val amount: Double,
    val paidBy: String,
    val split: String, // "50/50", "Rahul owes full", "Priya owes full", "Custom 30%", "Personal"
    val settled: Boolean = false,
    val attachmentUri: String? = null // local storage file path
)
