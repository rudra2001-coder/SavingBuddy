package com.example.savingbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class CardType {
    VISA, MASTERCARD, AMEX, RUPAY, OTHER
}

@Entity(tableName = "credit_cards")
data class CreditCardEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val cardType: String,
    val lastFourDigits: String,
    val creditLimit: Double,
    val currentBalance: Double,
    val availableCredit: Double,
    val minimumPayment: Double,
    val dueDate: Int,
    val interestRate: Double,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
