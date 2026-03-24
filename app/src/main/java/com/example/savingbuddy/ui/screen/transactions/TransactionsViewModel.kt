package com.example.savingbuddy.ui.screen.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.model.Category
import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.model.TransactionType
import com.example.savingbuddy.domain.repository.CategoryRepository
import com.example.savingbuddy.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TransactionsUiState(
    val transactions: List<Transaction> = emptyList(),
    val filteredTransactions: List<Transaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class TransactionsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsUiState())
    val uiState: StateFlow<TransactionsUiState> = _uiState.asStateFlow()

    private var searchQuery = ""
    private var selectedType: TransactionType? = null
    private var selectedCategoryId: String? = null

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val categories = categoryRepository.getAllCategories().first()
            _uiState.value = _uiState.value.copy(categories = categories)
            
            transactionRepository.getAllTransactions().collect { transactions ->
                _uiState.value = _uiState.value.copy(
                    transactions = transactions,
                    filteredTransactions = transactions,
                    isLoading = false
                )
            }
        }
    }

    fun filterTransactions(query: String, type: TransactionType?, categoryId: String?) {
        searchQuery = query
        selectedType = type
        selectedCategoryId = categoryId
        applyFilters()
    }

    fun clearFilters() {
        searchQuery = ""
        selectedType = null
        selectedCategoryId = null
        _uiState.value = _uiState.value.copy(filteredTransactions = _uiState.value.transactions)
    }

    private fun applyFilters() {
        val allTransactions = _uiState.value.transactions
        val filtered = allTransactions.filter { transaction ->
            val matchesQuery = searchQuery.isBlank() || 
                transaction.note?.contains(searchQuery, ignoreCase = true) == true ||
                transaction.type.name.contains(searchQuery, ignoreCase = true)
            
            val matchesType = selectedType == null || transaction.type == selectedType
            
            val matchesCategory = selectedCategoryId == null || transaction.categoryId == selectedCategoryId
            
            matchesQuery && matchesType && matchesCategory
        }
        
        _uiState.value = _uiState.value.copy(filteredTransactions = filtered)
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            transactionRepository.deleteTransaction(transaction)
        }
    }
}