package com.example.savingbuddy.ui.screen.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.savingbuddy.domain.model.SavingsGoal
import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.model.TransactionType
import com.example.savingbuddy.ui.navigation.Screen
import com.example.savingbuddy.ui.theme.GreenIncome
import com.example.savingbuddy.ui.theme.RedExpense
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initializeDefaultData()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = getGreeting(),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            BalanceCard(
                balance = uiState.totalBalance,
                income = uiState.monthlySummary.totalIncome,
                expense = uiState.monthlySummary.totalExpense
            )
        }

        item {
            QuickActionsRow(navController = navController)
        }

        if (uiState.monthlySummary.totalExpense > 0 || uiState.monthlySummary.savingsRate > 0) {
            item {
                InsightCard(
                    savingsRate = uiState.monthlySummary.savingsRate,
                    dailyAverage = uiState.monthlySummary.dailyAverage
                )
            }
        }

        if (uiState.savingsGoals.isNotEmpty()) {
            item {
                Text(
                    text = "Savings Goals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                SavingsGoalsRow(goals = uiState.savingsGoals)
            }
        }

        if (uiState.recentTransactions.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    TextButton(onClick = { navController.navigate(Screen.Transactions.route) }) {
                        Text("See All")
                    }
                }
            }
            items(uiState.recentTransactions) { transaction ->
                TransactionItem(transaction = transaction)
            }
        }
    }
}

@Composable
fun BalanceCard(balance: Double, income: Double, expense: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Total Balance",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
            Text(
                text = formatCurrency(balance),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ArrowUpward,
                            contentDescription = null,
                            tint = GreenIncome,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Income", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    Text(
                        text = formatCurrency(income),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.ArrowDownward,
                            contentDescription = null,
                            tint = RedExpense,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Expense", color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    Text(
                        text = formatCurrency(expense),
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsRow(navController: NavHostController) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        QuickActionButton(
            icon = Icons.Default.Add,
            label = "Expense",
            color = RedExpense,
            onClick = { navController.navigate(Screen.AddTransaction.route) }
        )
        QuickActionButton(
            icon = Icons.Default.TrendingUp,
            label = "Income",
            color = GreenIncome,
            onClick = { navController.navigate(Screen.AddTransaction.route) }
        )
        QuickActionButton(
            icon = Icons.Default.SwapHoriz,
            label = "Transfer",
            color = MaterialTheme.colorScheme.primary,
            onClick = { navController.navigate(Screen.Transfer.route) }
        )
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    color: Color,
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
                .size(56.dp)
                .background(color.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun InsightCard(savingsRate: Float, dailyAverage: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Monthly Insight",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = if (savingsRate > 20) "Great! You're saving ${savingsRate.toInt()}% of income this month."
                           else "Your daily spending average is ${formatCurrency(dailyAverage)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun SavingsGoalsRow(goals: List<SavingsGoal>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(goals) { goal ->
            SavingsGoalCard(goal = goal)
        }
    }
}

@Composable
fun SavingsGoalCard(goal: SavingsGoal) {
    Card(
        modifier = Modifier.width(160.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = goal.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { goal.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(goal.color),
                trackColor = Color(goal.color).copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "${formatCurrency(goal.currentAmount)} / ${formatCurrency(goal.targetAmount)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun TransactionItem(transaction: Transaction) {
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
                    .size(40.dp)
                    .background(
                        if (transaction.type == TransactionType.EXPENSE) RedExpense.copy(alpha = 0.1f)
                        else GreenIncome.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (transaction.type == TransactionType.EXPENSE) Icons.Default.ShoppingBag else Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = if (transaction.type == TransactionType.EXPENSE) RedExpense else GreenIncome,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (transaction.type == TransactionType.EXPENSE) "Expense" else "Income",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = formatDate(transaction.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Text(
                text = "${if (transaction.type == TransactionType.EXPENSE) "-" else "+"}${formatCurrency(transaction.amount)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (transaction.type == TransactionType.EXPENSE) RedExpense else GreenIncome
            )
        }
    }
}

fun formatCurrency(amount: Double): String {
    val format = NumberFormat.getCurrencyInstance(Locale("en", "BD"))
    return format.format(amount)
}

fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}