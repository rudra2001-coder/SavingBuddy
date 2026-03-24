package com.example.savingbuddy.data.repository

import com.example.savingbuddy.data.local.dao.UserPreferencesDao
import com.example.savingbuddy.data.local.mapper.toDomain
import com.example.savingbuddy.data.local.mapper.toEntity
import com.example.savingbuddy.data.local.entity.UserPreferencesEntity
import com.example.savingbuddy.domain.model.UserPreferences
import com.example.savingbuddy.domain.repository.UserPreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class UserPreferencesRepositoryImpl @Inject constructor(
    private val userPreferencesDao: UserPreferencesDao
) : UserPreferencesRepository {

    override fun getPreferences(): Flow<UserPreferences?> =
        userPreferencesDao.getPreferences().map { it?.toDomain() }

    override suspend fun getPreferencesSync(): UserPreferences? =
        userPreferencesDao.getPreferencesSync()?.toDomain()

    override suspend fun savePreferences(preferences: UserPreferences) {
        userPreferencesDao.insertPreferences(preferences.toEntity())
    }

    override suspend fun updateAutoBackup(enabled: Boolean) {
        userPreferencesDao.updateAutoBackup(enabled, System.currentTimeMillis())
    }

    override suspend fun updateLastBackupDate(date: Long) {
        userPreferencesDao.updateLastBackupDate(date, System.currentTimeMillis())
    }

    override suspend fun updateUserName(name: String) {
        userPreferencesDao.updateUserName(name, System.currentTimeMillis())
    }

    override suspend fun updateMonthlyIncome(income: Double) {
        userPreferencesDao.updateMonthlyIncome(income, System.currentTimeMillis())
    }

    override suspend fun updateDarkMode(enabled: Boolean) {
        userPreferencesDao.updateDarkMode(enabled, System.currentTimeMillis())
    }

    override suspend fun updateNotifications(enabled: Boolean) {
        userPreferencesDao.updateNotifications(enabled, System.currentTimeMillis())
    }

    override suspend fun updateBackupFrequency(frequency: String) {
        userPreferencesDao.updateBackupFrequency(frequency, System.currentTimeMillis())
    }
}
