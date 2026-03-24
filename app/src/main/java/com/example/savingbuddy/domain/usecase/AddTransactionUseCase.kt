package com.example.savingbuddy.domain.usecase

import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.model.TransactionType
import com.example.savingbuddy.domain.repository.AccountRepository
import com.example.savingbuddy.domain.repository.TransactionRepository
import javax.inject.Inject

class AddTransactionUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    suspend operator fun invoke(transaction: Transaction, fromAccountId: String?, toAccountId: String?) {
        transactionRepository.addTransaction(transaction)

        when (transaction.type) {
            TransactionType.EXPENSE -> {
                accountRepository.updateBalance(fromAccountId!!, -transaction.amount)
            }
            TransactionType.INCOME -> {
                accountRepository.updateBalance(fromAccountId!!, transaction.amount)
            }
            TransactionType.TRANSFER -> {
                accountRepository.updateBalance(fromAccountId!!, -transaction.amount)
                accountRepository.updateBalance(toAccountId!!, transaction.amount)
            }
        }
    }
}