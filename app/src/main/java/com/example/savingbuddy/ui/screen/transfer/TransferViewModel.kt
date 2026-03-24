package com.example.savingbuddy.ui.screen.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.model.Account
import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.model.TransactionType
import com.example.savingbuddy.domain.repository.AccountRepository
import com.example.savingbuddy.domain.usecase.AddTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TransferUiState(
    val fromAccount: Account? = null,
    val toAccount: Account? = null,
    val amount: String = "",
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val addTransactionUseCase: AddTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    init {
        loadAccounts()
    }

    private fun loadAccounts() {
        viewModelScope.launch {
            val accounts = accountRepository.getAllAccounts().first()
            _uiState.value = _uiState.value.copy(
                accounts = accounts,
                fromAccount = accounts.getOrNull(0),
                toAccount = accounts.getOrNull(1),
                isLoading = false
            )
        }
    }

    fun updateAmount(amount: String) {
        _uiState.value = _uiState.value.copy(amount = amount)
    }

    fun selectFromAccount(account: Account) {
        _uiState.value = _uiState.value.copy(fromAccount = account)
    }

    fun selectToAccount(account: Account) {
        _uiState.value = _uiState.value.copy(toAccount = account)
    }

    fun transfer() {
        val amount = _uiState.value.amount.toDoubleOrNull()
        val from = _uiState.value.fromAccount
        val to = _uiState.value.toAccount

        if (amount == null || amount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid amount")
            return
        }

        if (from == null || to == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select both accounts")
            return
        }

        if (from.id == to.id) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select different accounts")
            return
        }

        if (from.balance < amount) {
            _uiState.value = _uiState.value.copy(errorMessage = "Insufficient balance")
            return
        }

        viewModelScope.launch {
            try {
                val transaction = Transaction(
                    id = UUID.randomUUID().toString(),
                    amount = amount,
                    type = TransactionType.TRANSFER,
                    categoryId = "",
                    accountId = from.id,
                    note = "Transfer to ${to.name}",
                    timestamp = System.currentTimeMillis(),
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),
                    isSynced = false
                )

                addTransactionUseCase(transaction, from.id, to.id)
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