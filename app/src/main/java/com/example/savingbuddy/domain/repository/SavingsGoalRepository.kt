package com.example.savingbuddy.domain.repository

import com.example.savingbuddy.domain.model.SavingsGoal
import kotlinx.coroutines.flow.Flow

interface SavingsGoalRepository {
    fun getAllSavingsGoals(): Flow<List<SavingsGoal>>
    suspend fun getSavingsGoalById(id: String): SavingsGoal?
    fun getTotalSavings(): Flow<Double>
    fun getTotalTargetAmount(): Flow<Double>
    suspend fun addSavingsGoal(goal: SavingsGoal)
    suspend fun updateSavingsGoal(goal: SavingsGoal)
    suspend fun deleteSavingsGoal(goal: SavingsGoal)
    suspend fun addToGoal(goalId: String, amount: Double)
}