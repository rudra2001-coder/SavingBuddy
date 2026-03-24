package com.example.savingbuddy.ui.screen.analytics

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
    var selectedTimeRange by remember { mutableStateOf("Monthly") }
    val timeRanges = listOf("Weekly", "Monthly", "Yearly")

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFF8F9FF),
                        Color(0xFFFFFFFF)
                    )
                )
            ),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnimatedHeader(
                selectedRange = selectedTimeRange,
                onRangeSelected = { selectedTimeRange = it },
                ranges = timeRanges
            )
        }

        item {
            AnimatedKeyMetricsRow(
                totalIncome = uiState.totalIncome,
                totalExpense = uiState.totalExpense,
                savingsRate = uiState.savingsRate,
                transactionCount = uiState.transactionCount
            )
        }

        item {
            AnimatedBalanceOverviewCard(
                totalBalance = uiState.totalBalance,
                netWorth = uiState.netWorth,
                totalAssets = uiState.totalBalance + uiState.totalSaved,
                totalLiabilities = uiState.totalDebt + uiState.creditCardBalance
            )
        }

        item {
            AnimatedIncomeExpenseChart(
                income = uiState.totalIncome,
                expense = uiState.totalExpense
            )
        }

        if (uiState.topCategories.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Top Spending Categories",
                    icon = Icons.Outlined.PieChart,
                    actionText = "View All"
                )
            }
            items(uiState.topCategories.take(5)) { category ->
                AnimatedCategorySpendingItem(category = category)
            }
        }

        if (uiState.dailySpending.any { it.amount > 0 }) {
            item {
                SectionHeader(
                    title = "Spending Pattern",
                    icon = Icons.Outlined.ShowChart,
                    actionText = null
                )
            }
            item {
                AnimatedSpendingPatternChart(dailySpending = uiState.dailySpending)
            }
        }

        if (uiState.weeklyTrend.any { it.income > 0 || it.expense > 0 }) {
            item {
                SectionHeader(
                    title = "Weekly Trend",
                    icon = Icons.Outlined.TrendingUp,
                    actionText = null
                )
            }
            item {
                AnimatedWeeklyTrendChart(weeklyData = uiState.weeklyTrend)
            }
        }

        if (uiState.budgetOverview.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Budget Status",
                    icon = Icons.Outlined.AccountBalance,
                    actionText = "${uiState.overBudgetCount} over budget",
                    actionColor = RedExpense
                )
            }
            items(uiState.budgetOverview) { budget ->
                AnimatedBudgetStatusItem(budget = budget)
            }
        }

        item {
            SectionHeader(
                title = "Life Dashboard",
                icon = Icons.Outlined.Favorite,
                actionText = null
            )
        }

        item {
            AnimatedLifeStatsRow(
                habits = uiState.activeHabits,
                tasksTotal = uiState.totalTasks,
                tasksCompleted = uiState.completedTasks,
                focusMinutes = uiState.focusMinutes,
                journalEntries = uiState.journalEntries
            )
        }

        if (uiState.accounts.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Accounts",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    actionText = "View All"
                )
            }
            items(uiState.accounts.take(5)) { account ->
                AnimatedAccountStatItem(
                    name = account.name,
                    balance = account.balance,
                    icon = account.icon
                )
            }
        }

        if (uiState.savingsGoals.isNotEmpty()) {
            item {
                SectionHeader(
                    title = "Savings Goals",
                    icon = Icons.Outlined.Savings,
                    actionText = "View All"
                )
            }
            items(uiState.savingsGoals.take(3)) { goal ->
                AnimatedSavingsGoalItem(
                    name = goal.name,
                    current = goal.currentAmount,
                    target = goal.targetAmount,
                    icon = goal.icon
                )
            }
        }

        if (uiState.totalDebt > 0 || uiState.creditCardBalance > 0) {
            item {
                SectionHeader(
                    title = "Debt Summary",
                    icon = Icons.Outlined.Warning,
                    actionText = null
                )
            }
            if (uiState.totalDebt > 0) {
                item {
                    AnimatedDebtItem(
                        type = "Loans",
                        amount = uiState.totalDebt,
                        icon = "🏦",
                        color = Color(0xFFF44336)
                    )
                }
            }
            if (uiState.creditCardBalance > 0) {
                item {
                    AnimatedDebtItem(
                        type = "Credit Cards",
                        amount = uiState.creditCardBalance,
                        icon = "💳",
                        color = Color(0xFFFF9800)
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedHeader(
    selectedRange: String,
    onRangeSelected: (String) -> Unit,
    ranges: List<String>
) {
    var showHeader by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showHeader = true
    }

    AnimatedVisibility(
        visible = showHeader,
        enter = fadeIn() + slideInVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Analytics",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.padding(4.dp)
                    ) {
                        ranges.forEach { range ->
                            FilterChip(
                                selected = selectedRange == range,
                                onClick = { onRangeSelected(range) },
                                label = {
                                    Text(
                                        range,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selectedRange == range) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Track your financial progress and insights",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AnimatedKeyMetricsRow(
    totalIncome: Double,
    totalExpense: Double,
    savingsRate: Float,
    transactionCount: Int
) {
    var showRow by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showRow = true
    }

    AnimatedVisibility(
        visible = showRow,
        enter = fadeIn() + slideInVertically()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            KeyMetricCard(
                title = "Income",
                value = formatCurrency(totalIncome),
                icon = Icons.Default.ArrowUpward,
                color = GreenIncome,
                modifier = Modifier.weight(1f)
            )
            KeyMetricCard(
                title = "Expense",
                value = formatCurrency(totalExpense),
                icon = Icons.Default.ArrowDownward,
                color = RedExpense,
                modifier = Modifier.weight(1f)
            )
            KeyMetricCard(
                title = "Savings",
                value = "${savingsRate.toInt()}%",
                icon = Icons.Default.Savings,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun KeyMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AnimatedBalanceOverviewCard(
    totalBalance: Double,
    netWorth: Double,
    totalAssets: Double,
    totalLiabilities: Double
) {
    var showCard by remember { mutableStateOf(false) }
    val isNetWorthPositive = netWorth >= 0

    LaunchedEffect(Unit) {
        showCard = true
    }

    AnimatedVisibility(
        visible = showCard,
        enter = fadeIn() + scaleIn()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isNetWorthPositive)
                    Color(0xFF4CAF50).copy(alpha = 0.1f)
                else
                    Color(0xFFF44336).copy(alpha = 0.1f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Net Worth",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = CircleShape,
                        color = if (isNetWorthPositive)
                            Color(0xFF4CAF50).copy(alpha = 0.2f)
                        else
                            Color(0xFFF44336).copy(alpha = 0.2f)
                    ) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = if (isNetWorthPositive) Color(0xFF4CAF50) else Color(0xFFF44336),
                            modifier = Modifier.padding(8.dp).size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                AnimatedNumber(
                    value = netWorth,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        color = if (isNetWorthPositive) Color(0xFF4CAF50) else Color(0xFFF44336)
                    ),
                    format = { formatCurrency(it) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Total Assets",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(totalAssets),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF4CAF50)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Total Liabilities",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatCurrency(totalLiabilities),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF44336)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedIncomeExpenseChart(income: Double, expense: Double) {
    var showChart by remember { mutableStateOf(false) }
    val total = income + expense
    val incomePercentage = if (total > 0) (income / total).toFloat() else 0.5f
    val expensePercentage = if (total > 0) (expense / total).toFloat() else 0.5f

    LaunchedEffect(Unit) {
        showChart = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Income vs Expense",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.height(20.dp))

            Box(
                modifier = Modifier
                    .size(180.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 30f
                    val radius = size.minDimension / 2 - strokeWidth / 2
                    val center = Offset(size.width / 2, size.height / 2)

                    val incomeAngle = incomePercentage * 360f
                    drawArc(
                        color = GreenIncome,
                        startAngle = -90f,
                        sweepAngle = incomeAngle,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    drawArc(
                        color = RedExpense,
                        startAngle = -90f + incomeAngle,
                        sweepAngle = expensePercentage * 360f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius, center.y - radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = formatCurrency(total),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = GreenIncome, label = "Income", value = formatCurrency(income))
                LegendItem(color = RedExpense, label = "Expense", value = formatCurrency(expense))
            }
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AnimatedCategorySpendingItem(category: CategorySpending) {
    var showItem by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showItem = true
    }

    AnimatedVisibility(
        visible = showItem,
        enter = fadeIn() + slideInHorizontally()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = category.icon,
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = category.categoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = formatCurrency(category.amount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${category.percentage.toInt()}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(category.color)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { category.percentage / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(category.color),
                    trackColor = Color(category.color).copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun AnimatedSpendingPatternChart(dailySpending: List<DailySpending>) {
    var showChart by remember { mutableStateOf(false) }
    val maxAmount = dailySpending.maxOfOrNull { it.amount } ?: 1.0

    LaunchedEffect(Unit) {
        showChart = true
    }

    AnimatedVisibility(
        visible = showChart,
        enter = fadeIn() + slideInVertically()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dailySpending.forEachIndexed { index, day ->
                        val height = (day.amount / maxAmount * 100).toFloat().coerceAtLeast(4f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            AnimatedBar(
                                height = height,
                                color = if (day.amount > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                duration = 300 + (index * 50)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = day.dayOfWeek,
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val maxDay = dailySpending.maxByOrNull { it.amount }
                    maxDay?.let {
                        Text(
                            text = "Highest: ${it.dayOfWeek}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedBar(height: Float, color: Color, duration: Int) {
    val animatedHeight by animateFloatAsState(
        targetValue = height,
        animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing),
        label = "bar_animation"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(animatedHeight.dp)
            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
            .background(color)
    )
}

@Composable
fun AnimatedWeeklyTrendChart(weeklyData: List<WeeklyData>) {
    var showChart by remember { mutableStateOf(false) }
    val maxValue = weeklyData.flatMap { listOf(it.income, it.expense) }.maxOrNull() ?: 1.0

    LaunchedEffect(Unit) {
        showChart = true
    }

    AnimatedVisibility(
        visible = showChart,
        enter = fadeIn() + slideInVertically()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Canvas(modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                ) {
                    if (weeklyData.isEmpty()) return@Canvas

                    val stepX = size.width / (weeklyData.size - 1).coerceAtLeast(1)
                    val incomePoints = mutableListOf<Offset>()
                    val expensePoints = mutableListOf<Offset>()

                    weeklyData.forEachIndexed { index, week ->
                        val x = index * stepX
                        val incomeY = size.height * (1 - (week.income / maxValue).toFloat().coerceIn(0f, 1f))
                        val expenseY = size.height * (1 - (week.expense / maxValue).toFloat().coerceIn(0f, 1f))
                        incomePoints.add(Offset(x, incomeY))
                        expensePoints.add(Offset(x, expenseY))
                    }

                    val incomePath = Path().apply {
                        moveTo(incomePoints.first().x, incomePoints.first().y)
                        incomePoints.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path = incomePath,
                        color = GreenIncome,
                        style = Stroke(width = 3f)
                    )

                    val expensePath = Path().apply {
                        moveTo(expensePoints.first().x, expensePoints.first().y)
                        expensePoints.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path = expensePath,
                        color = RedExpense,
                        style = Stroke(width = 3f)
                    )

                    incomePoints.forEach { point ->
                        drawCircle(
                            color = GreenIncome,
                            radius = 6f,
                            center = point
                        )
                    }
                    expensePoints.forEach { point ->
                        drawCircle(
                            color = RedExpense,
                            radius = 6f,
                            center = point
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
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

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    weeklyData.forEach { week ->
                        Text(
                            text = week.weekLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedBudgetStatusItem(budget: BudgetStatus) {
    var showItem by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showItem = true
    }

    AnimatedVisibility(
        visible = showItem,
        enter = fadeIn() + slideInVertically()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (budget.isOver)
                    Color(0xFFF44336).copy(alpha = 0.1f)
                else
                    MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = budget.icon, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = budget.categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${formatCurrency(budget.spent)} / ${formatCurrency(budget.limit)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (budget.isOver) RedExpense else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { (budget.percentage / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = if (budget.isOver) RedExpense else MaterialTheme.colorScheme.primary,
                    trackColor = if (budget.isOver) RedExpense.copy(alpha = 0.2f)
                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )

                if (budget.isOver) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Over budget by ${formatCurrency(budget.spent - budget.limit)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RedExpense,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedLifeStatsRow(
    habits: Int,
    tasksTotal: Int,
    tasksCompleted: Int,
    focusMinutes: Int,
    journalEntries: Int
) {
    var showRow by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showRow = true
    }

    AnimatedVisibility(
        visible = showRow,
        enter = fadeIn() + slideInVertically()
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            item { AnimatedLifeStatCard(icon = "🔥", value = "$habits", label = "Active Habits", color = Color(0xFFFF5722)) }
            item { AnimatedLifeStatCard(icon = "✅", value = "$tasksCompleted/$tasksTotal", label = "Tasks Done", color = Color(0xFF4CAF50)) }
            item { AnimatedLifeStatCard(icon = "🧘", value = "$focusMinutes min", label = "Focus Time", color = Color(0xFF2196F3)) }
            item { AnimatedLifeStatCard(icon = "📝", value = "$journalEntries", label = "Journal", color = Color(0xFF9C27B0)) }
        }
    }
}

@Composable
fun AnimatedLifeStatCard(icon: String, value: String, label: String, color: Color) {
    var showCard by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showCard = true
    }

    AnimatedVisibility(
        visible = showCard,
        enter = fadeIn() + scaleIn()
    ) {
        Card(
            modifier = Modifier.width(100.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = icon, fontSize = 32.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
fun AnimatedAccountStatItem(name: String, balance: Double, icon: String) {
    var showItem by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showItem = true
    }

    AnimatedVisibility(
        visible = showItem,
        enter = fadeIn() + slideInHorizontally()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = icon, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatCurrency(balance),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (balance >= 0) Color(0xFF4CAF50) else RedExpense
                )
            }
        }
    }
}

@Composable
fun AnimatedSavingsGoalItem(name: String, current: Double, target: Double, icon: String) {
    var showItem by remember { mutableStateOf(false) }
    val progress = if (target > 0) (current / target).toFloat() else 0f

    LaunchedEffect(Unit) {
        showItem = true
    }

    AnimatedVisibility(
        visible = showItem,
        enter = fadeIn() + slideInHorizontally()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = icon, fontSize = 24.sp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatCurrency(current),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = formatCurrency(target),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedDebtItem(type: String, amount: Double, icon: String, color: Color) {
    var showItem by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showItem = true
    }

    AnimatedVisibility(
        visible = showItem,
        enter = fadeIn() + slideInHorizontally()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = icon, fontSize = 24.sp)
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = type,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = formatCurrency(amount),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = color
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionText: String? = null,
    actionColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (actionText != null) {
            TextButton(
                onClick = { },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelSmall,
                    color = actionColor
                )
            }
        }
    }
}

@Composable
fun AnimatedNumber(
    value: Double,
    modifier: Modifier = Modifier,
    textStyle: androidx.compose.ui.text.TextStyle,
    format: (Double) -> String
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
            kotlinx.coroutines.delay(16)
        }
        animatedValue = value
    }

    Text(
        text = format(animatedValue),
        style = textStyle,
        modifier = modifier
    )
}
