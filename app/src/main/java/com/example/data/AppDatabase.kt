package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.io.File

@Database(entities = [Expense::class, SetupProfile::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // v1 → v2: receipt attachments were removed entirely. SQLite < 3.35 (older
        // Android devices) has no DROP COLUMN, so we recreate the table.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE expenses_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        date TEXT NOT NULL,
                        description TEXT NOT NULL,
                        category TEXT NOT NULL,
                        tag TEXT,
                        amount REAL NOT NULL,
                        paidBy TEXT NOT NULL,
                        split TEXT NOT NULL,
                        settled INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO expenses_new (id, date, description, category, tag, amount, paidBy, split, settled)
                    SELECT id, date, description, category, tag, amount, paidBy, split, settled FROM expenses
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE expenses")
                db.execSQL("ALTER TABLE expenses_new RENAME TO expenses")
            }
        }

        // v2 → v3: drop the unused sheetUrl column from setup_profile.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE setup_profile_new (
                        id INTEGER PRIMARY KEY NOT NULL,
                        user1Name TEXT NOT NULL,
                        user2Name TEXT NOT NULL,
                        isSetup INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO setup_profile_new (id, user1Name, user2Name, isSetup)
                    SELECT id, user1Name, user2Name, isSetup FROM setup_profile
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE setup_profile")
                db.execSQL("ALTER TABLE setup_profile_new RENAME TO setup_profile")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expenses_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                INSTANCE = instance

                // Clean up the on-disk receipts directory left over from v1.
                runCatching {
                    File(context.applicationContext.filesDir, "receipts").deleteRecursively()
                }

                instance
            }
        }
    }
}
