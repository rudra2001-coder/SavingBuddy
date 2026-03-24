package com.example.savingbuddy.data.repository

import com.example.savingbuddy.data.local.dao.AccountDao
import com.example.savingbuddy.data.local.dao.CreditCardDao
import com.example.savingbuddy.data.local.dao.LoanDao
import com.example.savingbuddy.data.local.dao.SavingsGoalDao
import com.example.savingbuddy.domain.model.*
import com.example.savingbuddy.domain.repository.NetWorthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NetWorthRepositoryImpl @Inject constructor(
    private val accountDao: AccountDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val loanDao: LoanDao,
    private val creditCardDao: CreditCardDao
) : NetWorthRepository {

    override fun getNetWorth(): Flow<NetWorth> {
        return combine(
            accountDao.getAllAccounts(),
            savingsGoalDao.getAllSavingsGoals(),
            loanDao.getAllLoans(),
            creditCardDao.getAllCards()
        ) { accounts, savingsGoals, loans, creditCards ->
            val assets = mutableListOf<Asset>()
            var totalAssets = 0.0

            accounts.forEach { account ->
                val balance = account.balance
                totalAssets += balance
                assets.add(
                    Asset(
                        id = account.id,
                        name = account.name,
                        type = AssetType.ACCOUNT,
                        value = balance,
                        icon = account.icon
                    )
                )
            }

            savingsGoals.forEach { goal ->
                val currentAmount = goal.currentAmount
                totalAssets += currentAmount
                assets.add(
                    Asset(
                        id = goal.id,
                        name = goal.name,
                        type = AssetType.SAVINGS_GOAL,
                        value = currentAmount,
                        icon = goal.icon
                    )
                )
            }

            val liabilities = mutableListOf<Liability>()
            var totalLiabilities = 0.0

            loans.filter { it.isActive }.forEach { loan ->
                val amount = loan.remainingAmount
                totalLiabilities += amount
                liabilities.add(
                    Liability(
                        id = loan.id,
                        name = loan.name,
                        type = LiabilityType.LOAN,
                        amount = amount,
                        icon = "🏦"
                    )
                )
            }

            creditCards.filter { it.isActive }.forEach { card ->
                val balance = card.currentBalance
                totalLiabilities += balance
                liabilities.add(
                    Liability(
                        id = card.id,
                        name = card.name,
                        type = LiabilityType.CREDIT_CARD,
                        amount = balance,
                        icon = "💳"
                    )
                )
            }

            NetWorth(
                totalAssets = totalAssets,
                totalLiabilities = totalLiabilities,
                netWorth = totalAssets - totalLiabilities,
                assets = assets.sortedByDescending { it.value },
                liabilities = liabilities.sortedByDescending { it.amount },
                lastUpdated = System.currentTimeMillis()
            )
        }
    }

    override suspend fun calculateNetWorth(): NetWorth {
        val accounts = mutableListOf<Asset>()
        var totalAssets = 0.0

        accountDao.getAllAccounts().collect { list ->
            list.forEach { account ->
                totalAssets += account.balance
                accounts.add(
                    Asset(
                        id = account.id,
                        name = account.name,
                        type = AssetType.ACCOUNT,
                        value = account.balance,
                        icon = account.icon
                    )
                )
            }
        }

        savingsGoalDao.getAllSavingsGoals().collect { list ->
            list.forEach { goal ->
                totalAssets += goal.currentAmount
                accounts.add(
                    Asset(
                        id = goal.id,
                        name = goal.name,
                        type = AssetType.SAVINGS_GOAL,
                        value = goal.currentAmount,
                        icon = goal.icon
                    )
                )
            }
        }

        val liabilities = mutableListOf<Liability>()
        var totalLiabilities = 0.0

        loanDao.getAllLoans().collect { list ->
            list.filter { it.isActive }.forEach { loan ->
                totalLiabilities += loan.remainingAmount
                liabilities.add(
                    Liability(
                        id = loan.id,
                        name = loan.name,
                        type = LiabilityType.LOAN,
                        amount = loan.remainingAmount,
                        icon = "🏦"
                    )
                )
            }
        }

        creditCardDao.getAllCards().collect { list ->
            list.filter { it.isActive }.forEach { card ->
                totalLiabilities += card.currentBalance
                liabilities.add(
                    Liability(
                        id = card.id,
                        name = card.name,
                        type = LiabilityType.CREDIT_CARD,
                        amount = card.currentBalance,
                        icon = "💳"
                    )
                )
            }
        }

        return NetWorth(
            totalAssets = totalAssets,
            totalLiabilities = totalLiabilities,
            netWorth = totalAssets - totalLiabilities,
            assets = accounts.sortedByDescending { it.value },
            liabilities = liabilities.sortedByDescending { it.amount },
            lastUpdated = System.currentTimeMillis()
        )
    }
}