package com.example.savingbuddy.ui.screen.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.savingbuddy.domain.model.SavingsGoal
import com.example.savingbuddy.domain.model.InsightType
import com.example.savingbuddy.domain.model.SpendingInsight
import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.model.TransactionType
import com.example.savingbuddy.domain.usecase.FinancialHealthScore
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

        // Health Score Card
        uiState.healthScore?.let { score ->
            item {
                HealthScoreCard(
                    score = score,
                    onClick = { navController.navigate(Screen.HealthScore.route) }
                )
            }
        }

        // Budget Overview
        if (uiState.budgetStatus.totalBudget > 0) {
            item {
                BudgetOverviewCard(
                    budgetStatus = uiState.budgetStatus,
                    onClick = { navController.navigate(Screen.AllFunctions.route) }
                )
            }
        }

        // Spending Insights
        if (uiState.spendingInsights.isNotEmpty()) {
            item {
                Text(
                    text = "Spending Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(uiState.spendingInsights) { insight ->
                SpendingInsightCard(insight = insight)
            }
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
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        item {
            QuickActionButton(
                icon = Icons.Default.Add,
                label = "Expense",
                color = RedExpense,
                onClick = { navController.navigate(Screen.AddTransaction.route) }
            )
        }
        item {
            QuickActionButton(
                icon = Icons.Default.TrendingUp,
                label = "Income",
                color = GreenIncome,
                onClick = { navController.navigate(Screen.AddTransaction.route) }
            )
        }
        item {
            QuickActionButton(
                icon = Icons.Default.SwapHoriz,
                label = "Transfer",
                color = MaterialTheme.colorScheme.primary,
                onClick = { navController.navigate(Screen.Transfer.route) }
            )
        }
        item {
            QuickActionButton(
                icon = Icons.Default.Receipt,
                label = "Transactions",
                color = MaterialTheme.colorScheme.tertiary,
                onClick = { navController.navigate(Screen.Transactions.route) }
            )
        }
        item {
            QuickActionButton(
                icon = Icons.Default.Analytics,
                label = "Analytics",
                color = MaterialTheme.colorScheme.secondary,
                onClick = { navController.navigate(Screen.Analytics.route) }
            )
        }
        item {
            QuickActionButton(
                icon = Icons.Default.Savings,
                label = "Savings",
                color = GreenIncome,
                onClick = { navController.navigate(Screen.Savings.route) }
            )
        }
        item {
            QuickActionButton(
                icon = Icons.Default.HealthAndSafety,
                label = "Health",
                color = Color(0xFF4CAF50),
                onClick = { navController.navigate(Screen.Health.route) }
            )
        }
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

@Composable
fun HealthScoreCard(
    score: FinancialHealthScore,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Financial Health Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular Progress Indicator
                val animatedScore by animateFloatAsState(
                    targetValue = score.overallScore / 100f,
                    animationSpec = tween(durationMillis = 1000),
                    label = "score_animation"
                )
                
                Box(
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { animatedScore },
                        modifier = Modifier.size(80.dp),
                        strokeWidth = 8.dp,
                        trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                        color = when {
                            score.overallScore >= 80 -> Color(0xFF4CAF50)
                            score.overallScore >= 60 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        },
                        strokeCap = StrokeCap.Round
                    )
                    Text(
                        text = "${score.overallScore}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = score.grade,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = score.recommendations.firstOrNull() ?: "Keep up the good work!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetOverviewCard(
    budgetStatus: BudgetStatus,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Budget Overview",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = "View Details",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            val percentage = if (budgetStatus.totalBudget > 0) {
                (budgetStatus.spent / budgetStatus.totalBudget).toFloat()
            } else 0f
            
            val animatedProgress by animateFloatAsState(
                targetValue = percentage.coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 800),
                label = "budget_animation"
            )
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    percentage < 0.7f -> Color(0xFF4CAF50)
                    percentage < 0.9f -> Color(0xFFFF9800)
                    else -> Color(0xFFF44336)
                },
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.2f)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Spent",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatCurrency(budgetStatus.spent),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatCurrency(budgetStatus.remaining),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatCurrency(budgetStatus.totalBudget),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

@Composable
fun SpendingInsightCard(insight: SpendingInsight) {
    val insightColor = when (insight.type) {
        InsightType.SPENDING_INCREASE -> Color(0xFFF44336)
        InsightType.SPENDING_DECREASE -> Color(0xFF4CAF50)
        InsightType.CATEGORY_DOMINANCE -> Color(0xFFFF9800)
        InsightType.WEEKEND_SPENDING -> Color(0xFF9C27B0)
        InsightType.DAILY_AVERAGE -> Color(0xFF2196F3)
        InsightType.BUDGET_WARNING -> Color(0xFFF44336)
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        insightColor.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (insight.type) {
                        InsightType.SPENDING_INCREASE -> Icons.Default.TrendingUp
                        InsightType.SPENDING_DECREASE -> Icons.Default.TrendingDown
                        InsightType.CATEGORY_DOMINANCE -> Icons.Default.PieChart
                        InsightType.WEEKEND_SPENDING -> Icons.Default.CalendarMonth
                        InsightType.DAILY_AVERAGE -> Icons.Default.Calculate
                        InsightType.BUDGET_WARNING -> Icons.Default.Warning
                    },
                    contentDescription = null,
                    tint = insightColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// BudgetStatus is defined in DashboardViewModel

fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}