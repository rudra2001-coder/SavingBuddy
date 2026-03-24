package com.example.savingbuddy.domain.model

enum class InsightType {
    SPENDING_INCREASE,
    SPENDING_DECREASE,
    CATEGORY_DOMINANCE,
    WEEKEND_SPENDING,
    DAILY_AVERAGE,
    BUDGET_WARNING
}

data class SpendingInsight(
    val id: String,
    val type: InsightType,
    val title: String,
    val description: String,
    val value: Double,
    val percentage: Double? = null,
    val categoryId: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
