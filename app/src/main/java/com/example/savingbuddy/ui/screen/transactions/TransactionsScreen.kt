package com.example.savingbuddy.ui.screen.transactions

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
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
import com.example.savingbuddy.domain.model.Category
import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.model.TransactionType
import com.example.savingbuddy.ui.screen.dashboard.formatCurrency
import com.example.savingbuddy.ui.screen.dashboard.formatDate
import com.example.savingbuddy.ui.theme.GreenIncome
import com.example.savingbuddy.ui.theme.RedExpense
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    navController: NavHostController,
    viewModel: TransactionsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showFilters by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf<TransactionType?>(null) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transactions",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            IconButton(onClick = { showFilters = !showFilters }) {
                Icon(
                    Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = if (showFilters || selectedType != null || selectedCategoryId != null) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { 
                searchQuery = it
                viewModel.filterTransactions(it, selectedType, selectedCategoryId)
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            placeholder = { Text("Search transactions...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { 
                        searchQuery = ""
                        viewModel.filterTransactions("", selectedType, selectedCategoryId)
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        // Type Filter Chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { 
                        selectedType = null
                        viewModel.filterTransactions(searchQuery, null, selectedCategoryId)
                    },
                    label = { Text("All") }
                )
            }
            item {
                FilterChip(
                    selected = selectedType == TransactionType.INCOME,
                    onClick = { 
                        selectedType = TransactionType.INCOME
                        viewModel.filterTransactions(searchQuery, TransactionType.INCOME, selectedCategoryId)
                    },
                    label = { Text("Income") },
                    leadingIcon = if (selectedType == TransactionType.INCOME) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
            item {
                FilterChip(
                    selected = selectedType == TransactionType.EXPENSE,
                    onClick = { 
                        selectedType = TransactionType.EXPENSE
                        viewModel.filterTransactions(searchQuery, TransactionType.EXPENSE, selectedCategoryId)
                    },
                    label = { Text("Expense") },
                    leadingIcon = if (selectedType == TransactionType.EXPENSE) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
            item {
                FilterChip(
                    selected = selectedType == TransactionType.TRANSFER,
                    onClick = { 
                        selectedType = TransactionType.TRANSFER
                        viewModel.filterTransactions(searchQuery, TransactionType.TRANSFER, selectedCategoryId)
                    },
                    label = { Text("Transfer") },
                    leadingIcon = if (selectedType == TransactionType.TRANSFER) {
                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        // Category Filter (shown when type is selected)
        AnimatedVisibility(visible = selectedType != null && uiState.categories.isNotEmpty()) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryId == null,
                        onClick = { 
                            selectedCategoryId = null
                            viewModel.filterTransactions(searchQuery, selectedType, null)
                        },
                        label = { Text("All Categories") }
                    )
                }
                items(uiState.categories.filter { selectedType == null || it.type == selectedType }) { category ->
                    FilterChip(
                        selected = selectedCategoryId == category.id,
                        onClick = { 
                            selectedCategoryId = category.id
                            viewModel.filterTransactions(searchQuery, selectedType, category.id)
                        },
                        label = { Text(category.name) },
                        leadingIcon = { Text(category.icon) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Active filters indicator
        if (selectedType != null || selectedCategoryId != null || searchQuery.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (searchQuery.isNotEmpty()) {
                    AssistChip(
                        onClick = { 
                            searchQuery = ""
                            viewModel.filterTransactions("", selectedType, selectedCategoryId)
                        },
                        label = { Text("Search: $searchQuery") },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                if (selectedType != null) {
                    AssistChip(
                        onClick = { 
                            selectedType = null
                            viewModel.filterTransactions(searchQuery, null, selectedCategoryId)
                        },
                        label = { Text(selectedType!!.name) },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                if (selectedCategoryId != null) {
                    AssistChip(
                        onClick = { 
                            selectedCategoryId = null
                            viewModel.filterTransactions(searchQuery, selectedType, null)
                        },
                        label = { Text(uiState.categories.find { it.id == selectedCategoryId }?.name ?: "Category") },
                        trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { 
                    searchQuery = ""
                    selectedType = null
                    selectedCategoryId = null
                    viewModel.clearFilters()
                }) {
                    Text("Clear All")
                }
            }
        }

        val filteredTransactions = uiState.filteredTransactions

        if (filteredTransactions.isEmpty() && !uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No transactions found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    if (searchQuery.isNotEmpty() || selectedType != null || selectedCategoryId != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Try adjusting your filters",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        )
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val groupedTransactions = filteredTransactions.groupBy { transaction ->
                    SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Date(transaction.timestamp))
                }

                groupedTransactions.forEach { (date, transactions) ->
                    item {
                        Text(
                            text = date,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(transactions, key = { it.id }) { transaction ->
                        SwipeableTransactionItem(
                            transaction = transaction,
                            onDelete = { viewModel.deleteTransaction(transaction) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableTransactionItem(
    transaction: Transaction,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RedExpense),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
        },
        enableDismissFromStartToEnd = false
    ) {
        TransactionListItem(transaction = transaction)
    }
}

@Composable
fun TransactionListItem(transaction: Transaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (transaction.type == TransactionType.EXPENSE) RedExpense.copy(alpha = 0.1f)
                        else GreenIncome.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (transaction.type) {
                        TransactionType.EXPENSE -> Icons.Default.ShoppingBag
                        TransactionType.INCOME -> Icons.Default.TrendingUp
                        TransactionType.TRANSFER -> Icons.Default.SwapHoriz
                    },
                    contentDescription = null,
                    tint = if (transaction.type == TransactionType.EXPENSE) RedExpense else GreenIncome,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = transaction.type.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (!transaction.note.isNullOrBlank()) {
                    Text(
                        text = transaction.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        maxLines = 1
                    )
                }
            }
            Text(
                text = "${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}${formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (transaction.type == TransactionType.EXPENSE) RedExpense else GreenIncome
            )
        }
    }
}