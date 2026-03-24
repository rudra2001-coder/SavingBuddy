package com.example.savingbuddy.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.model.Account
import com.example.savingbuddy.domain.model.MonthlySummary
import com.example.savingbuddy.domain.model.SavingsGoal
import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.repository.AccountRepository
import com.example.savingbuddy.domain.repository.SavingsGoalRepository
import com.example.savingbuddy.domain.usecase.GetMonthlySummaryUseCase
import com.example.savingbuddy.domain.usecase.GetRecentTransactionsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val totalBalance: Double = 0.0,
    val monthlySummary: MonthlySummary = MonthlySummary(0.0, 0.0, 0.0, 0f, 0.0, emptyMap()),
    val recentTransactions: List<Transaction> = emptyList(),
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val totalSavings: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getMonthlySummaryUseCase: GetMonthlySummaryUseCase,
    private val getRecentTransactionsUseCase: GetRecentTransactionsUseCase,
    private val accountRepository: AccountRepository,
    private val savingsGoalRepository: SavingsGoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            combine(
                accountRepository.getTotalBalance(),
                getMonthlySummaryUseCase(),
                getRecentTransactionsUseCase(5),
                savingsGoalRepository.getAllSavingsGoals(),
                savingsGoalRepository.getTotalSavings()
            ) { balance, summary, transactions, goals, totalSavings ->
                DashboardUiState(
                    totalBalance = balance,
                    monthlySummary = summary,
                    recentTransactions = transactions,
                    savingsGoals = goals,
                    totalSavings = totalSavings,
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun initializeDefaultData() {
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { accounts ->
                if (accounts.isEmpty()) {
                    accountRepository.addAccount(
                        Account(
                            id = "",
                            name = "Cash",
                            balance = 0.0,
                            icon = "wallet",
                            color = 0xFF4CAF50,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis(),
                            isSynced = false
                        )
                    )
                }
            }
        }
    }
}