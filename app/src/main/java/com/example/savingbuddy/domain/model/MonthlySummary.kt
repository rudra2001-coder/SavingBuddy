package com.example.savingbuddy.domain.model

data class MonthlySummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val balance: Double,
    val savingsRate: Float,
    val dailyAverage: Double,
    val categoryBreakdown: Map<String, Double>
)