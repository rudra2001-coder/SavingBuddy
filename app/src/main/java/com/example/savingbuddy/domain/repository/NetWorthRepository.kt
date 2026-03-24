package com.example.savingbuddy.domain.repository

import com.example.savingbuddy.domain.model.NetWorth
import kotlinx.coroutines.flow.Flow

interface NetWorthRepository {
    fun getNetWorth(): Flow<NetWorth>
    suspend fun calculateNetWorth(): NetWorth
}