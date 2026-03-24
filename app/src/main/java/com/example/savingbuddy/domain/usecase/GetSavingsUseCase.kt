package com.example.savingbuddy.domain.usecase

import com.example.savingbuddy.domain.model.SavingsGoal
import com.example.savingbuddy.domain.repository.SavingsGoalRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetSavingsUseCase @Inject constructor(
    private val savingsGoalRepository: SavingsGoalRepository
) {
    operator fun invoke(): Flow<List<SavingsGoal>> =
        savingsGoalRepository.getAllSavingsGoals()

    fun getTotalSavings(): Flow<Double> = savingsGoalRepository.getTotalSavings()
}