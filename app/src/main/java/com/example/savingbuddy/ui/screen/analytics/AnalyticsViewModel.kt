package com.example.savingbuddy.ui.screen.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.model.*
import com.example.savingbuddy.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class AnalyticsUiState(
    val isLoading: Boolean = true,
    
    // Overview stats
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val savingsRate: Float = 0f,
    
    // Transaction stats
    val transactionCount: Int = 0,
    val topCategories: List<CategorySpending> = emptyList(),
    val dailySpending: List<DailySpending> = emptyList(),
    val weeklyTrend: List<WeeklyData> = emptyList(),
    
    // Account stats
    val accounts: List<Account> = emptyList(),
    
    // Savings stats
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val totalSaved: Double = 0.0,
    
    // Budget stats
    val budgetOverview: List<BudgetStatus> = emptyList(),
    val overBudgetCount: Int = 0,
    
    // Life stats
    val activeHabits: Int = 0,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val focusMinutes: Int = 0,
    val journalEntries: Int = 0,
    
    // Loan & Credit Card stats
    val totalDebt: Double = 0.0,
    val creditCardBalance: Double = 0.0,
    
    // Net Worth
    val netWorth: Double = 0.0
)

data class CategorySpending(
    val categoryId: String,
    val categoryName: String,
    val icon: String,
    val color: Long,
    val amount: Double,
    val percentage: Float
)

data class DailySpending(
    val dayOfWeek: String,
    val amount: Double
)

data class WeeklyData(
    val weekLabel: String,
    val income: Double,
    val expense: Double
)

data class BudgetStatus(
    val categoryName: String,
    val icon: String,
    val limit: Double,
    val spent: Double,
    val percentage: Float,
    val isOver: Boolean
)

@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val budgetRepository: BudgetRepository,
    private val loanRepository: LoanRepository,
    private val creditCardRepository: CreditCardRepository,
    private val habitRepository: HabitRepository,
    private val taskRepository: TaskRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val journalRepository: JournalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AnalyticsUiState())
    val uiState: StateFlow<AnalyticsUiState> = _uiState.asStateFlow()

    init {
        loadAnalytics()
    }

    private fun loadAnalytics() {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)
            
            // Start of month
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            val startOfMonth = calendar.timeInMillis
            
            // End of month
            calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            val endOfMonth = calendar.timeInMillis
            
            // Combine all data sources
            combine(
                accountRepository.getAllAccounts(),
                accountRepository.getTotalBalance(),
                transactionRepository.getAllTransactions(),
                categoryRepository.getAllCategories(),
                savingsGoalRepository.getAllSavingsGoals(),
                budgetRepository.getBudgetsForMonth(currentMonth, currentYear),
                loanRepository.getTotalDebt(),
                creditCardRepository.getTotalBalance(),
                habitRepository.getAllHabits(),
                focusSessionRepository.getTotalMinutesForDateRange(startOfMonth, endOfMonth),
                journalRepository.getAllEntries()
            ) { values ->
                @Suppress("UNCHECKED_CAST")
                val accounts = values[0] as List<Account>
                val totalBalance = values[1] as Double
                val transactions = values[2] as List<Transaction>
                val categories = values[3] as List<Category>
                val savingsGoals = values[4] as List<SavingsGoal>
                val budgets = values[5] as List<Budget>
                val totalDebt = values[6] as Double
                val creditCardBalance = values[7] as Double
                val habits = values[8] as List<Habit>
                val focusMinutes = values[9] as Int
                val journalEntries = values[10] as List<JournalEntry>

                // Calculate monthly income/expense
                val monthTransactions = transactions.filter { 
                    it.timestamp in startOfMonth..endOfMonth 
                }
                val monthlyIncome = monthTransactions
                    .filter { it.type == TransactionType.INCOME }
                    .sumOf { it.amount }
                val monthlyExpense = monthTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .sumOf { it.amount }
                
                val savingsRate = if (monthlyIncome > 0) {
                    ((monthlyIncome - monthlyExpense) / monthlyIncome * 100).toFloat()
                } else 0f

                // Top categories
                val categorySpending = monthTransactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .groupBy { it.categoryId }
                    .map { (catId, txs) ->
                        val category = categories.find { it.id == catId }
                        val amount = txs.sumOf { it.amount }
                        CategorySpending(
                            categoryId = catId,
                            categoryName = category?.name ?: "Unknown",
                            icon = category?.icon ?: "📁",
                            color = category?.color ?: 0xFF4CAF50,
                            amount = amount,
                            percentage = if (monthlyExpense > 0) (amount / monthlyExpense * 100).toFloat() else 0f
                        )
                    }
                    .sortedByDescending { it.amount }
                    .take(5)

                // Daily spending by day of week
                val dailySpending = (0..6).map { day ->
                    val dayTransactions = monthTransactions.filter { tx ->
                        Calendar.getInstance().apply { timeInMillis = tx.timestamp }
                            .get(Calendar.DAY_OF_WEEK) == day + 1
                    }
                    val dayName = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")[day]
                    DailySpending(dayName, dayTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount })
                }

                // Weekly trend (last 4 weeks)
                val weeklyTrend = (0..3).map { weeksAgo ->
                    val cal = Calendar.getInstance().apply { 
                        add(Calendar.WEEK_OF_YEAR, -weeksAgo) 
                    }
                    val weekStart = cal.apply {
                        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                        set(Calendar.HOUR_OF_DAY, 0)
                    }.timeInMillis
                    val weekEnd = cal.apply {
                        set(Calendar.DAY_OF_WEEK, firstDayOfWeek)
                        add(Calendar.DAY_OF_WEEK, 6)
                        set(Calendar.HOUR_OF_DAY, 23)
                    }.timeInMillis
                    
                    val weekTxs = transactions.filter { it.timestamp in weekStart..weekEnd }
                    WeeklyData(
                        weekLabel = "${weeksAgo + 1}w ago",
                        income = weekTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount },
                        expense = weekTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                    )
                }.reversed()

                // Budget status
                val budgetStatus = budgets.map { budget ->
                    val spent = monthTransactions
                        .filter { it.type == TransactionType.EXPENSE && it.categoryId == budget.categoryId }
                        .sumOf { it.amount }
                    val category = categories.find { it.id == budget.categoryId }
                    BudgetStatus(
                        categoryName = category?.name ?: "Unknown",
                        icon = category?.icon ?: "📁",
                        limit = budget.monthlyLimit,
                        spent = spent,
                        percentage = (spent / budget.monthlyLimit * 100).toFloat().coerceAtMost(100f),
                        isOver = spent > budget.monthlyLimit
                    )
                }

                // Tasks
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                }.timeInMillis
                val tasks = taskRepository.getTodayTasks(today).first()
                val totalTasks = tasks.size
                val completedTasks = tasks.count { it.completed }

                AnalyticsUiState(
                    isLoading = false,
                    totalBalance = totalBalance,
                    totalIncome = monthlyIncome,
                    totalExpense = monthlyExpense,
                    savingsRate = savingsRate,
                    transactionCount = transactions.size,
                    topCategories = categorySpending,
                    dailySpending = dailySpending,
                    weeklyTrend = weeklyTrend,
                    accounts = accounts,
                    savingsGoals = savingsGoals,
                    totalSaved = savingsGoals.sumOf { it.currentAmount },
                    budgetOverview = budgetStatus,
                    overBudgetCount = budgetStatus.count { it.isOver },
                    activeHabits = habits.count { it.isActive },
                    totalTasks = totalTasks,
                    completedTasks = completedTasks,
                    focusMinutes = focusMinutes,
                    journalEntries = journalEntries.size,
                    totalDebt = totalDebt,
                    creditCardBalance = creditCardBalance,
                    netWorth = totalBalance + savingsGoals.sumOf { it.currentAmount } - totalDebt - creditCardBalance
                )
            }.collect { state ->
                _uiState.value = state
            }
        }
    }

    fun refresh() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        loadAnalytics()
    }
}