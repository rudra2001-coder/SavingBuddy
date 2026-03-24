package com.example.savingbuddy.data.repository

import com.example.savingbuddy.data.local.dao.AccountDao
import com.example.savingbuddy.data.local.mapper.toDomain
import com.example.savingbuddy.data.local.mapper.toEntity
import com.example.savingbuddy.domain.model.Account
import com.example.savingbuddy.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAllAccounts(): Flow<List<Account>> =
        accountDao.getAllAccounts().map { list -> list.map { it.toDomain() } }

    override suspend fun getAccountById(id: String): Account? =
        accountDao.getAccountById(id)?.toDomain()

    override fun getTotalBalance(): Flow<Double> =
        accountDao.getTotalBalance().map { it ?: 0.0 }

    override suspend fun addAccount(account: Account) {
        accountDao.insertAccount(account.toEntity())
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(account.toEntity())
    }

    override suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(account.toEntity())
    }

    override suspend fun updateBalance(accountId: String, amount: Double) {
        accountDao.updateBalance(accountId, amount)
    }
}