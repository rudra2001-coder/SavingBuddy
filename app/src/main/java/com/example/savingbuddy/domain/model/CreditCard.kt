package com.example.savingbuddy.domain.model

enum class CardType {
    VISA, MASTERCARD, AMEX, RUPAY, OTHER
}

data class CreditCard(
    val id: String,
    val name: String,
    val cardType: CardType,
    val lastFourDigits: String,
    val creditLimit: Double,
    val currentBalance: Double,
    val availableCredit: Double,
    val minimumPayment: Double,
    val dueDate: Int,
    val interestRate: Double,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
) {
    val utilizationPercentage: Double get() = if (creditLimit > 0) (currentBalance / creditLimit) * 100 else 0.0
    val isOverLimit: Boolean get() = currentBalance > creditLimit
    val displayName: String get() = "$name •••• $lastFourDigits"
}
