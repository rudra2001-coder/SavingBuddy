package com.example.savingbuddy.ui.screen.analytics

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.savingbuddy.ui.screen.dashboard.formatCurrency
import com.example.savingbuddy.ui.theme.GreenIncome
import com.example.savingbuddy.ui.theme.RedExpense

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsScreen(
    viewModel: AnalyticsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
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
                    text = "Analytics",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.refresh() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                }
            }
        }

        // Overview Card
        item {
            OverviewCard(
                totalBalance = uiState.totalBalance,
                totalIncome = uiState.totalIncome,
                totalExpense = uiState.totalExpense,
                savingsRate = uiState.savingsRate,
                transactionCount = uiState.transactionCount
            )
        }

        // Net Worth
        item {
            NetWorthCard(
                netWorth = uiState.netWorth,
                totalAssets = uiState.totalBalance + uiState.totalSaved,
                totalLiabilities = uiState.totalDebt + uiState.creditCardBalance
            )
        }

        // Income vs Expense
        item {
            IncomeExpenseCard(
                income = uiState.totalIncome,
                expense = uiState.totalExpense
            )
        }

        // Top Spending Categories
        if (uiState.topCategories.isNotEmpty()) {
            item {
                Text(
                    text = "Top Spending Categories",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(uiState.topCategories) { category ->
                CategorySpendingItem(category = category)
            }
        }

        // Daily Spending Pattern
        if (uiState.dailySpending.any { it.amount > 0 }) {
            item {
                Text(
                    text = "Spending by Day",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                DailySpendingChart(dailySpending = uiState.dailySpending)
            }
        }

        // Weekly Trend
        if (uiState.weeklyTrend.any { it.income > 0 || it.expense > 0 }) {
            item {
                Text(
                    text = "Weekly Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            item {
                WeeklyTrendChart(weeklyData = uiState.weeklyTrend)
            }
        }

        // Budget Status
        if (uiState.budgetOverview.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Budget Status",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (uiState.overBudgetCount > 0) {
                        Text(
                            text = "${uiState.overBudgetCount} over budget",
                            style = MaterialTheme.typography.labelMedium,
                            color = RedExpense
                        )
                    }
                }
            }
            items(uiState.budgetOverview) { budget ->
                BudgetStatusItem(budget = budget)
            }
        }

        // Life Statistics
        item {
            Text(
                text = "Life Dashboard",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        item {
            LifeStatsRow(
                habits = uiState.activeHabits,
                tasksTotal = uiState.totalTasks,
                tasksCompleted = uiState.completedTasks,
                focusMinutes = uiState.focusMinutes,
                journalEntries = uiState.journalEntries
            )
        }

        // Accounts Overview
        if (uiState.accounts.isNotEmpty()) {
            item {
                Text(
                    text = "Accounts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(uiState.accounts.size.coerceAtMost(5)) { index ->
                val account = uiState.accounts[index]
                AccountStatItem(name = account.name, balance = account.balance, icon = account.icon)
            }
        }

        // Savings Goals
        if (uiState.savingsGoals.isNotEmpty()) {
            item {
                Text(
                    text = "Savings Goals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            items(uiState.savingsGoals.size.coerceAtMost(3)) { index ->
                val goal = uiState.savingsGoals[index]
                SavingsGoalItem(
                    name = goal.name,
                    current = goal.currentAmount,
                    target = goal.targetAmount,
                    icon = goal.icon
                )
            }
        }

        // Debt Summary
        if (uiState.totalDebt > 0 || uiState.creditCardBalance > 0) {
            item {
                Text(
                    text = "Debt Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (uiState.totalDebt > 0) {
                item {
                    DebtItem(type = "Loans", amount = uiState.totalDebt, icon = "🏦")
                }
            }
            if (uiState.creditCardBalance > 0) {
                item {
                    DebtItem(type = "Credit Cards", amount = uiState.creditCardBalance, icon = "💳")
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun OverviewCard(
    totalBalance: Double,
    totalIncome: Double,
    totalExpense: Double,
    savingsRate: Float,
    transactionCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = formatCurrency(totalBalance),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Total Balance",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Income", value = formatCurrency(totalIncome), color = GreenIncome)
                StatItem(label = "Expense", value = formatCurrency(totalExpense), color = RedExpense)
                StatItem(label = "Savings Rate", value = "${savingsRate.toInt()}%", color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$transactionCount transactions this month",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun StatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun NetWorthCard(netWorth: Double, totalAssets: Double, totalLiabilities: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (netWorth >= 0) Color(0xFF4CAF50).copy(alpha = 0.1f)
                        else Color(0xFFF44336).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = if (netWorth >= 0) Color(0xFF4CAF50) else Color(0xFFF44336),
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Net Worth",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Assets: ${formatCurrency(totalAssets)} | Liabilities: ${formatCurrency(totalLiabilities)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = formatCurrency(netWorth),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = if (netWorth >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}

@Composable
fun IncomeExpenseCard(income: Double, expense: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = GreenIncome)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = formatCurrency(income), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = GreenIncome)
                Text(text = "Income", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = RedExpense)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = formatCurrency(expense), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = RedExpense)
                Text(text = "Expense", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun CategorySpendingItem(category: CategorySpending) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = category.icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = category.categoryName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                LinearProgressIndicator(
                    progress = { category.percentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = Color(category.color),
                    trackColor = Color(category.color).copy(alpha = 0.2f)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(text = formatCurrency(category.amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                Text(text = "${category.percentage.toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DailySpendingChart(dailySpending: List<DailySpending>) {
    val maxAmount = dailySpending.maxOfOrNull { it.amount } ?: 1.0
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dailySpending.forEach { day ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height((80 * (day.amount / maxAmount)).dp.coerceAtLeast(4.dp))
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = day.dayOfWeek, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun WeeklyTrendChart(weeklyData: List<WeeklyData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            weeklyData.forEach { week ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = week.weekLabel, style = MaterialTheme.typography.labelSmall, modifier = Modifier.width(50.dp))
                    Row(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .weight((week.income / (week.income + week.expense).coerceAtLeast(1.0)).toFloat().coerceIn(0.1f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(GreenIncome)
                        )
                        Box(
                            modifier = Modifier
                                .weight((week.expense / (week.income + week.expense).coerceAtLeast(1.0)).toFloat().coerceIn(0.1f, 1f))
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(RedExpense)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(GreenIncome, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Income", style = MaterialTheme.typography.labelSmall)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(8.dp).background(RedExpense, CircleShape))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Expense", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun BudgetStatusItem(budget: BudgetStatus) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (budget.isOver) Color(0xFFF44336).copy(alpha = 0.1f) else MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = budget.icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = budget.categoryName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text(text = "${formatCurrency(budget.spent)} / ${formatCurrency(budget.limit)}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { budget.percentage / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = if (budget.isOver) RedExpense else MaterialTheme.colorScheme.primary,
                trackColor = if (budget.isOver) RedExpense.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun LifeStatsRow(habits: Int, tasksTotal: Int, tasksCompleted: Int, focusMinutes: Int, journalEntries: Int) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { LifeStatCard(icon = "🔥", value = "$habits", label = "Habits") }
        item { LifeStatCard(icon = "✅", value = "$tasksCompleted/$tasksTotal", label = "Tasks") }
        item { LifeStatCard(icon = "🧘", value = "$focusMinutes min", label = "Focus") }
        item { LifeStatCard(icon = "📝", value = "$journalEntries", label = "Journal") }
    }
}

@Composable
fun LifeStatCard(icon: String, value: String, label: String) {
    Card(
        modifier = Modifier.width(80.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, fontSize = 24.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun AccountStatItem(name: String, balance: Double, icon: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(text = formatCurrency(balance), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SavingsGoalItem(name: String, current: Double, target: Double, icon: String) {
    val progress = if (target > 0) (current / target).toFloat() else 0f
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, fontSize = 20.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text(text = "${formatCurrency(current)} / ${formatCurrency(target)}", style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun DebtItem(type: String, amount: Double, icon: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336).copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = icon, fontSize = 20.sp)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text = type, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
            Text(text = formatCurrency(amount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color(0xFFF44336))
        }
    }
}