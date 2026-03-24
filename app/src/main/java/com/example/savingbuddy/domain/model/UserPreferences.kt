package com.example.savingbuddy.domain.model

data class UserPreferences(
    val id: Int = 1,
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
    val createdAt: Long,
    val updatedAt: Long
)
