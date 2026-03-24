package com.example.savingbuddy.ui.screen.transactions

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Transactions",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.transactions.isEmpty() && !uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "No transactions yet",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tap + to add your first transaction",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val groupedTransactions = uiState.transactions.groupBy { transaction ->
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