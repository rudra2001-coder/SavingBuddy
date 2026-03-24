package com.example.savingbuddy.domain.usecase

import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRecentTransactionsUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository
) {
    operator fun invoke(limit: Int = 10): Flow<List<Transaction>> =
        transactionRepository.getRecentTransactions(limit)
}