package com.example.savingbuddy.domain.usecase

import com.example.savingbuddy.domain.model.InsightType
import com.example.savingbuddy.domain.model.SpendingInsight
import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

class GetSpendingInsightsUseCase @Inject constructor() {

    fun analyzeSpending(
        currentPeriodTransactions: List<Transaction>,
        previousPeriodTransactions: List<Transaction>,
        categoryNames: Map<String, String>
    ): List<SpendingInsight> {
        val insights = mutableListOf<SpendingInsight>()

        val currentTotal = currentPeriodTransactions.filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        val previousTotal = previousPeriodTransactions.filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }

        if (previousTotal > 0 && currentTotal > 0) {
            val percentageChange = ((currentTotal - previousTotal) / previousTotal) * 100
            
            if (percentageChange > 10) {
                insights.add(
                    SpendingInsight(
                        id = UUID.randomUUID().toString(),
                        type = InsightType.SPENDING_INCREASE,
                        title = "Spending Increased",
                        description = "Your expenses increased by ${String.format("%.1f", percentageChange)}% compared to last period",
                        value = currentTotal,
                        percentage = percentageChange
                    )
                )
            } else if (percentageChange < -10) {
                insights.add(
                    SpendingInsight(
                        id = UUID.randomUUID().toString(),
                        type = InsightType.SPENDING_DECREASE,
                        title = "Great Spending Control!",
                        description = "Your expenses decreased by ${String.format("%.1f", -percentageChange)}% compared to last period",
                        value = currentTotal,
                        percentage = percentageChange
                    )
                )
            }
        }

        val categorySpending = currentPeriodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { it.categoryId }
            .mapValues { (_, transactions) -> transactions.sumOf { it.amount } }

        if (categorySpending.isNotEmpty() && currentTotal > 0) {
            val dominantCategory = categorySpending.maxByOrNull { it.value }
            dominantCategory?.let { (catId, amount) ->
                val percentage = (amount / currentTotal) * 100
                if (percentage > 30) {
                    insights.add(
                        SpendingInsight(
                            id = UUID.randomUUID().toString(),
                            type = InsightType.CATEGORY_DOMINANCE,
                            title = "Top Spending Category",
                            description = "${categoryNames[catId] ?: "Unknown"} accounts for ${String.format("%.0f", percentage)}% of your expenses",
                            value = amount,
                            percentage = percentage,
                            categoryId = catId
                        )
                    )
                }
            }
        }

        val weekendSpending = currentPeriodTransactions
            .filter { it.type == TransactionType.EXPENSE }
            .filter { transaction ->
                val calendar = Calendar.getInstance().apply { timeInMillis = transaction.timestamp }
                val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
                dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY
            }
            .sumOf { it.amount }

        val weekdaySpending = currentTotal - weekendSpending
        if (weekendSpending > 0 && currentTotal > 0) {
            val weekendPercentage = (weekendSpending / currentTotal) * 100
            if (weekendPercentage > 40) {
                insights.add(
                    SpendingInsight(
                        id = UUID.randomUUID().toString(),
                        type = InsightType.WEEKEND_SPENDING,
                        title = "Weekend Spending Alert",
                        description = "You spend ${String.format("%.0f", weekendPercentage)}% of your expenses on weekends",
                        value = weekendSpending,
                        percentage = weekendPercentage
                    )
                )
            }
        }

        if (currentPeriodTransactions.isNotEmpty()) {
            val calendar = Calendar.getInstance().apply { 
                timeInMillis = currentPeriodTransactions.maxOfOrNull { it.timestamp } ?: System.currentTimeMillis()
            }
            val today = Calendar.getInstance()
            val daysInPeriod = maxOf(1, ((today.timeInMillis - (currentPeriodTransactions.minOfOrNull { it.timestamp } ?: today.timeInMillis)) / (1000 * 60 * 60 * 24)).toInt())
            
            val dailyAverage = currentTotal / daysInPeriod
            
            insights.add(
                SpendingInsight(
                    id = UUID.randomUUID().toString(),
                    type = InsightType.DAILY_AVERAGE,
                    title = "Daily Spending Average",
                    description = "You spend an average of ৳${String.format("%.0f", dailyAverage)} per day this period",
                    value = dailyAverage
                )
            )
        }

        return insights.sortedByDescending { it.createdAt }
    }
}
