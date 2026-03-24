package com.example.savingbuddy.data.local.mapper

import com.example.savingbuddy.data.local.entity.AccountEntity
import com.example.savingbuddy.data.local.entity.CategoryEntity
import com.example.savingbuddy.data.local.entity.SavingsGoalEntity
import com.example.savingbuddy.data.local.entity.TransactionEntity
import com.example.savingbuddy.domain.model.Account
import com.example.savingbuddy.domain.model.Category
import com.example.savingbuddy.domain.model.SavingsGoal
import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.model.TransactionType

fun AccountEntity.toDomain() = Account(
    id = id,
    name = name,
    balance = balance,
    icon = icon,
    color = color,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isSynced = isSynced
)

fun Account.toEntity() = AccountEntity(
    id = id,
    name = name,
    balance = balance,
    icon = icon,
    color = color,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isSynced = isSynced
)

fun TransactionEntity.toDomain() = Transaction(
    id = id,
    amount = amount,
    type = TransactionType.valueOf(type),
    categoryId = categoryId,
    accountId = accountId,
    note = note,
    timestamp = timestamp,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isSynced = isSynced
)

fun Transaction.toEntity() = TransactionEntity(
    id = id,
    amount = amount,
    type = type.name,
    categoryId = categoryId,
    accountId = accountId,
    note = note,
    timestamp = timestamp,
    createdAt = createdAt,
    updatedAt = updatedAt,
    isSynced = isSynced
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    icon = icon,
    color = color,
    type = TransactionType.valueOf(type),
    isDefault = isDefault
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    icon = icon,
    color = color,
    type = type.name,
    isDefault = isDefault
)

fun SavingsGoalEntity.toDomain() = SavingsGoal(
    id = id,
    name = name,
    targetAmount = targetAmount,
    currentAmount = currentAmount,
    icon = icon,
    color = color,
    deadline = deadline,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun SavingsGoal.toEntity() = SavingsGoalEntity(
    id = id,
    name = name,
    targetAmount = targetAmount,
    currentAmount = currentAmount,
    icon = icon,
    color = color,
    deadline = deadline,
    createdAt = createdAt,
    updatedAt = updatedAt
)