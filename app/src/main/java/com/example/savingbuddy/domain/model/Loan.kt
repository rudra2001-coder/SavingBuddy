package com.example.savingbuddy.domain.model

enum class LoanType {
    PERSONAL, HOME, CAR, EDUCATION, BUSINESS, OTHER
}

data class Loan(
    val id: String,
    val name: String,
    val lenderName: String,
    val originalAmount: Double,
    val remainingAmount: Double,
    val monthlyPayment: Double,
    val interestRate: Double,
    val loanType: LoanType,
    val startDate: Long,
    val endDate: Long?,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    val paidAmount: Double get() = originalAmount - remainingAmount
    val progress: Double get() = if (originalAmount > 0) paidAmount / originalAmount else 0.0
}
