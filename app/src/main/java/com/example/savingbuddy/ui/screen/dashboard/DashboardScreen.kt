package com.example.savingbuddy.ui.screen.dashboard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

private val PremiumDeepBlue = Color(0xFF1E3A8A)
private val PremiumViolet = Color(0xFF7C3AED)
private val PremiumLightBackground = Color(0xFFF7F9FC)
private val PremiumDarkBackground = Color(0xFF0F0F14)
private val PremiumDarkCard = Color(0xFF1A1A24)
private val PremiumCardLight = Color(0xFFFFFFFF)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavHostController,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val isSystemDark = isSystemInDarkTheme()

    LaunchedEffect(Unit) {
        viewModel.initializeDefaultData()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddTransaction.route) },
                containerColor = PremiumViolet,
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
        },
        floatingActionButtonPosition = FabPosition.End
    ) { paddingValues ->
        val backgroundColor = if (isSystemDark) PremiumDarkBackground else PremiumLightBackground

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    GreetingHeader(
                        totalBalance = uiState.totalBalance,
                        monthlyIncome = uiState.monthlySummary.totalIncome,
                        monthlyExpense = uiState.monthlySummary.totalExpense
                    )
                }

                item {
                    HeroBalanceCard(
                        balance = uiState.totalBalance,
                        income = uiState.monthlySummary.totalIncome,
                        expense = uiState.monthlySummary.totalExpense
                    )
                }

                item {
                    QuickActionsGrid(navController = navController)
                }

                uiState.healthScore?.let { score ->
                    item {
                        HealthScoreCard(
                            score = score,
                            onClick = { navController.navigate(Screen.Health.route) }
                        )
                    }
                }

                if (uiState.budgetStatus.totalBudget > 0) {
                    item {
                        BudgetOverviewCard(
                            budgetStatus = uiState.budgetStatus,
                            onClick = { navController.navigate(Screen.Budget.route) }
                        )
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
                    items(uiState.recentTransactions.take(5)) { transaction ->
                        TransactionItem(
                            transaction = transaction,
                            onClick = { }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun isSystemInDarkTheme(): Boolean {
    return androidx.compose.foundation.isSystemInDarkTheme()
}

@Composable
fun GreetingHeader(
    totalBalance: Double,
    monthlyIncome: Double,
    monthlyExpense: Double
) {
    val greeting = getGreeting()
    val netChange = monthlyIncome - monthlyExpense
    val isPositive = netChange >= 0
    val isSystemDark = isSystemInDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSystemDark) Color.White else Color(0xFF1E293B)
                )
                Text(
                    text = SimpleDateFormat("EEEE, MMM dd", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isSystemDark) Color.White.copy(alpha = 0.6f) else Color(0xFF64748B)
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = PremiumViolet.copy(alpha = 0.1f)
            ) {
                Text(
                    text = SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = PremiumViolet,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = if (isPositive) GreenIncome else RedExpense,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isPositive) "+${formatCurrency(netChange)} this month"
                else "-${formatCurrency(-netChange)} this month",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = if (isPositive) GreenIncome else RedExpense
            )
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun HeroBalanceCard(balance: Double, income: Double, expense: Double) {
    var hideBalance by remember { mutableStateOf(false) }
    val isSystemDark = isSystemInDarkTheme()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .shadow(12.dp, RoundedCornerShape(24.dp), spotColor = PremiumViolet.copy(alpha = 0.3f))
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = if (isSystemDark) {
                        listOf(PremiumDeepBlue, PremiumViolet)
                    } else {
                        listOf(PremiumDeepBlue.copy(alpha = 0.95f), PremiumViolet.copy(alpha = 0.9f))
                    },
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
            .drawBehind {
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.15f), Color.Transparent),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height / 2)
                    )
                )
            }
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
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
                IconButton(
                    onClick = { hideBalance = !hideBalance },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (hideBalance) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = "Toggle Balance",
                        tint = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            AnimatedContent(
                targetState = hideBalance,
                transitionSpec = { fadeIn() with fadeOut() }
            ) { hidden ->
                Text(
                    text = if (hidden) "••••••••" else formatCurrency(balance),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 36.sp,
                        letterSpacing = (-0.5).sp
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ArrowDownward,
                    label = "Income",
                    amount = income,
                    iconTint = Color(0xFF81C784),
                    isHidden = hideBalance
                )
                StatPill(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Default.ArrowUpward,
                    label = "Expense",
                    amount = expense,
                    iconTint = Color(0xFFE57373),
                    isHidden = hideBalance
                )
            }
        }
    }
}

@Composable
private fun StatPill(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    amount: Double,
    iconTint: Color,
    isHidden: Boolean
) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.12f),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.15f)
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isHidden) "••••" else formatCurrency(amount),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    }
}

@Composable
fun QuickActionsGrid(navController: NavHostController) {
    val primaryActions = listOf(
        QuickActionItem(Icons.Default.Add, "Expense", Screen.AddExpense.route),
        QuickActionItem(Icons.Default.TrendingUp, "Income", Screen.AddIncome.route),
        QuickActionItem(Icons.Default.SwapHoriz, "Transfer", Screen.Transfer.route),
        QuickActionItem(Icons.Default.Savings, "Savings", Screen.Savings.route)
    )

    val secondaryActions = listOf(
        QuickActionItem(Icons.Default.Receipt, "History", Screen.Transactions.route),
        QuickActionItem(Icons.Default.Analytics, "Analytics", Screen.Analytics.route),
        QuickActionItem(Icons.Default.HealthAndSafety, "Health", Screen.Health.route),
        QuickActionItem(Icons.Default.BarChart, "Budget", Screen.Budget.route)
    )

    val isSystemDark = isSystemInDarkTheme()
    val cardColor = if (isSystemDark) PremiumDarkCard else PremiumCardLight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            primaryActions.forEach { action ->
                QuickActionItemCard(
                    modifier = Modifier.weight(1f),
                    action = action,
                    isPrimary = true,
                    cardColor = cardColor,
                    navController = navController
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            secondaryActions.forEach { action ->
                QuickActionItemCard(
                    modifier = Modifier.weight(1f),
                    action = action,
                    isPrimary = false,
                    cardColor = cardColor,
                    navController = navController
                )
            }
        }
    }
}

@Composable
private fun QuickActionItemCard(
    modifier: Modifier = Modifier,
    action: QuickActionItem,
    isPrimary: Boolean,
    cardColor: Color,
    navController: NavHostController
) {
    var isPressed by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    Surface(
        modifier = modifier
            .scale(scale)
            .aspectRatio(1f)
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) {
                isPressed = true
                navController.navigate(action.route)
            },
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        shadowElevation = if (isPrimary) 2.dp else 1.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            val iconColor = when (action.label) {
                "Expense" -> RedExpense
                "Income" -> GreenIncome
                "Transfer" -> PremiumViolet
                "Savings" -> GreenIncome
                else -> if (isPrimary) PremiumViolet else Color(0xFF64748B)
            }

            Icon(
                action.icon,
                contentDescription = action.label,
                tint = iconColor,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = action.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isPrimary) Color(0xFF1E293B) else Color(0xFF64748B),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            delay(100)
            isPressed = false
        }
    }
}

@Composable
fun HealthScoreCard(
    score: FinancialHealthScore,
    onClick: () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val cardColor = if (isSystemDark) PremiumDarkCard else PremiumCardLight

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick)
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val animatedScore by animateFloatAsState(
                targetValue = score.overallScore / 100f,
                animationSpec = tween(1200, easing = FastOutSlowInEasing),
                label = "health_score"
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(64.dp)
            ) {
                CircularProgressIndicator(
                    progress = { animatedScore },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 6.dp,
                    trackColor = Color.Gray.copy(alpha = 0.2f),
                    color = when {
                        score.overallScore >= 80 -> Color(0xFF4CAF50)
                        score.overallScore >= 60 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    },
                    strokeCap = StrokeCap.Round
                )

                Text(
                    text = "${score.overallScore}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Financial Health",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = score.grade,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        score.overallScore >= 80 -> Color(0xFF4CAF50)
                        score.overallScore >= 60 -> Color(0xFFFF9800)
                        else -> Color(0xFFF44336)
                    }
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = score.recommendations.firstOrNull() ?: "Keep it up!",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View Details",
                tint = Color.Gray.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
fun BudgetOverviewCard(
    budgetStatus: BudgetStatus,
    onClick: () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val cardColor = if (isSystemDark) PremiumDarkCard else PremiumCardLight
    val percentage = if (budgetStatus.totalBudget > 0) {
        (budgetStatus.spent / budgetStatus.totalBudget).toFloat()
    } else 0f

    val progressColor = when {
        percentage < 0.7f -> Color(0xFF4CAF50)
        percentage < 0.9f -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .clickable(onClick = onClick)
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
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
                    text = "Budget",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "${(percentage * 100).toInt()}% used",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = progressColor
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            val animatedProgress by animateFloatAsState(
                targetValue = percentage.coerceIn(0f, 1f),
                animationSpec = tween(800, easing = FastOutSlowInEasing),
                label = "budget_progress"
            )

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = progressColor,
                trackColor = progressColor.copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BudgetStatItem(label = "Spent", value = budgetStatus.spent, color = progressColor)
                BudgetStatItem(label = "Remaining", value = budgetStatus.remaining, color = Color(0xFF4CAF50))
                BudgetStatItem(label = "Total", value = budgetStatus.totalBudget, color = Color(0xFF64748B))
            }

            if (percentage > 0.9f) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF44336).copy(alpha = 0.1f)
                ) {
                    Text(
                        text = "Budget nearly exhausted!",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFF44336),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetStatItem(label: String, value: Double, color: Color) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.Gray.copy(alpha = 0.7f)
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
fun TransactionItem(
    transaction: Transaction,
    onClick: () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val cardColor = if (isSystemDark) PremiumDarkCard else PremiumCardLight

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clickable(onClick = onClick)
            .shadow(1.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor)
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
                    modifier = Modifier.size(18.dp)
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
                    color = Color.Gray.copy(alpha = 0.6f)
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

@Composable
fun SectionHeader(
    title: String,
    actionText: String?,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
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
                    color = PremiumViolet
                )
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = PremiumViolet
                )
            }
        }
    }
}

data class QuickActionItem(
    val icon: ImageVector,
    val label: String,
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
