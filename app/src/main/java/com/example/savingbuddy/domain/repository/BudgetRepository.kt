package com.example.savingbuddy.domain.repository

import com.example.savingbuddy.domain.model.Budget
import kotlinx.coroutines.flow.Flow

interface BudgetRepository {
    fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>>
    suspend fun getBudgetForCategory(categoryId: String, month: Int, year: Int): Budget?
    suspend fun getBudgetById(id: String): Budget?
    suspend fun addBudget(budget: Budget)
    suspend fun updateBudget(budget: Budget)
    suspend fun deleteBudget(budget: Budget)
}
