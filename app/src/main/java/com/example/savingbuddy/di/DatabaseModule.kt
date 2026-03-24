package com.example.savingbuddy.di

import android.content.Context
import androidx.room.Room
import com.example.savingbuddy.data.local.SavingBuddyDatabase
import com.example.savingbuddy.data.local.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SavingBuddyDatabase {
        return Room.databaseBuilder(
            context,
            SavingBuddyDatabase::class.java,
            "saving_buddy_db"
        ).build()
    }

    @Provides
    fun provideAccountDao(database: SavingBuddyDatabase): AccountDao = database.accountDao()

    @Provides
    fun provideTransactionDao(database: SavingBuddyDatabase): TransactionDao = database.transactionDao()

    @Provides
    fun provideCategoryDao(database: SavingBuddyDatabase): CategoryDao = database.categoryDao()

    @Provides
    fun provideSavingsGoalDao(database: SavingBuddyDatabase): SavingsGoalDao = database.savingsGoalDao()

    @Provides
    fun provideBudgetDao(database: SavingBuddyDatabase): BudgetDao = database.budgetDao()

    @Provides
    fun provideRecurringTransactionDao(database: SavingBuddyDatabase): RecurringTransactionDao = database.recurringTransactionDao()

    @Provides
    fun provideLoanDao(database: SavingBuddyDatabase): LoanDao = database.loanDao()

    @Provides
    fun provideCreditCardDao(database: SavingBuddyDatabase): CreditCardDao = database.creditCardDao()

    @Provides
    fun provideHealthDao(database: SavingBuddyDatabase): HealthDao = database.healthDao()

    @Provides
    fun provideJournalDao(database: SavingBuddyDatabase): JournalDao = database.journalDao()

    @Provides
    fun provideHabitDao(database: SavingBuddyDatabase): HabitDao = database.habitDao()

    @Provides
    fun provideAchievementDao(database: SavingBuddyDatabase): AchievementDao = database.achievementDao()

    @Provides
    fun provideFocusSessionDao(database: SavingBuddyDatabase): FocusSessionDao = database.focusSessionDao()

    @Provides
    fun provideTaskDao(database: SavingBuddyDatabase): TaskDao = database.taskDao()

    @Provides
    fun provideMindfulSessionDao(database: SavingBuddyDatabase): MindfulSessionDao = database.mindfulSessionDao()

    @Provides
    fun provideUserPreferencesDao(database: SavingBuddyDatabase): UserPreferencesDao = database.userPreferencesDao()

    @Provides
    fun provideWorkLogDao(database: SavingBuddyDatabase): WorkLogDao = database.workLogDao()
}
