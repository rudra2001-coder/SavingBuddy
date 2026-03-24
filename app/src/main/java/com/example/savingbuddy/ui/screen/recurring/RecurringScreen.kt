package com.example.savingbuddy.ui.screen.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.savingbuddy.domain.model.*
import com.example.savingbuddy.ui.screen.dashboard.formatCurrency
import com.example.savingbuddy.ui.theme.GreenIncome
import com.example.savingbuddy.ui.theme.RedExpense

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringScreen(
    navController: NavHostController,
    viewModel: RecurringViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Recurring")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recurring Transactions",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    FilledTonalButton(onClick = { viewModel.processRecurringTransactions() }) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run Now")
                    }
                }
            }

            if (uiState.transactions.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No recurring transactions",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Tap + to create automatic transactions",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            } else {
                items(uiState.transactions) { transaction ->
                    RecurringTransactionCard(
                        transaction = transaction,
                        onEdit = { viewModel.editTransaction(transaction) },
                        onDelete = { viewModel.deleteTransaction(transaction) },
                        onToggle = { viewModel.toggleActive(transaction) }
                    )
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        RecurringFormDialog(
            uiState = uiState,
            onTitleChange = { viewModel.updateTitle(it) },
            onAmountChange = { viewModel.updateAmount(it) },
            onTypeChange = { viewModel.updateType(it) },
            onRecurringTypeChange = { viewModel.updateRecurringType(it) },
            onStartDateChange = { viewModel.updateStartDate(it) },
            onHasEndDateChange = { viewModel.updateHasEndDate(it) },
            onEndDateChange = { viewModel.updateEndDate(it) },
            onSelectedDaysChange = { viewModel.updateSelectedDays(it) },
            onSelectedDayOfMonthChange = { viewModel.updateSelectedDayOfMonth(it) },
            onCategoryChange = { viewModel.updateCategoryId(it) },
            onAccountChange = { viewModel.updateAccountId(it) },
            onNoteChange = { viewModel.updateNote(it) },
            onExcludeHolidaysChange = { viewModel.updateExcludeHolidays(it) },
            onReminderEnabledChange = { viewModel.updateReminderEnabled(it) },
            onReminderMinutesChange = { viewModel.updateReminderMinutes(it) },
            onDismiss = { viewModel.hideAddDialog() },
            onSave = { viewModel.saveRecurringTransaction() }
        )
    }
}

@Composable
fun RecurringTransactionCard(
    transaction: RecurringTransaction,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                if (transaction.type == TransactionType.INCOME) GreenIncome.copy(alpha = 0.1f)
                                else RedExpense.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            if (transaction.type == TransactionType.INCOME) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = if (transaction.type == TransactionType.INCOME) GreenIncome else RedExpense
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = transaction.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (transaction.isActive) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = getRecurringDescription(transaction),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Row {
                    IconButton(onClick = onToggle) {
                        Icon(
                            if (transaction.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = "Toggle",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatCurrency(transaction.amount),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.type == TransactionType.INCOME) GreenIncome else RedExpense
                )
                if (!transaction.isActive) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = "Paused",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

fun getRecurringDescription(transaction: RecurringTransaction): String {
    return when (transaction.recurringType) {
        RecurringType.DAILY -> "Every day"
        RecurringType.WEEKLY -> {
            val days = transaction.selectedDays?.map { DayOfWeek.fromInt(it).shortName } ?: emptyList()
            if (days.isEmpty()) "Weekly" else "Every ${days.joinToString(", ")}"
        }
        RecurringType.WEEKDAYS_ONLY -> "Weekdays (Mon-Fri)"
        RecurringType.WEEKENDS_ONLY -> "Weekends (Sat-Sun)"
        RecurringType.MONTHLY -> "Every ${transaction.selectedDate}${getOrdinalSuffix(transaction.selectedDate ?: 1)} of month"
        RecurringType.YEARLY -> "Yearly"
    }
}

fun getOrdinalSuffix(day: Int): String {
    return when {
        day % 100 in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurringFormDialog(
    uiState: RecurringUiState,
    onTitleChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onTypeChange: (TransactionType) -> Unit,
    onRecurringTypeChange: (RecurringType) -> Unit,
    onStartDateChange: (Long) -> Unit,
    onHasEndDateChange: (Boolean) -> Unit,
    onEndDateChange: (Long?) -> Unit,
    onSelectedDaysChange: (Set<Int>) -> Unit,
    onSelectedDayOfMonthChange: (Int) -> Unit,
    onCategoryChange: (String) -> Unit,
    onAccountChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onExcludeHolidaysChange: (Boolean) -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderMinutesChange: (Int) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (uiState.editingTransaction != null) "Edit Recurring" else "Add Recurring") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = uiState.newTitle,
                    onValueChange = onTitleChange,
                    label = { Text("Title") },
                    placeholder = { Text("e.g., Monthly Rent") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = uiState.newAmount,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            onAmountChange(newValue)
                        }
                    },
                    label = { Text("Amount (৳)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = uiState.newType == TransactionType.EXPENSE,
                        onClick = { onTypeChange(TransactionType.EXPENSE) },
                        label = { Text("Expense", color = RedExpense) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = uiState.newType == TransactionType.INCOME,
                        onClick = { onTypeChange(TransactionType.INCOME) },
                        label = { Text("Income", color = GreenIncome) },
                        modifier = Modifier.weight(1f)
                    )
                }

                Text(
                    text = "Repeat",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(RecurringType.entries.toList()) { type ->
                        FilterChip(
                            selected = uiState.newRecurringType == type,
                            onClick = { onRecurringTypeChange(type) },
                            label = {
                                Text(
                                    when (type) {
                                        RecurringType.DAILY -> "Daily"
                                        RecurringType.WEEKLY -> "Weekly"
                                        RecurringType.WEEKDAYS_ONLY -> "Weekdays"
                                        RecurringType.WEEKENDS_ONLY -> "Weekends"
                                        RecurringType.MONTHLY -> "Monthly"
                                        RecurringType.YEARLY -> "Yearly"
                                    },
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                when (uiState.newRecurringType) {
                    RecurringType.WEEKLY -> {
                        Text(
                            text = "Select Days",
                            style = MaterialTheme.typography.titleSmall
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            items(DayOfWeek.entries.toList()) { day ->
                                FilterChip(
                                    selected = uiState.selectedDays.contains(day.value),
                                    onClick = {
                                        val newDays = if (uiState.selectedDays.contains(day.value)) {
                                            uiState.selectedDays - day.value
                                        } else {
                                            uiState.selectedDays + day.value
                                        }
                                        onSelectedDaysChange(newDays)
                                    },
                                    label = { Text(day.shortName, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                    RecurringType.MONTHLY -> {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = it }
                        ) {
                            OutlinedTextField(
                                value = "${uiState.selectedDayOfMonth}${getOrdinalSuffix(uiState.selectedDayOfMonth)} of month",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Day of Month") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor()
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                (1..31).forEach { day ->
                                    DropdownMenuItem(
                                        text = { Text("$day${getOrdinalSuffix(day)}") },
                                        onClick = {
                                            onSelectedDayOfMonthChange(day)
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    else -> {}
                }

                var categoryExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.categories.find { it.id == uiState.newCategoryId }?.name ?: "Select Category",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        uiState.categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    onCategoryChange(category.id)
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                var accountExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = accountExpanded,
                    onExpandedChange = { accountExpanded = it }
                ) {
                    OutlinedTextField(
                        value = uiState.accounts.find { it.id == uiState.newAccountId }?.name ?: "Select Account",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Account") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = accountExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = accountExpanded,
                        onDismissRequest = { accountExpanded = false }
                    ) {
                        uiState.accounts.forEach { account ->
                            DropdownMenuItem(
                                text = { Text(account.name) },
                                onClick = {
                                    onAccountChange(account.id)
                                    accountExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = uiState.newNote,
                    onValueChange = onNoteChange,
                    label = { Text("Note (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = uiState.reminderEnabled,
                        onCheckedChange = onReminderEnabledChange
                    )
                    Text("Enable Reminder")
                }

                Button(
                    onClick = onSave,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.newTitle.isNotBlank() && (uiState.newAmount.toDoubleOrNull() ?: 0.0) > 0
                ) {
                    Text(if (uiState.editingTransaction != null) "Update" else "Save")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
