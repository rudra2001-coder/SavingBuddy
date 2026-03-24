package com.example.savingbuddy.data.local.dao

import androidx.room.*
import com.example.savingbuddy.data.local.entity.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    @Query("SELECT * FROM user_preferences WHERE id = 1")
    fun getPreferences(): Flow<UserPreferencesEntity?>

    @Query("SELECT * FROM user_preferences WHERE id = 1")
    suspend fun getPreferencesSync(): UserPreferencesEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreferences(preferences: UserPreferencesEntity)

    @Query("UPDATE user_preferences SET autoBackupEnabled = :enabled, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateAutoBackup(enabled: Boolean, timestamp: Long)

    @Query("UPDATE user_preferences SET lastBackupDate = :date, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateLastBackupDate(date: Long, timestamp: Long)

    @Query("UPDATE user_preferences SET userName = :name, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateUserName(name: String, timestamp: Long)

    @Query("UPDATE user_preferences SET monthlyIncome = :income, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateMonthlyIncome(income: Double, timestamp: Long)

    @Query("UPDATE user_preferences SET darkModeEnabled = :enabled, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateDarkMode(enabled: Boolean, timestamp: Long)

    @Query("UPDATE user_preferences SET notificationsEnabled = :enabled, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateNotifications(enabled: Boolean, timestamp: Long)

    @Query("UPDATE user_preferences SET backupFrequency = :frequency, updatedAt = :timestamp WHERE id = 1")
    suspend fun updateBackupFrequency(frequency: String, timestamp: Long)
}
