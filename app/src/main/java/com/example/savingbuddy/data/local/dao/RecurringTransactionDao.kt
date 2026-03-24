package com.example.savingbuddy.data.local.dao

import androidx.room.*
import com.example.savingbuddy.data.local.entity.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringTransactionDao {
    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions ORDER BY createdAt DESC")
    fun getAllRecurringTransactions(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE id = :id")
    suspend fun getRecurringTransactionById(id: String): RecurringTransactionEntity?

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 AND startDate <= :timestamp")
    suspend fun getDueRecurringTransactions(timestamp: Long): List<RecurringTransactionEntity>

    @Query("UPDATE recurring_transactions SET isActive = 0 WHERE id = :id")
    suspend fun deactivateRecurringTransaction(id: String)

    @Query("UPDATE recurring_transactions SET lastProcessedDate = :date WHERE id = :id")
    suspend fun updateLastProcessedDate(id: String, date: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecurringTransaction(transaction: RecurringTransactionEntity)

    @Update
    suspend fun updateRecurringTransaction(transaction: RecurringTransactionEntity)

    @Delete
    suspend fun deleteRecurringTransaction(transaction: RecurringTransactionEntity)
}
