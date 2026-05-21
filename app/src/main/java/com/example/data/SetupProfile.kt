package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "setup_profile")
data class SetupProfile(
    @PrimaryKey val id: Int = 1,
    val user1Name: String,
    val user2Name: String,
    val isSetup: Boolean = true,
    val sheetUrl: String? = null
)
