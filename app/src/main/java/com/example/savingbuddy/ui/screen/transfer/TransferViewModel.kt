package com.example.savingbuddy.ui.screen.transfer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.model.Account
import com.example.savingbuddy.domain.model.SavingsGoal
import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.model.TransactionType
import com.example.savingbuddy.domain.repository.AccountRepository
import com.example.savingbuddy.domain.repository.SavingsGoalRepository
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
    val transferType: TransferType = TransferType.ACCOUNT_TO_ACCOUNT,
    val fromAccount: Account? = null,
    val toAccount: Account? = null,
    val selectedSavingsGoal: SavingsGoal? = null,
    val toSavingsGoal: SavingsGoal? = null,
    val amount: String = "",
    val note: String = "",
    val accounts: List<Account> = emptyList(),
    val savingsGoals: List<SavingsGoal> = emptyList(),
    val isLoading: Boolean = true,
    val isSaved: Boolean = false,
    val showConfirmation: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class TransferViewModel @Inject constructor(
    private val accountRepository: AccountRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val addTransactionUseCase: AddTransactionUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransferUiState())
    val uiState: StateFlow<TransferUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            val accounts = accountRepository.getAllAccounts().first()
            val savingsGoals = savingsGoalRepository.getAllSavingsGoals().first()
            _uiState.value = _uiState.value.copy(
                accounts = accounts,
                savingsGoals = savingsGoals,
                fromAccount = accounts.getOrNull(0),
                toAccount = accounts.getOrNull(1),
                isLoading = false
            )
        }
    }

    fun setTransferType(type: TransferType) {
        _uiState.value = _uiState.value.copy(
            transferType = type,
            errorMessage = null
        )
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

    fun selectSavingsGoal(goal: SavingsGoal) {
        _uiState.value = _uiState.value.copy(selectedSavingsGoal = goal)
    }

    fun selectToSavingsGoal(goal: SavingsGoal) {
        _uiState.value = _uiState.value.copy(toSavingsGoal = goal)
    }

    fun setNote(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun showConfirmation() {
        _uiState.value = _uiState.value.copy(showConfirmation = true)
    }

    fun hideConfirmation() {
        _uiState.value = _uiState.value.copy(showConfirmation = false)
    }

    fun transfer() {
        val amount = _uiState.value.amount.toDoubleOrNull()
        if (amount == null || amount <= 0) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please enter a valid amount", showConfirmation = false)
            return
        }

        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(showConfirmation = false)
                
                when (_uiState.value.transferType) {
                    TransferType.ACCOUNT_TO_ACCOUNT -> handleAccountToAccount(amount)
                    TransferType.INCOME_TO_SAVINGS -> handleIncomeToSavings(amount)
                    TransferType.SAVINGS_TO_INCOME -> handleSavingsToIncome(amount)
                    TransferType.ACCOUNT_TO_SAVINGS -> handleAccountToSavings(amount)
                    TransferType.SAVINGS_TO_SAVINGS -> handleSavingsToSavings(amount)
                    TransferType.INCOME_TO_INCOME -> handleIncomeToIncome(amount)
                }
                _uiState.value = _uiState.value.copy(isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message ?: "Transfer failed")
            }
        }
    }

    private suspend fun handleAccountToAccount(amount: Double) {
        val from = _uiState.value.fromAccount
        val to = _uiState.value.toAccount

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

        val userNote = _uiState.value.note
        val fromNote = if (userNote.isNotBlank()) "$userNote (to ${to.name})" else "Transfer to ${to.name}"
        val toNote = if (userNote.isNotBlank()) "$userNote (from ${from.name})" else "Transfer from ${from.name}"

        // Create expense transaction from source account
        val expenseTx = Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            type = TransactionType.EXPENSE,
            categoryId = "transfer",
            accountId = from.id,
            note = fromNote,
            timestamp = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        
        // Create income transaction to destination account
        val incomeTx = Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            type = TransactionType.INCOME,
            categoryId = "transfer",
            accountId = to.id,
            note = toNote,
            timestamp = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )

        addTransactionUseCase(expenseTx, from.id, null)
        addTransactionUseCase(incomeTx, to.id, null)
    }

    private suspend fun handleIncomeToSavings(amount: Double) {
        val from = _uiState.value.fromAccount
        val goal = _uiState.value.selectedSavingsGoal

        if (from == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select an account")
            return
        }

        if (goal == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select a savings goal")
            return
        }

        if (from.balance < amount) {
            _uiState.value = _uiState.value.copy(errorMessage = "Insufficient balance")
            return
        }

        // Create expense from account (savings contribution)
        val expenseTx = Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            type = TransactionType.EXPENSE,
            categoryId = "savings",
            accountId = from.id,
            note = "Savings: ${goal.name}",
            timestamp = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )

        addTransactionUseCase(expenseTx, from.id, null)
        
        // Add to savings goal
        savingsGoalRepository.addToGoal(goal.id, amount)
    }

    private suspend fun handleSavingsToIncome(amount: Double) {
        val goal = _uiState.value.selectedSavingsGoal
        val to = _uiState.value.toAccount

        if (goal == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select a savings goal")
            return
        }

        if (to == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select an account")
            return
        }

        if (goal.currentAmount < amount) {
            _uiState.value = _uiState.value.copy(errorMessage = "Insufficient savings")
            return
        }

        // Create income to account (withdrawal from savings)
        val incomeTx = Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            type = TransactionType.INCOME,
            categoryId = "savings_withdrawal",
            accountId = to.id,
            note = "Withdrawal from: ${goal.name}",
            timestamp = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )

        addTransactionUseCase(incomeTx, to.id, null)
        
        // Subtract from savings goal (add negative amount)
        savingsGoalRepository.addToGoal(goal.id, -amount)
    }

    private suspend fun handleAccountToSavings(amount: Double) {
        val from = _uiState.value.fromAccount
        val goal = _uiState.value.selectedSavingsGoal

        if (from == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select an account")
            return
        }

        if (goal == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select a savings goal")
            return
        }

        if (from.balance < amount) {
            _uiState.value = _uiState.value.copy(errorMessage = "Insufficient balance")
            return
        }

        // Create expense from account
        val expenseTx = Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            type = TransactionType.EXPENSE,
            categoryId = "savings",
            accountId = from.id,
            note = "Savings: ${goal.name}",
            timestamp = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )

        addTransactionUseCase(expenseTx, from.id, null)
        
        // Add to savings goal
        savingsGoalRepository.addToGoal(goal.id, amount)
    }

    private suspend fun handleSavingsToSavings(amount: Double) {
        val fromGoal = _uiState.value.selectedSavingsGoal
        val toGoal = _uiState.value.toSavingsGoal

        if (fromGoal == null || toGoal == null) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select both savings goals")
            return
        }

        if (fromGoal.id == toGoal.id) {
            _uiState.value = _uiState.value.copy(errorMessage = "Please select different savings goals")
            return
        }

        if (fromGoal.currentAmount < amount) {
            _uiState.value = _uiState.value.copy(errorMessage = "Insufficient savings in source goal")
            return
        }

        // Subtract from source goal
        savingsGoalRepository.addToGoal(fromGoal.id, -amount)
        // Add to destination goal
        savingsGoalRepository.addToGoal(toGoal.id, amount)
    }

    private suspend fun handleIncomeToIncome(amount: Double) {
        val from = _uiState.value.fromAccount
        val to = _uiState.value.toAccount

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

        // Create expense transaction from source (moving money out)
        val expenseTx = Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            type = TransactionType.EXPENSE,
            categoryId = "transfer",
            accountId = from.id,
            note = "Move to ${to.name}",
            timestamp = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )
        
        // Create income transaction to destination
        val incomeTx = Transaction(
            id = UUID.randomUUID().toString(),
            amount = amount,
            type = TransactionType.INCOME,
            categoryId = "transfer",
            accountId = to.id,
            note = "Move from ${from.name}",
            timestamp = System.currentTimeMillis(),
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            isSynced = false
        )

        addTransactionUseCase(expenseTx, from.id, null)
        addTransactionUseCase(incomeTx, to.id, null)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}