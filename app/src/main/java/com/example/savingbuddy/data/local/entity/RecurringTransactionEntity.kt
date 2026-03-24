package com.example.savingbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId"), Index("accountId")]
)
data class RecurringTransactionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val type: String,
    val recurringType: String,
    val startDate: Long,
    val endDate: Long? = null,
    val selectedDays: String? = null,
    val selectedDate: Int? = null,
    val isActive: Boolean = true,
    val categoryId: String,
    val accountId: String,
    val note: String? = null,
    val lastProcessedDate: Long? = null,
    val excludeHolidays: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderMinutesBefore: Int = 60,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
