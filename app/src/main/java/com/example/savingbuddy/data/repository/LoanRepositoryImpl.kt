package com.example.savingbuddy.data.repository

import com.example.savingbuddy.data.local.dao.LoanDao
import com.example.savingbuddy.data.local.entity.LoanEntity
import com.example.savingbuddy.domain.model.Loan
import com.example.savingbuddy.domain.model.LoanType
import com.example.savingbuddy.domain.repository.LoanRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LoanRepositoryImpl @Inject constructor(
    private val loanDao: LoanDao
) : LoanRepository {

    override fun getAllLoans(): Flow<List<Loan>> =
        loanDao.getAllLoans().map { list -> list.mapNotNull { entityToLoan(it) } }

    override fun getActiveLoans(): Flow<List<Loan>> =
        loanDao.getActiveLoans().map { list -> list.mapNotNull { entityToLoan(it) } }

    override suspend fun getLoanById(id: String): Loan? =
        entityToLoan(loanDao.getLoanById(id))

    override fun getTotalDebt(): Flow<Double> =
        loanDao.getTotalDebt().map { it ?: 0.0 }

    override suspend fun addLoan(loan: Loan) {
        loanDao.insertLoan(loanToEntity(loan))
    }

    override suspend fun updateLoan(loan: Loan) {
        loanDao.updateLoan(loanToEntity(loan))
    }

    override suspend fun deleteLoan(loan: Loan) {
        loanDao.deleteLoan(loanToEntity(loan))
    }

    private fun entityToLoan(entity: LoanEntity?): Loan? {
        return entity?.let {
            Loan(
                id = it.id,
                name = it.name,
                lenderName = it.lenderName,
                originalAmount = it.originalAmount,
                remainingAmount = it.remainingAmount,
                monthlyPayment = it.monthlyPayment,
                interestRate = it.interestRate,
                loanType = LoanType.valueOf(it.loanType),
                startDate = it.startDate,
                endDate = it.endDate,
                isActive = it.isActive,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
    }

    private fun loanToEntity(loan: Loan): LoanEntity {
        return LoanEntity(
            id = loan.id,
            name = loan.name,
            lenderName = loan.lenderName,
            originalAmount = loan.originalAmount,
            remainingAmount = loan.remainingAmount,
            monthlyPayment = loan.monthlyPayment,
            interestRate = loan.interestRate,
            loanType = loan.loanType.name,
            startDate = loan.startDate,
            endDate = loan.endDate,
            isActive = loan.isActive,
            createdAt = loan.createdAt,
            updatedAt = loan.updatedAt
        )
    }
}
