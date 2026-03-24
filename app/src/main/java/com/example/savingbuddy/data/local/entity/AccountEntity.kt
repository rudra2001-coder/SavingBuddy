package com.example.savingbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val balance: Double = 0.0,
    val icon: String = "wallet",
    val color: Long = 0xFF4CAF50,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)