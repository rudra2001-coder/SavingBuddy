package com.example.savingbuddy.ui.screen.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.model.Account
import com.example.savingbuddy.domain.model.MonthlySummary
import com.example.savingbuddy.domain.model.SavingsGoal
import com.example.savingbuddy.domain.model.SpendingInsight
import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.repository.AccountRepository
import com.example.savingbuddy.domain.repository.BudgetRepository
import com.example.savingbuddy.domain.repository.CategoryRepository
import com.example.savingbuddy.domain.repository.SavingsGoalRepository
import com.example.savingbuddy.domain.repository.TransactionRepository
import com.example.savingbuddy.domain.repository.UserPreferencesRepository
import com.example.savingbuddy.domain.usecase.CalculateFinancialHealthScore
import com.example.savingbuddy.domain.usecase.FinancialHealthScore
import com.example.savingbuddy.domain.usecase.GetMonthlySummaryUseCase
import com.example.savingbuddy.domain.usecase.GetRecentTransactionsUseCase
import com.example.savingbuddy.domain.usecase.GetSpendingInsightsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

data class DashboardUiState(
    val totalBalance: Double = 0.0,
    val monthlySummary: MonthlySummary = MonthlySummary(0.0, 0.0, 0.0, 0f, 0.0, emptyMap()),
    val recentTransactions: List<Transaction> = emptyList(),
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val totalSavings: Double = 0.0,
    val healthScore: FinancialHealthScore? = null,
    val spendingInsights: List<SpendingInsight> = emptyList(),
    val budgetStatus: BudgetStatus = BudgetStatus(),
    val isLoading: Boolean = true
)

data class BudgetStatus(
    val totalBudget: Double = 0.0,
    val spent: Double = 0.0,
    val remaining: Double = 0.0,
    val percentageUsed: Float = 0f
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getMonthlySummaryUseCase: GetMonthlySummaryUseCase,
    private val getRecentTransactionsUseCase: GetRecentTransactionsUseCase,
    private val accountRepository: AccountRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val budgetRepository: BudgetRepository,
    private val calculateHealthScore: CalculateFinancialHealthScore,
    private val getSpendingInsightsUseCase: GetSpendingInsightsUseCase,
    private val preferencesRepository: UserPreferencesRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        loadDashboardData()
    }

    private fun loadDashboardData() {
        viewModelScope.launch {
            // Load health score
            val prefs = preferencesRepository.getPreferences().first()
            val monthlyIncome = prefs?.monthlyIncome ?: 50000.0
            val healthScore = calculateHealthScore.calculate(monthlyIncome)
            
            // Get budget status
            val now = Calendar.getInstance()
            val budgets = budgetRepository.getBudgetsForMonth(now.get(Calendar.MONTH), now.get(Calendar.YEAR)).first()
            val totalBudget = budgets.sumOf { it.monthlyLimit }
            val spent = _uiState.value.monthlySummary.totalExpense
            val remaining = (totalBudget - spent).coerceAtLeast(0.0)
            val percentageUsed = if (totalBudget > 0) (spent / totalBudget * 100).toFloat() else 0f
            
            val budgetStatus = BudgetStatus(
                totalBudget = totalBudget,
                spent = spent,
                remaining = remaining,
                percentageUsed = percentageUsed
            )

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
                    healthScore = healthScore,
                    budgetStatus = BudgetStatus(
                        totalBudget = totalBudget,
                        spent = summary.totalExpense,
                        remaining = (totalBudget - summary.totalExpense).coerceAtLeast(0.0),
                        percentageUsed = if (totalBudget > 0) (summary.totalExpense / totalBudget * 100).toFloat() else 0f
                    ),
                    isLoading = false
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
        
        // Load spending insights
        viewModelScope.launch {
            val now = Calendar.getInstance()
            val monthStart = getMonthStart(now)
            val monthEnd = getMonthEnd(now)
            
            val currentTransactions = transactionRepository.getTransactionsByDateRange(monthStart, monthEnd).first()
            val previousMonthStart = getPreviousMonthStart(now)
            val previousMonthEnd = getPreviousMonthEnd(now)
            val previousTransactions = transactionRepository.getTransactionsByDateRange(previousMonthStart, previousMonthEnd).first()
            
            val categoryNames = categoryRepository.getAllCategories().first().associate { it.id to it.name }
            
            val insights = getSpendingInsightsUseCase.analyzeSpending(
                currentPeriodTransactions = currentTransactions,
                previousPeriodTransactions = previousTransactions,
                categoryNames = categoryNames
            )
            _uiState.value = _uiState.value.copy(spendingInsights = insights.take(3))
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
    
    private fun getMonthStart(calendar: Calendar): Long {
        return (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    private fun getMonthEnd(calendar: Calendar): Long {
        return (calendar.clone() as Calendar).apply {
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
    
    private fun getPreviousMonthStart(calendar: Calendar): Long {
        return (calendar.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    
    private fun getPreviousMonthEnd(calendar: Calendar): Long {
        return (calendar.clone() as Calendar).apply {
            add(Calendar.MONTH, -1)
            set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }.timeInMillis
    }
}