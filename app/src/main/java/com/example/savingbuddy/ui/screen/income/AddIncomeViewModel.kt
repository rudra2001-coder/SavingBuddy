package com.example.savingbuddy.ui.screen.income

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.model.Account
import com.example.savingbuddy.domain.model.Category
import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.model.TransactionType
import com.example.savingbuddy.domain.repository.AccountRepository
import com.example.savingbuddy.domain.repository.BudgetRepository
import com.example.savingbuddy.domain.repository.CategoryRepository
import com.example.savingbuddy.domain.repository.SavingsGoalRepository
import com.example.savingbuddy.domain.repository.TransactionRepository
import com.example.savingbuddy.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

private val GreenIncome = android.graphics.Color.parseColor("#4CAF50")

data class AddIncomeUiState(
    val amount: String = "",
    val type: TransactionType = TransactionType.INCOME,
    val selectedCategory: Category? = null,
    val selectedAccount: Account? = null,
    val note: String = "",
    val accounts: List<Account> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val incomeAdvice: IncomeAdvice? = null,
    val totalIncome: Double = 0.0,
    val currentMonthSpending: Double = 0.0,
    val selectedDate: Long = System.currentTimeMillis(),
    val showDatePicker: Boolean = false,
    val quickAmounts: List<Long> = listOf(500, 1000, 2000, 5000, 10000, 20000, 50000, 100000),
    val isAnalyzing: Boolean = false
)

@HiltViewModel
class AddIncomeViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val savingsGoalRepository: SavingsGoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddIncomeUiState())
    val uiState: StateFlow<AddIncomeUiState> = _uiState.asStateFlow()

    private var analysisJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            categoryRepository.addDefaultCategories()

            val accounts = accountRepository.getAllAccounts().first()
            val expenseCategories = categoryRepository.getCategoriesByType(TransactionType.EXPENSE).first()
            val incomeCategories = categoryRepository.getCategoriesByType(TransactionType.INCOME).first()

            val now = Calendar.getInstance()
            val monthStart = getMonthStart(now)
            val monthEnd = getMonthEnd(now)

            val currentMonthTransactions = transactionRepository.getTransactionsByDateRange(monthStart, monthEnd).first()
            val totalIncome = transactionRepository.getTotalIncome(monthStart, monthEnd).first()
            val currentMonthSpending = currentMonthTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            _uiState.value = _uiState.value.copy(
                accounts = accounts,
                expenseCategories = expenseCategories,
                incomeCategories = incomeCategories,
                selectedAccount = accounts.firstOrNull(),
                selectedCategory = incomeCategories.firstOrNull(),
                isLoading = false,
                totalIncome = totalIncome,
                currentMonthSpending = currentMonthSpending
            )
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

    fun updateAmount(amount: String) {
        _uiState.value = _uiState.value.copy(amount = amount)
        analyzeIncomeDebounced()
    }

    private fun analyzeIncomeDebounced() {
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            delay(500)
            analyzeIncome()
        }
    }

    private suspend fun analyzeIncome() {
        val amount = _uiState.value.amount.toDoubleOrNull() ?: return

        if (amount <= 0) {
            _uiState.value = _uiState.value.copy(incomeAdvice = null, isAnalyzing = false)
            return
        }

        _uiState.value = _uiState.value.copy(isAnalyzing = true)

        try {
            val advice = generateIncomeAdvice(amount)
            _uiState.value = _uiState.value.copy(incomeAdvice = advice, isAnalyzing = false)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isAnalyzing = false)
        }
    }

    private suspend fun generateIncomeAdvice(amount: Double): IncomeAdvice {
        val savingsGoals = savingsGoalRepository.getAllSavingsGoals().first()
        val monthlyIncome = _uiState.value.totalIncome + amount

        return when {
            savingsGoals.isNotEmpty() -> {
                val totalGoalAmount = savingsGoals.sumOf { it.targetAmount - it.currentAmount }
                if (amount >= totalGoalAmount * 0.1) {
                    IncomeAdvice(
                        title = "Great Progress!",
                        message = "This income brings you closer to your savings goals!",
                        savingsSuggestion = "Consider allocating ${formatCurrency(amount * 0.2)} to your savings goals."
                    )
                } else {
                    IncomeAdvice(
                        title = "Income Recorded",
                        message = "Keep tracking your income to reach your financial goals.",
                        savingsSuggestion = "Try to save at least 20% of every income for your goals."
                    )
                }
            }
            monthlyIncome > _uiState.value.currentMonthSpending * 1.5 -> {
                IncomeAdvice(
                    title = "Excellent!",
                    message = "Your income is significantly higher than your spending. Great financial health!",
                    savingsSuggestion = "Consider investing the surplus for long-term growth."
                )
            }
            monthlyIncome > _uiState.value.currentMonthSpending -> {
                IncomeAdvice(
                    title = "Good Job!",
                    message = "You're earning more than you spend. Keep it up!",
                    savingsSuggestion = "Build an emergency fund with 3-6 months of expenses."
                )
            }
            else -> {
                IncomeAdvice(
                    title = "Income Added",
                    message = "Track your expenses to ensure you're saving enough.",
                    savingsSuggestion = "Aim to save at least 20% of your income."
                )
            }
        }
    }

    private fun formatCurrency(amount: Double): String {
        return String.format("৳%,.2f", amount)
    }

    fun updateType(type: TransactionType) {
        val categories = if (type == TransactionType.EXPENSE) _uiState.value.expenseCategories else _uiState.value.incomeCategories
        _uiState.value = _uiState.value.copy(
            type = type,
            selectedCategory = categories.firstOrNull(),
            incomeAdvice = null
        )
    }

    fun selectCategory(category: Category) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
    }

    fun selectAccount(account: Account) {
        _uiState.value = _uiState.value.copy(selectedAccount = account)
    }

    fun updateNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun saveTransaction() {
        val amount = _uiState.value.amount.toDoubleOrNull()
        val account = _uiState.value.selectedAccount
        val category = _uiState.value.selectedCategory

        if (amount == null || amount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid amount")
            return
        }

        if (account == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select an account")
            return
        }

        viewModelScope.launch {
            try {
                val transaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    amount = amount,
                    type = TransactionType.INCOME,
                    categoryId = category?.id ?: "",
                    accountId = account.id,
                    note = _uiState.value.note.ifBlank { null },
                    timestamp = _uiState.value.selectedDate,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )

                addTransactionUseCase(transaction, account.id, null)
                _uiState.value = _uiState.value.copy(isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun resetForm() {
        _uiState.value = _uiState.value.copy(
            amount = "",
            selectedCategory = _uiState.value.incomeCategories.firstOrNull(),
            note = "",
            isSaved = false,
            errorMessage = null,
            selectedDate = System.currentTimeMillis(),
            showDatePicker = false,
            incomeAdvice = null
        )
    }

    fun updateDate(date: Long) {
        _uiState.value = _uiState.value.copy(selectedDate = date, showDatePicker = false)
        analyzeIncomeDebounced()
    }

    fun showDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = true)
    }

    fun hideDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = false)
    }

    fun setQuickAmount(amount: Long) {
        _uiState.value = _uiState.value.copy(amount = amount.toString())
        analyzeIncomeDebounced()
    }

    fun navigateDate(days: Int) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = _uiState.value.selectedDate
            add(Calendar.DAY_OF_MONTH, days)
        }
        _uiState.value = _uiState.value.copy(selectedDate = calendar.timeInMillis)
        analyzeIncomeDebounced()
    }

    fun setTodayDate() {
        _uiState.value = _uiState.value.copy(selectedDate = System.currentTimeMillis())
        analyzeIncomeDebounced()
    }
}
