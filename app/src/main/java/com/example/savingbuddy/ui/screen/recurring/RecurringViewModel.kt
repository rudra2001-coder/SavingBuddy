package com.example.savingbuddy.ui.screen.recurring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.model.*
import com.example.savingbuddy.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

data class RecurringUiState(
    val transactions: List<RecurringTransaction> = emptyList(),
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val isLoading: Boolean = true,
    val showAddDialog: Boolean = false,
    val editingTransaction: RecurringTransaction? = null,
    val newTitle: String = "",
    val newAmount: String = "",
    val newType: TransactionType = TransactionType.EXPENSE,
    val newRecurringType: RecurringType = RecurringType.WEEKLY,
    val newStartDate: Long = System.currentTimeMillis(),
    val hasEndDate: Boolean = false,
    val newEndDate: Long? = null,
    val selectedDays: Set<Int> = emptySet(),
    val selectedDayOfMonth: Int = 1,
    val newCategoryId: String = "",
    val newAccountId: String = "",
    val newNote: String = "",
    val excludeHolidays: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderMinutes: Int = 60
)

@HiltViewModel
class RecurringViewModel @Inject constructor(
    private val recurringRepository: RecurringTransactionRepository,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecurringUiState())
    val uiState: StateFlow<RecurringUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            recurringRepository.getActiveRecurringTransactions().collect { transactions ->
                _uiState.value = _uiState.value.copy(
                    transactions = transactions,
                    isLoading = false
                )
            }
        }
        viewModelScope.launch {
            categoryRepository.getCategoriesByType(TransactionType.EXPENSE).collect { categories ->
                _uiState.value = _uiState.value.copy(
                    categories = categories,
                    newCategoryId = categories.firstOrNull()?.id ?: ""
                )
            }
        }
        viewModelScope.launch {
            accountRepository.getAllAccounts().collect { accounts ->
                _uiState.value = _uiState.value.copy(
                    accounts = accounts,
                    newAccountId = accounts.firstOrNull()?.id ?: ""
                )
            }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingTransaction = null,
            newTitle = "",
            newAmount = "",
            newType = TransactionType.EXPENSE,
            newRecurringType = RecurringType.WEEKLY,
            newStartDate = System.currentTimeMillis(),
            hasEndDate = false,
            newEndDate = null,
            selectedDays = emptySet(),
            selectedDayOfMonth = 1,
            newNote = "",
            excludeHolidays = false,
            reminderEnabled = false
        )
    }

    fun editTransaction(transaction: RecurringTransaction) {
        _uiState.value = _uiState.value.copy(
            showAddDialog = true,
            editingTransaction = transaction,
            newTitle = transaction.title,
            newAmount = transaction.amount.toString(),
            newType = transaction.type,
            newRecurringType = transaction.recurringType,
            newStartDate = transaction.startDate,
            hasEndDate = transaction.endDate != null,
            newEndDate = transaction.endDate,
            selectedDays = transaction.selectedDays?.toSet() ?: emptySet(),
            selectedDayOfMonth = transaction.selectedDate ?: 1,
            newCategoryId = transaction.categoryId,
            newAccountId = transaction.accountId,
            newNote = transaction.note ?: "",
            excludeHolidays = transaction.excludeHolidays,
            reminderEnabled = transaction.reminderEnabled,
            reminderMinutes = transaction.reminderMinutesBefore
        )
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = false, editingTransaction = null)
    }

    fun updateTitle(title: String) { _uiState.value = _uiState.value.copy(newTitle = title) }
    fun updateAmount(amount: String) { _uiState.value = _uiState.value.copy(newAmount = amount) }
    fun updateType(type: TransactionType) { _uiState.value = _uiState.value.copy(newType = type) }
    fun updateRecurringType(recurringType: RecurringType) { _uiState.value = _uiState.value.copy(newRecurringType = recurringType) }
    fun updateStartDate(date: Long) { _uiState.value = _uiState.value.copy(newStartDate = date) }
    fun updateHasEndDate(hasEndDate: Boolean) { _uiState.value = _uiState.value.copy(hasEndDate = hasEndDate) }
    fun updateEndDate(endDate: Long?) { _uiState.value = _uiState.value.copy(newEndDate = endDate) }
    fun updateSelectedDays(days: Set<Int>) { _uiState.value = _uiState.value.copy(selectedDays = days) }
    fun updateSelectedDayOfMonth(day: Int) { _uiState.value = _uiState.value.copy(selectedDayOfMonth = day) }
    fun updateCategoryId(categoryId: String) { _uiState.value = _uiState.value.copy(newCategoryId = categoryId) }
    fun updateAccountId(accountId: String) { _uiState.value = _uiState.value.copy(newAccountId = accountId) }
    fun updateNote(note: String) { _uiState.value = _uiState.value.copy(newNote = note) }
    fun updateExcludeHolidays(exclude: Boolean) { _uiState.value = _uiState.value.copy(excludeHolidays = exclude) }
    fun updateReminderEnabled(enabled: Boolean) { _uiState.value = _uiState.value.copy(reminderEnabled = enabled) }
    fun updateReminderMinutes(minutes: Int) { _uiState.value = _uiState.value.copy(reminderMinutes = minutes) }

    fun saveRecurringTransaction() {
        val state = _uiState.value
        val amount = state.newAmount.toDoubleOrNull() ?: return

        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val transaction = RecurringTransaction(
                id = state.editingTransaction?.id ?: UUID.randomUUID().toString(),
                title = state.newTitle,
                amount = amount,
                type = state.newType,
                recurringType = state.newRecurringType,
                startDate = state.newStartDate,
                endDate = if (state.hasEndDate) state.newEndDate else null,
                selectedDays = when (state.newRecurringType) {
                    RecurringType.WEEKLY, RecurringType.WEEKDAYS_ONLY, RecurringType.WEEKENDS_ONLY -> state.selectedDays.toList()
                    else -> null
                },
                selectedDate = if (state.newRecurringType == RecurringType.MONTHLY) state.selectedDayOfMonth else null,
                isActive = true,
                categoryId = state.newCategoryId,
                accountId = state.newAccountId,
                note = state.newNote.ifBlank { null },
                lastProcessedDate = state.editingTransaction?.lastProcessedDate,
                excludeHolidays = state.excludeHolidays,
                reminderEnabled = state.reminderEnabled,
                reminderMinutesBefore = state.reminderMinutes,
                createdAt = state.editingTransaction?.createdAt ?: now,
                updatedAt = now
            )

            if (state.editingTransaction != null) {
                recurringRepository.updateRecurringTransaction(transaction)
            } else {
                recurringRepository.addRecurringTransaction(transaction)
            }
            hideAddDialog()
        }
    }

    fun deleteTransaction(transaction: RecurringTransaction) {
        viewModelScope.launch {
            recurringRepository.deleteRecurringTransaction(transaction)
        }
    }

    fun toggleActive(transaction: RecurringTransaction) {
        viewModelScope.launch {
            val updated = transaction.copy(isActive = !transaction.isActive, updatedAt = System.currentTimeMillis())
            recurringRepository.updateRecurringTransaction(updated)
        }
    }

    fun processRecurringTransactions() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val dueTransactions = recurringRepository.getDueRecurringTransactions(now)

            dueTransactions.forEach { recurring ->
                if (shouldProcessTransaction(recurring, now)) {
                    val transaction = Transaction(
                        id = UUID.randomUUID().toString(),
                        amount = recurring.amount,
                        type = recurring.type,
                        categoryId = recurring.categoryId,
                        accountId = recurring.accountId,
                        note = recurring.note,
                        timestamp = now,
                        createdAt = now,
                        updatedAt = now,
                        isSynced = false
                    )
                    transactionRepository.addTransaction(transaction)
                    recurringRepository.updateLastProcessedDate(recurring.id, now)
                }
            }
        }
    }

    private fun shouldProcessTransaction(recurring: RecurringTransaction, currentDate: Long): Boolean {
        recurring.lastProcessedDate?.let { lastProcessed ->
            if (isSameDay(lastProcessed, currentDate)) {
                return false
            }
        }

        if (currentDate < recurring.startDate) return false
        recurring.endDate?.let { endDate ->
            if (currentDate > endDate) return false
        }

        val calendar = Calendar.getInstance().apply { timeInMillis = currentDate }
        val currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val currentDayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)

        return when (recurring.recurringType) {
            RecurringType.DAILY -> true
            RecurringType.WEEKLY -> recurring.selectedDays?.contains(currentDayOfWeek) ?: false
            RecurringType.WEEKDAYS_ONLY -> currentDayOfWeek in 1..5
            RecurringType.WEEKENDS_ONLY -> currentDayOfWeek == 0 || currentDayOfWeek == 6
            RecurringType.MONTHLY -> recurring.selectedDate == currentDayOfMonth
            RecurringType.YEARLY -> {
                val startCalendar = Calendar.getInstance().apply { timeInMillis = recurring.startDate }
                currentDayOfMonth == startCalendar.get(Calendar.DAY_OF_MONTH) &&
                calendar.get(Calendar.MONTH) == startCalendar.get(Calendar.MONTH)
            }
        }
    }

    private fun isSameDay(date1: Long, date2: Long): Boolean {
        val cal1 = Calendar.getInstance().apply { timeInMillis = date1 }
        val cal2 = Calendar.getInstance().apply { timeInMillis = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
    }
}
