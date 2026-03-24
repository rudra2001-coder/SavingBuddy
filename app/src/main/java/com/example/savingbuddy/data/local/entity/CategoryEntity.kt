package com.example.savingbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val icon: String,
    val color: Long,
    val type: String,
    val isDefault: Boolean = false
)