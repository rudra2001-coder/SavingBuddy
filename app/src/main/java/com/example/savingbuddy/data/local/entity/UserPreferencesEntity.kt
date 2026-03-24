package com.example.savingbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreferencesEntity(
    @PrimaryKey val id: Int = 1,
    val userName: String = "User",
    val monthlyIncome: Double = 0.0,
    val currency: String = "BDT",
    val currencySymbol: String = "৳",
    val autoBackupEnabled: Boolean = false,
    val backupFrequency: String = "DAILY",
    val lastBackupDate: Long? = null,
    val darkModeEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val defaultAccountId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
