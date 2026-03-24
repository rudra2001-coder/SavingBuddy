package com.example.savingbuddy.di

import android.content.Context
import androidx.room.Room
import com.example.savingbuddy.data.local.SavingBuddyDatabase
import com.example.savingbuddy.data.local.dao.AccountDao
import com.example.savingbuddy.data.local.dao.CategoryDao
import com.example.savingbuddy.data.local.dao.SavingsGoalDao
import com.example.savingbuddy.data.local.dao.TransactionDao
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
}