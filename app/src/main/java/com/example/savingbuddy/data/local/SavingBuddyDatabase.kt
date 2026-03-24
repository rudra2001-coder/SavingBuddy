package com.example.savingbuddy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.savingbuddy.data.local.dao.AccountDao
import com.example.savingbuddy.data.local.dao.CategoryDao
import com.example.savingbuddy.data.local.dao.SavingsGoalDao
import com.example.savingbuddy.data.local.dao.TransactionDao
import com.example.savingbuddy.data.local.entity.AccountEntity
import com.example.savingbuddy.data.local.entity.CategoryEntity
import com.example.savingbuddy.data.local.entity.SavingsGoalEntity
import com.example.savingbuddy.data.local.entity.TransactionEntity

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        SavingsGoalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class SavingBuddyDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun savingsGoalDao(): SavingsGoalDao
}