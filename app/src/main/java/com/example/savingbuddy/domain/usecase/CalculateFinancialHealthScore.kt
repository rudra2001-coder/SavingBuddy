package com.example.savingbuddy.domain.usecase

import com.example.savingbuddy.data.local.dao.*
import com.example.savingbuddy.data.local.dao.BudgetDao
import com.example.savingbuddy.data.local.dao.SavingsGoalDao
import com.example.savingbuddy.data.local.dao.TransactionDao
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

data class FinancialHealthScore(
    val overallScore: Int,
    val savingsRateScore: Int,
    val budgetAdherenceScore: Int,
    val debtScore: Int,
    val emergencyFundScore: Int,
    val grade: String,
    val recommendations: List<String>
)

@Singleton
class CalculateFinancialHealthScore @Inject constructor(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val loanDao: LoanDao,
    private val creditCardDao: CreditCardDao,
    private val accountDao: AccountDao
) {
    suspend fun calculate(monthlyIncome: Double): FinancialHealthScore {
        val recommendations = mutableListOf<String>()
        
        // 1. Savings Rate Score (0-100)
        // Good: >20%, OK: 10-20%, Poor: <10%
        val thisMonth = java.util.Calendar.getInstance()
        val startOfMonth = thisMonth.apply {
            set(java.util.Calendar.DAY_OF_MONTH, 1)
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
        }.timeInMillis
        val endOfMonth = thisMonth.apply {
            set(java.util.Calendar.DAY_OF_MONTH, getActualMaximum(java.util.Calendar.DAY_OF_MONTH))
            set(java.util.Calendar.HOUR_OF_DAY, 23)
            set(java.util.Calendar.MINUTE, 59)
            set(java.util.Calendar.SECOND, 59)
        }.timeInMillis

        val income = transactionDao.getTotalIncome(startOfMonth, endOfMonth).first() ?: 0.0
        val expenses = transactionDao.getTotalExpense(startOfMonth, endOfMonth).first() ?: 0.0
        val savingsRate = if (income > 0) ((income - expenses) / income * 100).toInt() else 0
        val savingsRateScore = when {
            savingsRate >= 20 -> 100
            savingsRate >= 15 -> 80
            savingsRate >= 10 -> 60
            savingsRate >= 5 -> 40
            else -> 20
        }
        if (savingsRate < 10) recommendations.add("Try to save at least 10% of your income")

        // 2. Budget Adherence Score (0-100)
        val budgets = budgetDao.getBudgetsForMonth(thisMonth.get(java.util.Calendar.MONTH), thisMonth.get(java.util.Calendar.YEAR)).first()
        var budgetOverCount = 0
        budgets.forEach { budget ->
            val spent = budgets.sumOf { it.monthlyLimit }
            if (spent > budget.monthlyLimit) budgetOverCount++
        }
        val budgetAdherenceScore = if (budgets.isEmpty()) 70 else {
            val adherence = ((budgets.size - budgetOverCount) * 100) / budgets.size
            maxOf(adherence, 20)
        }
        if (budgetAdherenceScore < 60) recommendations.add("Review your budget - you're overspending in some categories")

        // 3. Debt Score (0-100, inverse - less debt is better)
        val totalDebt = (loanDao.getTotalDebt().first() ?: 0.0) + (creditCardDao.getTotalBalance().first() ?: 0.0)
        val debtRatio = if (monthlyIncome > 0) (totalDebt / (monthlyIncome * 12)) else 0.0 // Debt to annual income
        val debtScore = when {
            debtRatio == 0.0 -> 100
            debtRatio <= 0.25 -> 80
            debtRatio <= 0.5 -> 60
            debtRatio <= 1.0 -> 40
            else -> 20
        }
        if (debtRatio > 0.5) recommendations.add("Focus on paying down high-interest debt")

        // 4. Emergency Fund Score (0-100)
        val totalSavings = accountDao.getTotalBalance().first() ?: 0.0
        val monthlyExpenses = if (expenses > 0) expenses else income * 0.8
        val monthsOfSavings = if (monthlyExpenses > 0) (totalSavings / monthlyExpenses).toInt() else 0
        val emergencyFundScore = when {
            monthsOfSavings >= 6 -> 100
            monthsOfSavings >= 3 -> 80
            monthsOfSavings >= 1 -> 50
            else -> 20
        }
        if (monthsOfSavings < 3) recommendations.add("Build an emergency fund - aim for 3-6 months of expenses")

        // Calculate overall score
        val overallScore = (savingsRateScore + budgetAdherenceScore + debtScore + emergencyFundScore) / 4
        
        // Determine grade
        val grade = when {
            overallScore >= 90 -> "A+"
            overallScore >= 80 -> "A"
            overallScore >= 70 -> "B"
            overallScore >= 60 -> "C"
            overallScore >= 50 -> "D"
            else -> "F"
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Great job! Keep up the good financial habits")
        }

        return FinancialHealthScore(
            overallScore = overallScore,
            savingsRateScore = savingsRateScore,
            budgetAdherenceScore = budgetAdherenceScore,
            debtScore = debtScore,
            emergencyFundScore = emergencyFundScore,
            grade = grade,
            recommendations = recommendations
        )
    }
}