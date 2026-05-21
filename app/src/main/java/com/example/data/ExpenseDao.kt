package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM setup_profile WHERE id = 1 LIMIT 1")
    fun getSetupProfile(): Flow<SetupProfile?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetupProfile(profile: SetupProfile)

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpenseById(id: Long)

    @Query("UPDATE expenses SET settled = 1 WHERE settled = 0")
    suspend fun settleAllUnsettled()
}
