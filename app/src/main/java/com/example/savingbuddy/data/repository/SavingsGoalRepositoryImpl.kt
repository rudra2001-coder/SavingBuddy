package com.example.savingbuddy.data.repository

import com.example.savingbuddy.data.local.dao.SavingsGoalDao
import com.example.savingbuddy.data.local.mapper.toDomain
import com.example.savingbuddy.data.local.mapper.toEntity
import com.example.savingbuddy.domain.model.SavingsGoal
import com.example.savingbuddy.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SavingsGoalRepositoryImpl @Inject constructor(
    private val savingsGoalDao: SavingsGoalDao
) : SavingsGoalRepository {

    override fun getAllSavingsGoals(): Flow<List<SavingsGoal>> =
        savingsGoalDao.getAllSavingsGoals().map { list -> list.map { it.toDomain() } }

    override suspend fun getSavingsGoalById(id: String): SavingsGoal? =
        savingsGoalDao.getSavingsGoalById(id)?.toDomain()

    override fun getTotalSavings(): Flow<Double> =
        savingsGoalDao.getTotalSavings().map { it ?: 0.0 }

    override fun getTotalTargetAmount(): Flow<Double> =
        savingsGoalDao.getTotalTargetAmount().map { it ?: 0.0 }

    override suspend fun addSavingsGoal(goal: SavingsGoal) {
        savingsGoalDao.insertSavingsGoal(goal.toEntity())
    }

    override suspend fun updateSavingsGoal(goal: SavingsGoal) {
        savingsGoalDao.updateSavingsGoal(goal.toEntity())
    }

    override suspend fun deleteSavingsGoal(goal: SavingsGoal) {
        savingsGoalDao.deleteSavingsGoal(goal.toEntity())
    }

    override suspend fun addToGoal(goalId: String, amount: Double) {
        savingsGoalDao.addToGoal(goalId, amount)
    }
}