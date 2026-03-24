package com.example.savingbuddy.domain.usecase

import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.model.TransactionType
import java.util.Calendar
import javax.inject.Inject
import kotlin.math.pow

data class FutureProjection(
    val months: Int,
    val expectedSavings: Double,
    val optimizedSavings: Double,
    val monthlyIncome: Double,
    val monthlyExpenses: Double,
    val savingsRate: Double,
    val recommendations: List<String>
)

data class SpendingInsightData(
    val averageDailySpending: Double,
    val topCategory: String,
    val topCategoryAmount: Double,
    val weekendSpending: Double,
    val weekdaySpending: Double,
    val trend: String
)

class FutureRealityCheckerUseCase @Inject constructor() {

    fun calculateProjection(
        transactions: List<Transaction>,
        monthlyIncome: Double,
        monthlyBudget: Map<String, Double>?
    ): FutureProjection {
        val now = Calendar.getInstance()
        val thirtyDaysAgo = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -30)
        }.timeInMillis

        val recentTransactions = transactions.filter { 
            it.timestamp >= thirtyDaysAgo && it.type == TransactionType.EXPENSE 
        }
        
        val monthlyExpenses = recentTransactions.sumOf { it.amount }
        val savingsRate = if (monthlyIncome > 0) ((monthlyIncome - monthlyExpenses) / monthlyIncome) * 100 else 0.0
        val expectedSavings = monthlyIncome - monthlyExpenses

        val categorySpending = recentTransactions
            .groupBy { it.categoryId }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }
        
        val topCategory = categorySpending.maxByOrNull { it.value }?.key ?: "Food"
        val topCategoryAmount = categorySpending.maxByOrNull { it.value }?.value ?: 0.0

        val optimizedSavings = if (topCategoryAmount > monthlyIncome * 0.15) {
            expectedSavings + (topCategoryAmount * 0.3)
        } else {
            expectedSavings * 1.2
        }

        val recommendations = generateRecommendations(
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            savingsRate = savingsRate,
            topCategoryAmount = topCategoryAmount,
            categorySpending = categorySpending
        )

        return FutureProjection(
            months = 6,
            expectedSavings = expectedSavings * 6,
            optimizedSavings = optimizedSavings * 6,
            monthlyIncome = monthlyIncome,
            monthlyExpenses = monthlyExpenses,
            savingsRate = savingsRate,
            recommendations = recommendations
        )
    }

    private fun generateRecommendations(
        monthlyIncome: Double,
        monthlyExpenses: Double,
        savingsRate: Double,
        topCategoryAmount: Double,
        categorySpending: Map<String, Double>
    ): List<String> {
        val recommendations = mutableListOf<String>()

        if (savingsRate < 10) {
            recommendations.add("Your savings rate is low. Try to save at least 20% of income.")
        }

        if (topCategoryAmount > monthlyIncome * 0.2) {
            recommendations.add("Your top spending category is very high. Consider reducing it by 30%.")
        }

        if (monthlyExpenses > monthlyIncome) {
            recommendations.add("CRITICAL: You are spending more than you earn!")
        }

        val foodSpending = categorySpending.filter { 
            it.key.contains("food", ignoreCase = true) || it.key.contains("restaurant", ignoreCase = true)
        }.values.sum()

        if (foodSpending > monthlyIncome * 0.15) {
            recommendations.add("Food expenses are high. Cooking at home could save you money.")
        }

        if (savingsRate >= 20) {
            recommendations.add("Great job! You're saving well. Consider investing for better returns.")
        }

        return recommendations
    }

    fun analyzeSpendingPatterns(transactions: List<Transaction>): SpendingInsightData {
        val now = Calendar.getInstance()
        val thirtyDaysAgo = (now.clone() as Calendar).apply {
            add(Calendar.DAY_OF_MONTH, -30)
        }.timeInMillis

        val recentTxns = transactions.filter { it.timestamp >= thirtyDaysAgo && it.type == TransactionType.EXPENSE }
        
        val dailySpending = if (recentTxns.isNotEmpty()) {
            recentTxns.sumOf { it.amount } / 30.0
        } else 0.0

        val weekendSpending = recentTxns.filter { txn ->
            val cal = Calendar.getInstance().apply { timeInMillis = txn.timestamp }
            val day = cal.get(Calendar.DAY_OF_WEEK)
            day == Calendar.SATURDAY || day == Calendar.SUNDAY
        }.sumOf { it.amount }

        val weekdaySpending = recentTxns.sumOf { it.amount } - weekendSpending

        val categorySpending = recentTxns
            .groupBy { it.categoryId }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }

        val topCategory = categorySpending.maxByOrNull { it.value }?.key ?: "None"
        val topCategoryAmount = categorySpending.maxByOrNull { it.value }?.value ?: 0.0

        val trend = when {
            weekendSpending > weekdaySpending * 0.5 -> "High weekend spending"
            dailySpending > 1000 -> "High daily spending"
            else -> "Stable spending"
        }

        return SpendingInsightData(
            averageDailySpending = dailySpending,
            topCategory = topCategory,
            topCategoryAmount = topCategoryAmount,
            weekendSpending = weekendSpending,
            weekdaySpending = weekdaySpending,
            trend = trend
        )
    }
}
