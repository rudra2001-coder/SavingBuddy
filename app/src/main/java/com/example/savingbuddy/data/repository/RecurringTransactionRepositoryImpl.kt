package com.example.savingbuddy.data.repository

import com.example.savingbuddy.data.local.dao.RecurringTransactionDao
import com.example.savingbuddy.data.local.entity.RecurringTransactionEntity
import com.example.savingbuddy.domain.model.RecurringTransaction
import com.example.savingbuddy.domain.model.RecurringType
import com.example.savingbuddy.domain.model.TransactionType
import com.example.savingbuddy.domain.repository.RecurringTransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class RecurringTransactionRepositoryImpl @Inject constructor(
    private val recurringTransactionDao: RecurringTransactionDao
) : RecurringTransactionRepository {

    override fun getActiveRecurringTransactions(): Flow<List<RecurringTransaction>> =
        recurringTransactionDao.getActiveRecurringTransactions().map { list -> list.mapNotNull { entityToRecurring(it) } }

    override fun getAllRecurringTransactions(): Flow<List<RecurringTransaction>> =
        recurringTransactionDao.getAllRecurringTransactions().map { list -> list.mapNotNull { entityToRecurring(it) } }

    override suspend fun getRecurringTransactionById(id: String): RecurringTransaction? =
        entityToRecurring(recurringTransactionDao.getRecurringTransactionById(id))

    override suspend fun getDueRecurringTransactions(timestamp: Long): List<RecurringTransaction> =
        recurringTransactionDao.getDueRecurringTransactions(timestamp).mapNotNull { entityToRecurring(it) }

    override suspend fun deactivateRecurringTransaction(id: String) {
        recurringTransactionDao.deactivateRecurringTransaction(id)
    }

    override suspend fun updateLastProcessedDate(id: String, date: Long) {
        recurringTransactionDao.updateLastProcessedDate(id, date)
    }

    override suspend fun addRecurringTransaction(transaction: RecurringTransaction) {
        recurringTransactionDao.insertRecurringTransaction(recurringToEntity(transaction))
    }

    override suspend fun updateRecurringTransaction(transaction: RecurringTransaction) {
        recurringTransactionDao.updateRecurringTransaction(recurringToEntity(transaction))
    }

    override suspend fun deleteRecurringTransaction(transaction: RecurringTransaction) {
        recurringTransactionDao.deleteRecurringTransaction(recurringToEntity(transaction))
    }

    private fun entityToRecurring(entity: RecurringTransactionEntity?): RecurringTransaction? {
        return entity?.let {
            RecurringTransaction(
                id = it.id,
                title = it.title,
                amount = it.amount,
                type = TransactionType.valueOf(it.type),
                categoryId = it.categoryId,
                accountId = it.accountId,
                recurringType = RecurringType.valueOf(it.recurringType),
                startDate = it.startDate,
                endDate = it.endDate,
                selectedDays = it.selectedDays?.split(",")?.mapNotNull { s -> s.toIntOrNull() },
                selectedDate = it.selectedDate,
                note = it.note,
                isActive = it.isActive,
                excludeHolidays = it.excludeHolidays,
                reminderEnabled = it.reminderEnabled,
                reminderMinutesBefore = it.reminderMinutesBefore,
                lastProcessedDate = it.lastProcessedDate,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
    }

    private fun recurringToEntity(transaction: RecurringTransaction): RecurringTransactionEntity {
        return RecurringTransactionEntity(
            id = transaction.id,
            title = transaction.title,
            amount = transaction.amount,
            type = transaction.type.name,
            categoryId = transaction.categoryId,
            accountId = transaction.accountId,
            recurringType = transaction.recurringType.name,
            startDate = transaction.startDate,
            endDate = transaction.endDate,
            selectedDays = transaction.selectedDays?.joinToString(","),
            selectedDate = transaction.selectedDate,
            note = transaction.note,
            isActive = transaction.isActive,
            excludeHolidays = transaction.excludeHolidays,
            reminderEnabled = transaction.reminderEnabled,
            reminderMinutesBefore = transaction.reminderMinutesBefore,
            lastProcessedDate = transaction.lastProcessedDate,
            createdAt = transaction.createdAt,
            updatedAt = transaction.updatedAt
        )
    }
}
