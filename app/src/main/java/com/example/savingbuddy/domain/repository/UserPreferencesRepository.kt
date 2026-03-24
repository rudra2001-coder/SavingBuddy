package com.example.savingbuddy.domain.repository

import com.example.savingbuddy.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface UserPreferencesRepository {
    fun getPreferences(): Flow<UserPreferences?>
    suspend fun getPreferencesSync(): UserPreferences?
    suspend fun savePreferences(preferences: UserPreferences)
    suspend fun updateAutoBackup(enabled: Boolean)
    suspend fun updateLastBackupDate(date: Long)
    suspend fun updateUserName(name: String)
    suspend fun updateMonthlyIncome(income: Double)
    suspend fun updateDarkMode(enabled: Boolean)
    suspend fun updateNotifications(enabled: Boolean)
    suspend fun updateBackupFrequency(frequency: String)
}
