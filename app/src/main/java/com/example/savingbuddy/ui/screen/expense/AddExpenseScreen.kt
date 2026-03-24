package com.example.savingbuddy.ui.screen.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.savingbuddy.domain.model.Category
import com.example.savingbuddy.domain.model.TransactionType
import com.example.savingbuddy.domain.usecase.SpendingAdvice
import com.example.savingbuddy.domain.usecase.SpendingSafetyLevel
import com.example.savingbuddy.ui.theme.RedExpense
import com.example.savingbuddy.ui.screen.dashboard.formatCurrency
import com.example.savingbuddy.ui.screen.transactions.AccountSelector
import com.example.savingbuddy.ui.screen.expense.AddExpenseViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseScreen(
    navController: NavHostController,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }

    LaunchedEffect(Unit) {
        viewModel.resetForm()
        viewModel.updateType(TransactionType.EXPENSE)
    }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            navController.popBackStack()
        }
    }

    if (uiState.showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = uiState.selectedDate
        )
        DatePickerDialog(
            onDismissRequest = { viewModel.hideDatePicker() },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.updateDate(it) }
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDatePicker() }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Expense", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = RedExpense.copy(alpha = 0.1f)
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DateSelectorCard(
                selectedDate = uiState.selectedDate,
                dateFormat = dateFormat,
                onClick = { viewModel.showDatePicker() }
            )

            ExpenseAmountInput(
                amount = uiState.amount,
                onAmountChange = { viewModel.updateAmount(it) }
            )

            QuickAmountChips(
                quickAmounts = uiState.quickAmounts,
                onAmountSelected = { viewModel.setQuickAmount(it) }
            )

            uiState.spendingAdvice?.let { advice ->
                SpendingAdvisorCard(advice = advice)
            }

            Text(
                text = "Expense Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            ExpenseCategoryGrid(
                categories = uiState.expenseCategories,
                selectedCategory = uiState.selectedCategory,
                onCategorySelected = { viewModel.selectCategory(it) }
            )

            if (uiState.accounts.isNotEmpty()) {
                Text(
                    text = "Account",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )

                AccountSelector(
                    accounts = uiState.accounts,
                    selectedAccount = uiState.selectedAccount,
                    onAccountSelected = { viewModel.selectAccount(it) }
                )
            }

            OutlinedTextField(
                value = uiState.note,
                onValueChange = { viewModel.updateNote(it) },
                label = { Text("Note (optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { viewModel.saveTransaction() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = RedExpense)
            ) {
                Text("Add Expense", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun DateSelectorCard(
    selectedDate: Long,
    dateFormat: SimpleDateFormat,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Calendar",
                    tint = RedExpense,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = dateFormat.format(Date(selectedDate)),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun QuickAmountChips(
    quickAmounts: List<Long>,
    onAmountSelected: (Long) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(quickAmounts) { amount ->
            FilterChip(
                selected = false,
                onClick = { onAmountSelected(amount) },
                label = {
                    Text(
                        text = "৳$amount",
                        fontWeight = FontWeight.Medium
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = RedExpense.copy(alpha = 0.1f),
                    labelColor = RedExpense
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = RedExpense.copy(alpha = 0.3f),
                    selectedBorderColor = RedExpense,
                    enabled = true,
                    selected = false
                )
            )
        }
    }
}

@Composable
fun SpendingAdvisorCard(advice: SpendingAdvice) {
    val backgroundColor = when (advice.safetyLevel) {
        SpendingSafetyLevel.SAFE -> Color(0xFF4CAF50).copy(alpha = 0.1f)
        SpendingSafetyLevel.MODERATE -> Color(0xFFFF9800).copy(alpha = 0.1f)
        SpendingSafetyLevel.WARNING -> Color(0xFFFF5722).copy(alpha = 0.1f)
        SpendingSafetyLevel.DANGEROUS -> Color(0xFFF44336).copy(alpha = 0.1f)
    }
    
    val borderColor = when (advice.safetyLevel) {
        SpendingSafetyLevel.SAFE -> Color(0xFF4CAF50)
        SpendingSafetyLevel.MODERATE -> Color(0xFFFF9800)
        SpendingSafetyLevel.WARNING -> Color(0xFFFF5722)
        SpendingSafetyLevel.DANGEROUS -> Color(0xFFF44336)
    }
    
    val icon = when (advice.safetyLevel) {
        SpendingSafetyLevel.SAFE -> "✅"
        SpendingSafetyLevel.MODERATE -> "⚠️"
        SpendingSafetyLevel.WARNING -> "🔴"
        SpendingSafetyLevel.DANGEROUS -> "🚨"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = icon,
                    fontSize = 20.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = advice.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = borderColor
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = advice.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Daily Budget Left",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatCurrency(advice.dailyRemaining),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "This as % of Daily",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = "${String.format("%.0f", advice.percentOfDaily)}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (advice.percentOfDaily > 100) borderColor else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
fun ExpenseAmountInput(
    amount: String,
    onAmountChange: (String) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Amount",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "৳",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = RedExpense
            )
            TextField(
                value = amount,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        onAmountChange(newValue)
                    }
                },
                textStyle = MaterialTheme.typography.displayMedium.copy(
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = RedExpense
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.widthIn(min = 100.dp)
            )
        }
    }
}

@Composable
fun ExpenseCategoryGrid(
    categories: List<Category>,
    selectedCategory: Category?,
    onCategorySelected: (Category) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.height(160.dp)
    ) {
        items(categories) { category ->
            ExpenseCategoryItem(
                category = category,
                isSelected = category.id == selectedCategory?.id,
                onClick = { onCategorySelected(category) }
            )
        }
    }
}

@Composable
fun ExpenseCategoryItem(
    category: Category,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    if (isSelected) Color(category.color).copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape
                )
                .then(
                    if (isSelected) Modifier.border(2.dp, Color(category.color), CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = category.icon.take(2).uppercase(),
                fontWeight = FontWeight.Bold,
                color = Color(category.color)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}
