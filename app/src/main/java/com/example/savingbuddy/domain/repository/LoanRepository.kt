package com.example.savingbuddy.domain.repository

import com.example.savingbuddy.domain.model.Loan
import kotlinx.coroutines.flow.Flow

interface LoanRepository {
    fun getAllLoans(): Flow<List<Loan>>
    fun getActiveLoans(): Flow<List<Loan>>
    suspend fun getLoanById(id: String): Loan?
    fun getTotalDebt(): Flow<Double>
    suspend fun addLoan(loan: Loan)
    suspend fun updateLoan(loan: Loan)
    suspend fun deleteLoan(loan: Loan)
}
