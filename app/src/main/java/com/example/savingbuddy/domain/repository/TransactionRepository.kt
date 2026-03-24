package com.example.savingbuddy.domain.repository

import com.example.savingbuddy.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    suspend fun getTransactionById(id: String): Transaction?
    fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>>
    fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>>
    fun getTransactionsByType(type: String): Flow<List<Transaction>>
    fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>>
    fun getTotalIncome(startTime: Long, endTime: Long): Flow<Double>
    fun getTotalExpense(startTime: Long, endTime: Long): Flow<Double>
    fun getRecentTransactions(limit: Int): Flow<List<Transaction>>
    suspend fun addTransaction(transaction: Transaction)
    suspend fun updateTransaction(transaction: Transaction)
    suspend fun deleteTransaction(transaction: Transaction)
}