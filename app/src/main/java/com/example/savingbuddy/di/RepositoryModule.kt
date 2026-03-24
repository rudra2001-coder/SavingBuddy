package com.example.savingbuddy.di

import com.example.savingbuddy.data.repository.AccountRepositoryImpl
import com.example.savingbuddy.data.repository.CategoryRepositoryImpl
import com.example.savingbuddy.data.repository.SavingsGoalRepositoryImpl
import com.example.savingbuddy.data.repository.TransactionRepositoryImpl
import com.example.savingbuddy.domain.repository.AccountRepository
import com.example.savingbuddy.domain.repository.CategoryRepository
import com.example.savingbuddy.domain.repository.SavingsGoalRepository
import com.example.savingbuddy.domain.repository.TransactionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAccountRepository(impl: AccountRepositoryImpl): AccountRepository

    @Binds
    @Singleton
    abstract fun bindTransactionRepository(impl: TransactionRepositoryImpl): TransactionRepository

    @Binds
    @Singleton
    abstract fun bindCategoryRepository(impl: CategoryRepositoryImpl): CategoryRepository

    @Binds
    @Singleton
    abstract fun bindSavingsGoalRepository(impl: SavingsGoalRepositoryImpl): SavingsGoalRepository
}