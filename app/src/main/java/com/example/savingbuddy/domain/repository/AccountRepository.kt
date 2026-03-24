package com.example.savingbuddy.domain.repository

import com.example.savingbuddy.domain.model.Account
import kotlinx.coroutines.flow.Flow

interface AccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    suspend fun getAccountById(id: String): Account?
    fun getTotalBalance(): Flow<Double>
    suspend fun addAccount(account: Account)
    suspend fun updateAccount(account: Account)
    suspend fun deleteAccount(account: Account)
    suspend fun updateBalance(accountId: String, amount: Double)
}