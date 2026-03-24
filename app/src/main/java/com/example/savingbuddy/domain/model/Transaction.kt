package com.example.savingbuddy.domain.model

enum class TransactionType {
    INCOME, EXPENSE, TRANSFER
}

data class Transaction(
    val id: String,
    val amount: Double,
    val type: TransactionType,
    val categoryId: String,
    val accountId: String,
    val note: String?,
    val timestamp: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isSynced: Boolean
)