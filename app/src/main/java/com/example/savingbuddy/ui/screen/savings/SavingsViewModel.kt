package com.example.savingbuddy.ui.screen.savings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.model.SavingsGoal
import com.example.savingbuddy.domain.repository.SavingsGoalRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class SavingsUiState(
    val goals: List<SavingsGoal> = emptyList(),
    val totalSavings: Double = 0.0,
    val totalTarget: Double = 0.0,
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val newGoalName: String = "",
    val newGoalTarget: String = ""
)

@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val savingsGoalRepository: SavingsGoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SavingsUiState())
    val uiState: StateFlow<SavingsUiState> = _uiState.asStateFlow()

    init {
        loadSavings()
    }

    private fun loadSavings() {
        viewModelScope.launch {
            savingsGoalRepository.getAllSavingsGoals().collect { goals ->
                _uiState.value = _uiState.value.copy(
                    goals = goals,
                    isLoading = false
                )
            }
        }

        viewModelScope.launch {
            savingsGoalRepository.getTotalSavings().collect { total ->
                _uiState.value = _uiState.value.copy(totalSavings = total)
            }
        }

        viewModelScope.launch {
            savingsGoalRepository.getTotalTargetAmount().collect { total ->
                _uiState.value = _uiState.value.copy(totalTarget = total)
            }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = false,
            newGoalName = "",
            newGoalTarget = ""
        )
    }

    fun updateNewGoalName(name: String) {
        _uiState.value = _uiState.value.copy(newGoalName = name)
    }

    fun updateNewGoalTarget(target: String) {
        _uiState.value = _uiState.value.copy(newGoalTarget = target)
    }

    fun addGoal() {
        val name = _uiState.value.newGoalName
        val target = _uiState.value.newGoalTarget.toDoubleOrNull()

        if (name.isBlank() || target == null || target <= 0) return

        viewModelScope.launch {
            val goal = SavingsGoal(
                id = UUID.randomUUID().toString(),
                name = name,
                targetAmount = target,
                currentAmount = 0.0,
                icon = "savings",
                color = 0xFF2196F3,
                deadline = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            savingsGoalRepository.addSavingsGoal(goal)
            hideAddDialog()
        }
    }

    fun deleteGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            savingsGoalRepository.deleteSavingsGoal(goal)
        }
    }

    fun addToGoal(goalId: String, amount: Double) {
        viewModelScope.launch {
            savingsGoalRepository.addToGoal(goalId, amount)
        }
    }
}