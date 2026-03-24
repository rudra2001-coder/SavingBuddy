package com.example.savingbuddy.ui.screen.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.model.Account
import com.example.savingbuddy.domain.model.Category
import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.model.TransactionType
import com.example.savingbuddy.domain.repository.AccountRepository
import com.example.savingbuddy.domain.repository.CategoryRepository
import com.example.savingbuddy.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class AddTransactionUiState(
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
    val errorMessage: String? = null
)

@HiltViewModel
class AddTransactionViewModel @Inject constructor(
    private val addTransactionUseCase: AddTransactionUseCase,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            categoryRepository.addDefaultCategories()

            val accounts = accountRepository.getAllAccounts().first()
            val expenseCategories = categoryRepository.getCategoriesByType(TransactionType.EXPENSE).first()
            val incomeCategories = categoryRepository.getCategoriesByType(TransactionType.INCOME).first()

            _uiState.value = _uiState.value.copy(
                accounts = accounts,
                expenseCategories = expenseCategories,
                incomeCategories = incomeCategories,
                selectedAccount = accounts.firstOrNull(),
                selectedCategory = expenseCategories.firstOrNull(),
                isLoading = false
            )
        }
    }

    fun updateAmount(amount: String) {
        _uiState.value = _uiState.value.copy(amount = amount)
    }

    fun updateType(type: TransactionType) {
        val categories = if (type == TransactionType.EXPENSE) _uiState.value.expenseCategories else _uiState.value.incomeCategories
        _uiState.value = _uiState.value.copy(
            type = type,
            selectedCategory = categories.firstOrNull()
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
                    type = _uiState.value.type,
                    categoryId = category?.id ?: "",
                    accountId = account.id,
                    note = _uiState.value.note.ifBlank { null },
                    timestamp = System.currentTimeMillis(),
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
}