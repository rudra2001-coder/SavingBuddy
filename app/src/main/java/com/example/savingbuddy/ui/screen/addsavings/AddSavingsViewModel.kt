package com.example.savingbuddy.ui.screen.addsavings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.model.Account
import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.model.TransactionType
import com.example.savingbuddy.domain.repository.AccountRepository
import com.example.savingbuddy.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject


data class AddSavingsUiState(
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val accounts: List<Account> = emptyList(),
    val selectedAccount: Account? = null,
    val amount: String = "",
    val note: String = "",
    val errorMessage: String? = null
)

@HiltViewModel
class AddSavingsViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddSavingsUiState())
    val uiState: StateFlow<AddSavingsUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            val accounts = accountRepository.getAllAccounts().first()
            _uiState.value = _uiState.value.copy(
                accounts = accounts,
                selectedAccount = accounts.firstOrNull()
            )
        }
    }

    fun selectAccount(account: Account) {
        _uiState.value = _uiState.value.copy(selectedAccount = account)
    }

    fun updateAmount(amount: String) {
        if (amount.isEmpty() || amount.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
            _uiState.value = _uiState.value.copy(amount = amount)
        }
    }

    fun updateNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun saveSavings() {
        viewModelScope.launch {
            val state = _uiState.value
            val amount = state.amount.toDoubleOrNull()

            if (amount == null || amount <= 0) {
                _uiState.value = state.copy(errorMessage = "Please enter a valid amount")
                return@launch
            }

            if (state.selectedAccount == null) {
                _uiState.value = state.copy(errorMessage = "Please select an account")
                return@launch
            }

            if (state.selectedAccount.balance < amount) {
                _uiState.value = state.copy(errorMessage = "Insufficient balance")
                return@launch
            }

            _uiState.value = state.copy(isLoading = true)

            try {
                val now = System.currentTimeMillis()
                val transaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    amount = amount,
                    type = TransactionType.TRANSFER,
                    categoryId = "savings_deposit",
                    accountId = state.selectedAccount.id,
                    note = state.note.ifBlank { "Savings Deposit" },
                    timestamp = now,
                    createdAt = now,
                    updatedAt = now,
                    isSynced = false
                )
                transactionRepository.addTransaction(transaction)

                val updatedAccount = state.selectedAccount.copy(
                    balance = state.selectedAccount.balance - amount,
                    updatedAt = now
                )
                accountRepository.updateAccount(updatedAccount)

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaved = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Failed to save: ${e.message}"
                )
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
