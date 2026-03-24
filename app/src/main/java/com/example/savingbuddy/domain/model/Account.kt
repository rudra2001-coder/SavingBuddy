package com.example.savingbuddy.domain.model

data class Account(
    val id: String,
    val name: String,
    val balance: Double,
    val icon: String,
    val color: Long,
    val createdAt: Long,
    val updatedAt: Long,
    val isSynced: Boolean
)