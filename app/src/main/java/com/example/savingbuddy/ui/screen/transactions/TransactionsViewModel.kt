package com.example.savingbuddy.ui.screen.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    init {
        loadTransactions()
    }

    private fun loadTransactions() {
        viewModelScope.launch {
            transactionRepository.getAllTransactions().collect { transactions ->
                _uiState.value = TransactionsUiState(
                    transactions = transactions,
                    isLoading = false
                )
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }
}