package com.example.savingbuddy.data.repository

import com.example.savingbuddy.data.local.dao.AccountDao
import com.example.savingbuddy.data.local.entity.AccountEntity
import com.example.savingbuddy.domain.model.Account
import com.example.savingbuddy.domain.repository.AccountRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class AccountRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao
) : AccountRepository {

    override fun getAllAccounts(): Flow<List<Account>> =
        accountDao.getAllAccounts().map { list -> list.mapNotNull { entityToAccount(it) } }

    override suspend fun getAccountById(id: String): Account? =
        entityToAccount(accountDao.getAccountById(id))

    override fun getTotalBalance(): Flow<Double> =
        accountDao.getTotalBalance().map { it ?: 0.0 }

    override suspend fun addAccount(account: Account) {
        accountDao.insertAccount(accountToEntity(account))
    }

    override suspend fun updateAccount(account: Account) {
        accountDao.updateAccount(accountToEntity(account))
    }

    override suspend fun deleteAccount(account: Account) {
        accountDao.deleteAccount(accountToEntity(account))
    }

    override suspend fun updateBalance(accountId: String, amount: Double) {
        accountDao.updateBalance(accountId, amount)
    }

    private fun entityToAccount(entity: AccountEntity?): Account? {
        return entity?.let {
            Account(
                id = it.id,
                name = it.name,
                balance = it.balance,
                icon = it.icon,
                color = it.color,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt,
                isSynced = it.isSynced
            )
        }
    }

    private fun accountToEntity(account: Account): AccountEntity {
        return AccountEntity(
            id = account.id,
            name = account.name,
            balance = account.balance,
            icon = account.icon,
            color = account.color,
            createdAt = account.createdAt,
            updatedAt = account.updatedAt,
            isSynced = false
        )
    }
}
