package com.example.savingbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class LoanType {
    PERSONAL, HOME, CAR, EDUCATION, BUSINESS, OTHER
}

@Entity(tableName = "loans")
data class LoanEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val lenderName: String,
    val originalAmount: Double,
    val remainingAmount: Double,
    val monthlyPayment: Double,
    val interestRate: Double,
    val loanType: String,
    val startDate: Long,
    val endDate: Long?,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
