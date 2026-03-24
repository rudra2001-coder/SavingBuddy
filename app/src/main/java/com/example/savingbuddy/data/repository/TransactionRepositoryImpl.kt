package com.example.savingbuddy.data.repository

import com.example.savingbuddy.data.local.dao.TransactionDao
import com.example.savingbuddy.data.local.mapper.toDomain
import com.example.savingbuddy.data.local.mapper.toEntity
import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class TransactionRepositoryImpl @Inject constructor(
    private val transactionDao: TransactionDao
) : TransactionRepository {

    override fun getAllTransactions(): Flow<List<Transaction>> =
        transactionDao.getAllTransactions().map { list -> list.map { it.toDomain() } }

    override suspend fun getTransactionById(id: String): Transaction? =
        transactionDao.getTransactionById(id)?.toDomain()

    override fun getTransactionsByDateRange(startTime: Long, endTime: Long): Flow<List<Transaction>> =
        transactionDao.getTransactionsByDateRange(startTime, endTime).map { list -> list.map { it.toDomain() } }

    override fun getTransactionsByAccount(accountId: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByAccount(accountId).map { list -> list.map { it.toDomain() } }

    override fun getTransactionsByType(type: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByType(type).map { list -> list.map { it.toDomain() } }

    override fun getTransactionsByCategory(categoryId: String): Flow<List<Transaction>> =
        transactionDao.getTransactionsByCategory(categoryId).map { list -> list.map { it.toDomain() } }

    override fun getTotalIncome(startTime: Long, endTime: Long): Flow<Double> =
        transactionDao.getTotalIncome(startTime, endTime).map { it ?: 0.0 }

    override fun getTotalExpense(startTime: Long, endTime: Long): Flow<Double> =
        transactionDao.getTotalExpense(startTime, endTime).map { it ?: 0.0 }

    override fun getRecentTransactions(limit: Int): Flow<List<Transaction>> =
        transactionDao.getRecentTransactions(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun addTransaction(transaction: Transaction) {
        transactionDao.insertTransaction(transaction.toEntity())
    }

    override suspend fun updateTransaction(transaction: Transaction) {
        transactionDao.updateTransaction(transaction.toEntity())
    }

    override suspend fun deleteTransaction(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction.toEntity())
    }
}