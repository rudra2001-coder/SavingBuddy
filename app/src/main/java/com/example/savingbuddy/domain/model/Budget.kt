package com.example.savingbuddy.domain.model

data class Budget(
    val id: String,
    val categoryId: String,
    val monthlyLimit: Double,
    val month: Int,
    val year: Int,
    val alertThreshold: Double = 0.8,
    val createdAt: Long,
    val updatedAt: Long
)
