package com.example.savingbuddy.domain.usecase

import com.example.savingbuddy.domain.model.MonthlySummary
import com.example.savingbuddy.domain.model.TransactionType
import com.example.savingbuddy.domain.repository.AccountRepository
import com.example.savingbuddy.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.util.Calendar
import javax.inject.Inject

class GetMonthlySummaryUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository
) {
    operator fun invoke(): Flow<MonthlySummary> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startOfMonth = calendar.timeInMillis

        calendar.add(Calendar.MONTH, 1)
        calendar.add(Calendar.MILLISECOND, -1)
        val endOfMonth = calendar.timeInMillis

        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)

        return combine(
            transactionRepository.getTotalIncome(startOfMonth, endOfMonth),
            transactionRepository.getTotalExpense(startOfMonth, endOfMonth),
            accountRepository.getTotalBalance()
        ) { income, expense, balance ->
            val savingsRate = if (income > 0) ((income - expense) / income * 100).toFloat() else 0f
            val dailyAverage = if (expense > 0) expense / daysInMonth else 0.0
            MonthlySummary(
                totalIncome = income,
                totalExpense = expense,
                balance = balance,
                savingsRate = savingsRate,
                dailyAverage = dailyAverage,
                categoryBreakdown = emptyMap()
            )
        }
    }
}