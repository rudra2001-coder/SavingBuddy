package com.example.savingbuddy.ui.screen.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val showFloatingButton by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.initializeDefaultData()
        delay(500)
    }

    Scaffold(
        floatingActionButton = {
            if (showFloatingButton) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddTransaction.route) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Transaction",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AnimatedGreetingHeader(
                    totalBalance = uiState.totalBalance,
                    monthlyIncome = uiState.monthlySummary.totalIncome,
                    monthlyExpense = uiState.monthlySummary.totalExpense
                )
            }

            item {
                AnimatedBalanceCard(
                    balance = uiState.totalBalance,
                    income = uiState.monthlySummary.totalIncome,
                    expense = uiState.monthlySummary.totalExpense
                )
            }

            item {
                QuickActionsRowEnhanced(navController = navController)
            }

            uiState.healthScore?.let { score ->
                item {
                    AnimatedHealthScoreCard(
                        score = score,
                        onClick = { navController.navigate(Screen.Health.route) }
                    )
                }
            }

            if (uiState.budgetStatus.totalBudget > 0) {
                item {
                    AnimatedBudgetOverviewCard(
                        budgetStatus = uiState.budgetStatus,
                        onClick = { navController.navigate(Screen.Budget.route) }
                    )
                }
            }

            if (uiState.spendingInsights.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Spending Insights",
                        actionText = "View All",
                        onActionClick = { navController.navigate(Screen.Analytics.route) }
                    )
                }
                items(uiState.spendingInsights) { insight ->
                    AnimatedSpendingInsightCard(insight = insight)
                }
            }

            if (uiState.monthlySummary.totalExpense > 0 || uiState.monthlySummary.savingsRate > 0) {
                item {
                    SectionHeader(
                        title = "Monthly Statistics",
                        actionText = null,
                        onActionClick = {}
                    )
                }
                item {
                    MonthlyStatisticsCard(
                        savingsRate = uiState.monthlySummary.savingsRate,
                        dailyAverage = uiState.monthlySummary.dailyAverage,
                        totalTransactions = uiState.recentTransactions.size
                    )
                }
            }

            if (uiState.savingsGoals.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Savings Goals",
                        actionText = "Add Goal",
                        onActionClick = { navController.navigate(Screen.Savings.route) }
                    )
                }
                item {
                    AnimatedSavingsGoalsRow(goals = uiState.savingsGoals.take(3))
                }
            }

            if (uiState.recentTransactions.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Recent Transactions",
                        actionText = "See All",
                        onActionClick = { navController.navigate(Screen.Transactions.route) }
                    )
                }
                items(uiState.recentTransactions) { transaction ->
                    AnimatedTransactionItem(
                        transaction = transaction,
                        onClick = { }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedGreetingHeader(
    totalBalance: Double,
    monthlyIncome: Double,
    monthlyExpense: Double
) {
    val greeting = getGreeting()
    val netChange = monthlyIncome - monthlyExpense
    val isPositive = netChange >= 0

    var animatedText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        animatedText = greeting
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = animatedText,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )

            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Text(
                    text = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        AnimatedContent(
            targetState = netChange,
            transitionSpec = {
                fadeIn() + slideInVertically() with fadeOut() + slideOutVertically()
            }
        ) { change ->
            Text(
                text = if (isPositive) "↑ Up ${formatCurrency(change)} this month"
                       else "↓ Down ${formatCurrency(-change)} this month",
                style = MaterialTheme.typography.bodySmall,
                color = if (isPositive) GreenIncome else RedExpense,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun AnimatedBalanceCard(balance: Double, income: Double, expense: Double) {
    var showCard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showCard = true
    }

    AnimatedVisibility(
        visible = showCard,
        enter = fadeIn(animationSpec = tween(500)) +
                slideInVertically(initialOffsetY = { -50 })
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .shadow(8.dp, RoundedCornerShape(24.dp), spotColor = Color(0xFF4CAF50).copy(alpha = 0.2f)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Balance",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedNumber(
                    value = balance,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    )
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
                            Text(
                                text = "Income",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = formatCurrency(income),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
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
                            Text(
                                text = "Expense",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                        Text(
                            text = formatCurrency(expense),
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val incomePercentage = if (income > 0) (expense / income).toFloat().coerceIn(0f, 1f) else 0f
                LinearProgressIndicator(
                    progress = { incomePercentage },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = RedExpense,
                    trackColor = GreenIncome.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
fun QuickActionsRowEnhanced(navController: NavHostController) {
    val quickActions = listOf(
        QuickAction(Icons.Default.Add, "Expense", RedExpense, Screen.AddExpense.route),
        QuickAction(Icons.Default.TrendingUp, "Income", GreenIncome, Screen.AddIncome.route),
        QuickAction(Icons.Default.SwapHoriz, "Transfer", MaterialTheme.colorScheme.primary, Screen.Transfer.route),
        QuickAction(Icons.Default.Receipt, "History", MaterialTheme.colorScheme.tertiary, Screen.Transactions.route),
        QuickAction(Icons.Default.Analytics, "Analytics", MaterialTheme.colorScheme.secondary, Screen.Analytics.route),
        QuickAction(Icons.Default.Savings, "Savings", GreenIncome, Screen.Savings.route),
        QuickAction(Icons.Default.HealthAndSafety, "Health", Color(0xFF4CAF50), Screen.Health.route),
        QuickAction(Icons.Default.BarChart, "Budget", Color(0xFF2196F3), Screen.Budget.route)
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(quickActions) { action ->
            var isPressed by remember { mutableStateOf(false) }

            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.95f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(scale)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { navController.navigate(action.route) }
                    .padding(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(action.color.copy(alpha = 0.1f), CircleShape)
                        .shadow(4.dp, CircleShape, spotColor = action.color.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        action.icon,
                        contentDescription = action.label,
                        tint = action.color,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = action.label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun AnimatedHealthScoreCard(
    score: FinancialHealthScore,
    onClick: () -> Unit
) {
    var showCard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showCard = true
    }

    AnimatedVisibility(
        visible = showCard,
        enter = fadeIn(animationSpec = tween(600)) + slideInHorizontally()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .clickable(onClick = onClick)
                .shadow(4.dp, RoundedCornerShape(16.dp)),
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
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
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
                    val animatedScore by animateFloatAsState(
                        targetValue = score.overallScore / 100f,
                        animationSpec = tween(
                            durationMillis = 1500,
                            easing = FastOutSlowInEasing
                        ),
                        label = "score_animation"
                    )

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(90.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedScore },
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 8.dp,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f),
                            color = when {
                                score.overallScore >= 80 -> Color(0xFF4CAF50)
                                score.overallScore >= 60 -> Color(0xFFFF9800)
                                else -> Color(0xFFF44336)
                            },
                            strokeCap = StrokeCap.Round
                        )

                        AnimatedNumber(
                            value = score.overallScore.toDouble(),
                            modifier = Modifier,
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp
                            ),
                            format = { "${it.toInt()}" }
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = score.grade,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = score.recommendations.firstOrNull() ?: "Keep up the good work!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedBudgetOverviewCard(
    budgetStatus: BudgetStatus,
    onClick: () -> Unit
) {
    var showCard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showCard = true
    }

    AnimatedVisibility(
        visible = showCard,
        enter = fadeIn(animationSpec = tween(600)) + slideInVertically()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
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
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
                    label = "budget_animation"
                )

                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
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
                    BudgetStat(
                        label = "Spent",
                        value = budgetStatus.spent,
                        color = when {
                            percentage < 0.7f -> Color(0xFF4CAF50)
                            percentage < 0.9f -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                    BudgetStat(
                        label = "Remaining",
                        value = budgetStatus.remaining,
                        color = Color(0xFF2196F3)
                    )
                    BudgetStat(
                        label = "Budget",
                        value = budgetStatus.totalBudget,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }

                if (percentage > 0.9f) {
                    Spacer(modifier = Modifier.height(8.dp))
                    WarningChip(
                        message = "You've used ${(percentage * 100).toInt()}% of your budget!",
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

@Composable
fun BudgetStat(label: String, value: Double, color: Color) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
        )
        Text(
            text = formatCurrency(value),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = color
        )
    }
}

@Composable
fun WarningChip(message: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = color,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
fun MonthlyStatisticsCard(
    savingsRate: Float,
    dailyAverage: Double,
    totalTransactions: Int
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFF5F5F5)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatisticItem(
                icon = Icons.Default.Savings,
                label = "Savings Rate",
                value = "${savingsRate.toInt()}%",
                color = GreenIncome
            )
            StatisticItem(
                icon = Icons.Default.CalendarToday,
                label = "Daily Avg",
                value = formatCurrency(dailyAverage),
                color = Color(0xFF2196F3)
            )
            StatisticItem(
                icon = Icons.Default.Receipt,
                label = "Transactions",
                value = "$totalTransactions",
                color = Color(0xFFFF9800)
            )
        }
    }
}

@Composable
fun StatisticItem(icon: ImageVector, label: String, value: String, color: Color) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = color
        )
    }
}

@Composable
fun AnimatedSpendingInsightCard(insight: SpendingInsight) {
    var showCard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(100)
        showCard = true
    }

    AnimatedVisibility(
        visible = showCard,
        enter = fadeIn() + slideInHorizontally()
    ) {
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
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedSavingsGoalsRow(goals: List<SavingsGoal>) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(goals) { goal ->
            var showCard by remember { mutableStateOf(false) }

            LaunchedEffect(Unit) {
                delay(100)
                showCard = true
            }

            AnimatedVisibility(
                visible = showCard,
                enter = fadeIn() + scaleIn()
            ) {
                SavingsGoalCardEnhanced(goal = goal)
            }
        }
    }
}

@Composable
fun SavingsGoalCardEnhanced(goal: SavingsGoal) {
    val progress = (goal.currentAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(1000, easing = FastOutSlowInEasing),
        label = "goal_progress"
    )

    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable { }
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
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
                    text = goal.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = goal.icon,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(goal.color),
                trackColor = Color(goal.color).copy(alpha = 0.2f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatCurrency(goal.currentAmount),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = Color(goal.color)
                )
                Text(
                    text = formatCurrency(goal.targetAmount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${(progress * 100).toInt()}% Complete",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun AnimatedTransactionItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    var showItem by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        showItem = true
    }

    AnimatedVisibility(
        visible = showItem,
        enter = fadeIn() + slideInHorizontally()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
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
                        if (transaction.type == TransactionType.EXPENSE) Icons.Default.ShoppingBag
                        else Icons.Default.AccountBalance,
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
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDate(transaction.timestamp),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        if (transaction.note != null) {
                            Icon(
                                Icons.Outlined.Note,
                                contentDescription = "Has note",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
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
}

@Composable
fun SectionHeader(
    title: String,
    actionText: String?,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp
        )
        if (actionText != null) {
            TextButton(
                onClick = onActionClick,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun AnimatedNumber(
    value: Double,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineLarge,
    format: (Double) -> String = { formatCurrency(it) }
) {
    var animatedValue by remember { mutableStateOf(0.0) }

    LaunchedEffect(value) {
        val startValue = animatedValue
        val duration = 1000
        val startTime = System.currentTimeMillis()

        while (System.currentTimeMillis() - startTime < duration) {
            val elapsed = System.currentTimeMillis() - startTime
            val progress = (elapsed.toFloat() / duration).coerceIn(0f, 1f)
            // Manual ease-out cubic calculation
            val easedProgress = 1 - (1 - progress).let { it * it * it }
            animatedValue = startValue + (value - startValue) * easedProgress
            delay(16)
        }
        animatedValue = value
    }

    Text(
        text = format(animatedValue),
        style = textStyle,
        modifier = modifier
    )
}

data class QuickAction(
    val icon: ImageVector,
    val label: String,
    val color: Color,
    val route: String
)

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
