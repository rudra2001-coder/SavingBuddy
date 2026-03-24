package com.example.savingbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val icon: String = "savings",
    val color: Long = 0xFF2196F3,
    val deadline: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)