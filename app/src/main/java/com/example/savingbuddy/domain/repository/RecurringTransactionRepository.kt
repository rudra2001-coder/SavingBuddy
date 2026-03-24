package com.example.savingbuddy.domain.repository

import com.example.savingbuddy.domain.model.RecurringTransaction
import kotlinx.coroutines.flow.Flow

interface RecurringTransactionRepository {
    fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>>
    fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>>
    suspend fun getRecurringTransactionById(id: String): RecurringTransaction?
    suspend fun getDueRecurringTransactions(timestamp: Long): List<RecurringTransaction>
    suspend fun deactivateRecurringTransaction(id: String)
    suspend fun updateLastProcessedDate(id: String, date: Long)
    suspend fun addRecurringTransaction(transaction: RecurringTransaction)
    suspend fun updateRecurringTransaction(transaction: RecurringTransaction)
    suspend fun deleteRecurringTransaction(transaction: RecurringTransaction)
}
