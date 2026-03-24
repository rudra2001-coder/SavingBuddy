package com.example.savingbuddy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.savingbuddy.data.local.dao.*
import com.example.savingbuddy.data.local.entity.*

@Database(
    entities = [
        AccountEntity::class,
        TransactionEntity::class,
        CategoryEntity::class,
        SavingsGoalEntity::class,
        BudgetEntity::class,
        RecurringTransactionEntity::class,
        LoanEntity::class,
        CreditCardEntity::class,
        HealthEntryEntity::class,
        JournalEntryEntity::class,
        HabitEntity::class,
        HabitLogEntity::class,
        AchievementEntity::class,
        FocusSessionEntity::class,
        TaskEntity::class,
        MindfulSessionEntity::class,
        UserPreferencesEntity::class,
        WorkLogEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SavingBuddyDatabase : RoomDatabase() {
    abstract fun accountDao(): AccountDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun savingsGoalDao(): SavingsGoalDao
    abstract fun budgetDao(): BudgetDao
    abstract fun recurringTransactionDao(): RecurringTransactionDao
    abstract fun loanDao(): LoanDao
    abstract fun creditCardDao(): CreditCardDao
    abstract fun healthDao(): HealthDao
    abstract fun journalDao(): JournalDao
    abstract fun habitDao(): HabitDao
    abstract fun achievementDao(): AchievementDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun taskDao(): TaskDao
    abstract fun mindfulSessionDao(): MindfulSessionDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun workLogDao(): WorkLogDao
}
