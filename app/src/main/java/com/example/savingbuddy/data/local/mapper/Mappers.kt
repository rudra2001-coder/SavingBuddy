package com.example.savingbuddy.data.local.mapper

import com.example.savingbuddy.data.local.entity.AccountEntity
import com.example.savingbuddy.data.local.entity.BudgetEntity
import com.example.savingbuddy.data.local.entity.CategoryEntity
import com.example.savingbuddy.data.local.entity.CreditCardEntity
import com.example.savingbuddy.data.local.entity.LoanEntity
import com.example.savingbuddy.data.local.entity.RecurringTransactionEntity
import com.example.savingbuddy.data.local.entity.SavingsGoalEntity
import com.example.savingbuddy.data.local.entity.TransactionEntity
import com.example.savingbuddy.domain.model.Account
import com.example.savingbuddy.domain.model.Budget
import com.example.savingbuddy.domain.model.CardType
import com.example.savingbuddy.domain.model.Category
import com.example.savingbuddy.domain.model.CreditCard
import com.example.savingbuddy.domain.model.Loan
import com.example.savingbuddy.domain.model.LoanType
import com.example.savingbuddy.domain.model.RecurringTransaction
import com.example.savingbuddy.domain.model.RecurringType
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

fun BudgetEntity.toDomain() = Budget(
    id = id,
    categoryId = categoryId,
    monthlyLimit = monthlyLimit,
    month = month,
    year = year,
    alertThreshold = alertThreshold,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Budget.toEntity() = BudgetEntity(
    id = id,
    categoryId = categoryId,
    monthlyLimit = monthlyLimit,
    month = month,
    year = year,
    alertThreshold = alertThreshold,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun RecurringTransactionEntity.toDomain() = RecurringTransaction(
    id = id,
    title = title,
    amount = amount,
    type = TransactionType.valueOf(type),
    recurringType = RecurringType.valueOf(recurringType),
    startDate = startDate,
    endDate = endDate,
    selectedDays = selectedDays?.split(",")?.mapNotNull { it.toIntOrNull() },
    selectedDate = selectedDate,
    isActive = isActive,
    categoryId = categoryId,
    accountId = accountId,
    note = note,
    lastProcessedDate = lastProcessedDate,
    excludeHolidays = excludeHolidays,
    reminderEnabled = reminderEnabled,
    reminderMinutesBefore = reminderMinutesBefore,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun RecurringTransaction.toEntity() = RecurringTransactionEntity(
    id = id,
    title = title,
    amount = amount,
    type = type.name,
    recurringType = recurringType.name,
    startDate = startDate,
    endDate = endDate,
    selectedDays = selectedDays?.joinToString(","),
    selectedDate = selectedDate,
    isActive = isActive,
    categoryId = categoryId,
    accountId = accountId,
    note = note,
    lastProcessedDate = lastProcessedDate,
    excludeHolidays = excludeHolidays,
    reminderEnabled = reminderEnabled,
    reminderMinutesBefore = reminderMinutesBefore,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun LoanEntity.toDomain() = Loan(
    id = id,
    name = name,
    lenderName = lenderName,
    originalAmount = originalAmount,
    remainingAmount = remainingAmount,
    monthlyPayment = monthlyPayment,
    interestRate = interestRate,
    loanType = LoanType.valueOf(loanType),
    startDate = startDate,
    endDate = endDate,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Loan.toEntity() = LoanEntity(
    id = id,
    name = name,
    lenderName = lenderName,
    originalAmount = originalAmount,
    remainingAmount = remainingAmount,
    monthlyPayment = monthlyPayment,
    interestRate = interestRate,
    loanType = loanType.name,
    startDate = startDate,
    endDate = endDate,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CreditCardEntity.toDomain() = CreditCard(
    id = id,
    name = name,
    cardType = CardType.valueOf(cardType),
    lastFourDigits = lastFourDigits,
    creditLimit = creditLimit,
    currentBalance = currentBalance,
    availableCredit = availableCredit,
    minimumPayment = minimumPayment,
    dueDate = dueDate,
    interestRate = interestRate,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun CreditCard.toEntity() = CreditCardEntity(
    id = id,
    name = name,
    cardType = cardType.name,
    lastFourDigits = lastFourDigits,
    creditLimit = creditLimit,
    currentBalance = currentBalance,
    availableCredit = availableCredit,
    minimumPayment = minimumPayment,
    dueDate = dueDate,
    interestRate = interestRate,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)