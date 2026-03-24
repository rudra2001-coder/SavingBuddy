package com.example.savingbuddy.domain.usecase

import com.example.savingbuddy.domain.model.Budget
import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.util.Calendar
import javax.inject.Inject

enum class SpendingSafetyLevel {
    SAFE,        // Spending is well within budget
    MODERATE,    // Spending is acceptable but notable
    WARNING,     // Spending may cause issues
    DANGEROUS   // Spending is harmful to financial health
}

data class SpendingAdvice(
    val safetyLevel: SpendingSafetyLevel,
    val title: String,
    val message: String,
    val remainingBudget: Double,
    val dailyRemaining: Double,
    val percentOfBudget: Double,
    val percentOfDaily: Double
)

class SpendingAdvisorUseCase @Inject constructor() {

    fun analyzeSpending(
        amount: Double,
        categoryId: String,
        currentMonthTransactions: List<Transaction>,
        categoryBudget: Budget?,
        totalIncome: Double,
        savingsGoals: List<com.example.savingbuddy.domain.model.SavingsGoal>
    ): SpendingAdvice {
        
        val currentMonthSpending = currentMonthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        
        val categorySpending = currentMonthTransactions
            .filter { it.type == TransactionType.EXPENSE && it.categoryId == categoryId }
            .sumOf { it.amount }
        
        val totalSpending = currentMonthSpending + amount
        val categoryTotalSpending = categorySpending + amount
        
        // Calculate daily budget
        val calendar = Calendar.getInstance()
        val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val today = calendar.get(Calendar.DAY_OF_MONTH)
        val daysRemaining = daysInMonth - today + 1
        
        val dailyBudget = if (totalIncome > 0) {
            totalIncome / daysInMonth
        } else 0.0
        
        val dailyRemaining = if (totalIncome > 0) {
            ((totalIncome - currentMonthSpending) / daysRemaining).coerceAtLeast(0.0)
        } else 0.0
        
        // Budget analysis
        var remainingBudget = Double.MAX_VALUE
        var budgetPercent = 0.0
        
        categoryBudget?.let { budget ->
            remainingBudget = budget.monthlyLimit - categorySpending
            budgetPercent = if (budget.monthlyLimit > 0) {
                (categoryTotalSpending / budget.monthlyLimit) * 100
            } else 0.0
        }
        
        // Calculate 50/30/20 rule analysis
        val needsLimit = totalIncome * 0.50
        val wantsLimit = totalIncome * 0.30
        val savingsTarget = totalIncome * 0.20
        
        val currentNeeds = currentMonthTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        
        val needsPercent = if (needsLimit > 0) (currentNeeds / needsLimit) * 100 else 0.0
        
        // Determine safety level
        val safetyLevel = calculateSafetyLevel(
            amount = amount,
            remainingBudget = remainingBudget,
            dailyRemaining = dailyRemaining,
            totalIncome = totalIncome,
            totalSpending = totalSpending,
            savingsTarget = savingsTarget,
            currentNeeds = currentNeeds,
            needsLimit = needsLimit
        )
        
        // Generate appropriate message
        val (title, message) = generateAdviceMessage(
            safetyLevel = safetyLevel,
            amount = amount,
            remainingBudget = remainingBudget,
            dailyRemaining = dailyRemaining,
            budgetPercent = budgetPercent,
            percentOfDaily = if (dailyBudget > 0) (amount / dailyBudget) * 100 else 0.0,
            totalIncome = totalIncome,
            currentSpending = currentMonthSpending,
            savingsTarget = savingsTarget
        )
        
        return SpendingAdvice(
            safetyLevel = safetyLevel,
            title = title,
            message = message,
            remainingBudget = remainingBudget,
            dailyRemaining = dailyRemaining,
            percentOfBudget = budgetPercent,
            percentOfDaily = if (dailyBudget > 0) (amount / dailyBudget) * 100 else 0.0
        )
    }
    
    private fun calculateSafetyLevel(
        amount: Double,
        remainingBudget: Double,
        dailyRemaining: Double,
        totalIncome: Double,
        totalSpending: Double,
        savingsTarget: Double,
        currentNeeds: Double,
        needsLimit: Double
    ): SpendingSafetyLevel {
        
        // Check if exceeds daily budget
        if (amount > dailyRemaining && dailyRemaining > 0) {
            return SpendingSafetyLevel.WARNING
        }
        
        // Check if exceeds remaining category budget
        if (amount > remainingBudget && remainingBudget < Double.MAX_VALUE) {
            return SpendingSafetyLevel.DANGEROUS
        }
        
        // Check 50/30/20 rule - needs should not exceed 50%
        val projectedNeeds = currentNeeds + amount
        if (needsLimit > 0 && projectedNeeds > needsLimit) {
            return SpendingSafetyLevel.DANGEROUS
        }
        
        // Check if spending exceeds income
        if (totalSpending > totalIncome && totalIncome > 0) {
            return SpendingSafetyLevel.WARNING
        }
        
        // Check savings impact
        val projectedSavings = totalIncome - totalSpending
        if (projectedSavings < savingsTarget * 0.5 && savingsTarget > 0) {
            return SpendingSafetyLevel.MODERATE
        }
        
        // Default to safe
        return SpendingSafetyLevel.SAFE
    }
    
    private fun generateAdviceMessage(
        safetyLevel: SpendingSafetyLevel,
        amount: Double,
        remainingBudget: Double,
        dailyRemaining: Double,
        budgetPercent: Double,
        percentOfDaily: Double,
        totalIncome: Double,
        currentSpending: Double,
        savingsTarget: Double
    ): Pair<String, String> {
        
        return when (safetyLevel) {
            SpendingSafetyLevel.SAFE -> "✅ Safe to Spend" to "This amount fits well within your budget. You're on track with your financial goals!"
            
            SpendingSafetyLevel.MODERATE -> "⚠️ Moderate Spending" to {
                val savingsMissed = savingsTarget - (totalIncome - currentSpending - amount)
                if (savingsMissed > 0) {
                    "This spend will reduce your potential savings by ৳${String.format("%.0f", savingsMissed)}. Consider if it's essential."
                } else {
                    "This is a notable expense. Make sure it aligns with your priorities."
                }
            }.invoke()
            
            SpendingSafetyLevel.WARNING -> "🔴 Warning!" to {
                val build = StringBuilder()
                if (amount > dailyRemaining && dailyRemaining > 0) {
                    build.append("This exceeds your daily budget (৳${String.format("%.0f", dailyRemaining)} remaining). ")
                }
                if (totalIncome > 0 && currentSpending + amount > totalIncome * 0.9) {
                    build.append("You're approaching your monthly income limit!")
                }
                build.toString()
            }.invoke()
            
            SpendingSafetyLevel.DANGEROUS -> "🚨 Harmful!" to {
                val build = StringBuilder()
                if (remainingBudget < Double.MAX_VALUE && amount > remainingBudget) {
                    build.append("This exceeds your ${if (remainingBudget > 0) "remaining budget (৳${String.format("%.0f", remainingBudget)})" else "budget"}! ")
                }
                if (totalIncome > 0 && currentSpending + amount > totalIncome) {
                    build.append("This would put you OVER your monthly income! ")
                }
                build.append("This spending could harm your financial health. Please reconsider.")
                build.toString()
            }.invoke()
        }
    }
}
