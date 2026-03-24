package com.example.savingbuddy.domain.model

data class SavingsGoal(
    val id: String,
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double,
    val icon: String,
    val color: Long,
    val deadline: Long?,
    val createdAt: Long,
    val updatedAt: Long
) {
    val progress: Float get() = if (targetAmount > 0) (currentAmount / targetAmount).toFloat().coerceIn(0f, 1f) else 0f
    val remainingAmount: Double get() = (targetAmount - currentAmount).coerceAtLeast(0.0)
    val isCompleted: Boolean get() = currentAmount >= targetAmount
}