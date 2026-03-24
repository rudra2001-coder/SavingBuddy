package com.example.savingbuddy.domain.repository

import com.example.savingbuddy.domain.model.HealthEntry
import kotlinx.coroutines.flow.Flow

interface HealthRepository {
    fun getHealthEntries(startDate: Long, endDate: Long): Flow<List<HealthEntry>>
    fun getTodayEntry(todayStart: Long): Flow<HealthEntry?>
    suspend fun getEntryById(id: String): HealthEntry?
    suspend fun saveEntry(entry: HealthEntry)
    suspend fun deleteEntry(entry: HealthEntry)
}
