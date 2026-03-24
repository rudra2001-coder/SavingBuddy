package com.example.savingbuddy.ui.screen.expense

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
import com.example.savingbuddy.domain.usecase.SpendingAdvice
import com.example.savingbuddy.domain.usecase.SpendingAdvisorUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

data class AddExpenseUiState(
    val amount: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val selectedCategory: Category? = null,
    val selectedAccount: Account? = null,
    val note: String = "",
    val accounts: List<Account> = emptyList(),
    val expenseCategories: List<Category> = emptyList(),
    val incomeCategories: List<Category> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val errorMessage: String? = null,
    val spendingAdvice: SpendingAdvice? = null,
    val totalIncome: Double = 0.0,
    val currentMonthSpending: Double = 0.0,
    val selectedDate: Long = System.currentTimeMillis(),
    val showDatePicker: Boolean = false,
    val quickAmounts: List<Long> = listOf(500, 1000, 2000, 5000, 10000, 20000, 50000),
    val isAnalyzing: Boolean = false
)

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository,
    private val transactionRepository: TransactionRepository,
    private val budgetRepository: BudgetRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val spendingAdvisorUseCase: SpendingAdvisorUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState.asStateFlow()

    private var analysisJob: kotlinx.coroutines.Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // This will automatically add preset categories if none exist
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
                selectedCategory = expenseCategories.firstOrNull(),
                isLoading = false,
                totalIncome = totalIncome,
                currentMonthSpending = currentMonthSpending
            )
        }
    }

    private suspend fun addPresetExpenseCategories() {
        // Categories are now handled by the repository
        // This method is kept for backward compatibility
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
        analyzeSpendingDebounced()
    }

    private fun analyzeSpendingDebounced() {
        analysisJob?.cancel()
        analysisJob = viewModelScope.launch {
            kotlinx.coroutines.delay(500)
            analyzeSpending()
        }
    }

    private suspend fun analyzeSpending() {
        val amount = _uiState.value.amount.toDoubleOrNull() ?: return
        val category = _uiState.value.selectedCategory ?: return

        if (amount <= 0) {
            _uiState.value = _uiState.value.copy(spendingAdvice = null, isAnalyzing = false)
            return
        }

        _uiState.value = _uiState.value.copy(isAnalyzing = true)

        try {
            val now = Calendar.getInstance()
            val monthStart = getMonthStart(now)
            val monthEnd = getMonthEnd(now)

            val currentMonthTransactions = transactionRepository.getTransactionsByDateRange(monthStart, monthEnd).first()
            val categoryBudget = budgetRepository.getBudgetForCategory(
                category.id,
                now.get(Calendar.MONTH),
                now.get(Calendar.YEAR)
            )
            val savingsGoals = savingsGoalRepository.getAllSavingsGoals().first()

            val advice = spendingAdvisorUseCase.analyzeSpending(
                amount = amount,
                categoryId = category.id,
                currentMonthTransactions = currentMonthTransactions,
                categoryBudget = categoryBudget,
                totalIncome = _uiState.value.totalIncome,
                savingsGoals = savingsGoals
            )

            _uiState.value = _uiState.value.copy(spendingAdvice = advice, isAnalyzing = false)
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(isAnalyzing = false)
        }
    }

    fun updateType(type: TransactionType) {
        val categories = if (type == TransactionType.EXPENSE) _uiState.value.expenseCategories else _uiState.value.incomeCategories
        _uiState.value = _uiState.value.copy(
            type = type,
            selectedCategory = categories.firstOrNull(),
            spendingAdvice = null
        )
    }

    fun selectCategory(category: Category) {
        _uiState.value = _uiState.value.copy(selectedCategory = category)
        analyzeSpendingDebounced()
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
                    type = TransactionType.EXPENSE,
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
            selectedCategory = _uiState.value.expenseCategories.firstOrNull(),
            note = "",
            isSaved = false,
            errorMessage = null,
            selectedDate = System.currentTimeMillis(),
            showDatePicker = false,
            spendingAdvice = null
        )
    }

    fun updateDate(date: Long) {
        _uiState.value = _uiState.value.copy(selectedDate = date, showDatePicker = false)
        analyzeSpendingDebounced()
    }

    fun showDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = true)
    }

    fun hideDatePicker() {
        _uiState.value = _uiState.value.copy(showDatePicker = false)
    }

    fun setQuickAmount(amount: Long) {
        _uiState.value = _uiState.value.copy(amount = amount.toString())
        analyzeSpendingDebounced()
    }

    fun navigateDate(days: Int) {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = _uiState.value.selectedDate
            add(Calendar.DAY_OF_MONTH, days)
        }
        _uiState.value = _uiState.value.copy(selectedDate = calendar.timeInMillis)
        analyzeSpendingDebounced()
    }

    fun setTodayDate() {
        _uiState.value = _uiState.value.copy(selectedDate = System.currentTimeMillis())
        analyzeSpendingDebounced()
    }
}