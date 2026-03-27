package com.example.savingbuddy.ui.screen.insights

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.model.Category
import com.example.savingbuddy.domain.model.InsightType
import com.example.savingbuddy.domain.model.SpendingInsight
import com.example.savingbuddy.domain.model.Transaction
import com.example.savingbuddy.domain.model.TransactionType
import com.example.savingbuddy.domain.repository.CategoryRepository
import com.example.savingbuddy.domain.repository.TransactionRepository
import com.example.savingbuddy.domain.usecase.GetSpendingInsightsUseCase
import com.example.savingbuddy.ui.screen.dashboard.formatCurrency
import com.example.savingbuddy.ui.theme.GreenIncome
import com.example.savingbuddy.ui.theme.RedExpense
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class InsightsUiState(
    val isLoading: Boolean = true,
    val insights: List<SpendingInsight> = emptyList(),
    val currentPeriodExpenses: Double = 0.0,
    val previousPeriodExpenses: Double = 0.0,
    val categorySpending: Map<String, Double> = emptyMap(),
    val categories: Map<String, Category> = emptyMap(),
    val dailySpending: List<DailySpending> = emptyList(),
    val topCategories: List<CategorySpending> = emptyList(),
    val selectedPeriod: TimePeriod = TimePeriod.THIS_MONTH
)

enum class TimePeriod(val displayName: String, val days: Int) {
    THIS_WEEK("This Week", 7),
    THIS_MONTH("This Month", 30),
    LAST_MONTH("Last Month", 30),
    THIS_YEAR("This Year", 365)
}

data class DailySpending(
    val dayLabel: String,
    val amount: Double
)

data class CategorySpending(
    val categoryId: String,
    val categoryName: String,
    val icon: String,
    val color: Long,
    val amount: Double,
    val percentage: Float
)

@HiltViewModel
class InsightsViewModel @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val getSpendingInsightsUseCase: GetSpendingInsightsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(InsightsUiState())
    val uiState: StateFlow<InsightsUiState> = _uiState.asStateFlow()

    init {
        loadInsights()
    }

    fun loadInsights() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            val period = _uiState.value.selectedPeriod
            val (currentStart, currentEnd) = getDateRange(period)
            val (previousStart, previousEnd) = getPreviousDateRange(period)

            val transactions = transactionRepository.getTransactionsByDateRange(currentStart, currentEnd).first()
            val previousTransactions = transactionRepository.getTransactionsByDateRange(previousStart, previousEnd).first()
            val categories = categoryRepository.getAllCategories().first().associateBy { it.id }

            val currentExpenses = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }
            val previousExpenses = previousTransactions
                .filter { it.type == TransactionType.EXPENSE }
                .sumOf { it.amount }

            val categorySpending = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.categoryId }
                .mapValues { (_, txns) -> txns.sumOf { it.amount } }

            val totalExpense = categorySpending.values.sum()
            val topCategories = categorySpending
                .map { (catId, amount) ->
                    val category = categories[catId]
                    CategorySpending(
                        categoryId = catId,
                        categoryName = category?.name ?: "Unknown",
                        icon = category?.icon ?: "💰",
                        color = category?.color ?: 0xFF9E9E9E,
                        amount = amount,
                        percentage = if (totalExpense > 0) (amount / totalExpense * 100).toFloat() else 0f
                    )
                }
                .sortedByDescending { it.amount }
                .take(6)

            val dailySpending = calculateDailySpending(transactions, period)

            val categoryNames = categories.mapValues { it.value.name }
            val insights = getSpendingInsightsUseCase.analyzeSpending(
                currentPeriodTransactions = transactions,
                previousPeriodTransactions = previousTransactions,
                categoryNames = categoryNames
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                insights = insights,
                currentPeriodExpenses = currentExpenses,
                previousPeriodExpenses = previousExpenses,
                categorySpending = categorySpending,
                categories = categories,
                dailySpending = dailySpending,
                topCategories = topCategories
            )
        }
    }

    fun selectPeriod(period: TimePeriod) {
        _uiState.value = _uiState.value.copy(selectedPeriod = period)
        loadInsights()
    }

    private fun getDateRange(period: TimePeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val end = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -period.days)
        val start = calendar.timeInMillis
        return start to end
    }

    private fun getPreviousDateRange(period: TimePeriod): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -period.days)
        val end = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -period.days)
        val start = calendar.timeInMillis
        return start to end
    }

    private fun calculateDailySpending(transactions: List<Transaction>, period: TimePeriod): List<DailySpending> {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_WEEK)
        
        val expensesByDay = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .groupBy { txn ->
                val cal = Calendar.getInstance().apply { timeInMillis = txn.timestamp }
                cal.get(Calendar.DAY_OF_WEEK)
            }
            .mapValues { (_, txns) -> txns.sumOf { it.amount } }

        val dayLabels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        return dayLabels.mapIndexed { index, label ->
            val dayOfWeek = if (index == 0) 7 else index
            DailySpending(
                dayLabel = label,
                amount = expensesByDay[dayOfWeek] ?: 0.0
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    viewModel: InsightsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Spending Insights", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
            ) {
                item {
                    TimePeriodSelector(
                        selectedPeriod = uiState.selectedPeriod,
                        onSelectPeriod = { viewModel.selectPeriod(it) }
                    )
                }

                item {
                    SpendingOverviewCard(
                        currentExpenses = uiState.currentPeriodExpenses,
                        previousExpenses = uiState.previousPeriodExpenses
                    )
                }

                if (uiState.insights.isNotEmpty()) {
                    item {
                        Text(
                            text = "Smart Insights",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(uiState.insights) { insight ->
                        InsightCard(insight = insight)
                    }
                }

                if (uiState.topCategories.isNotEmpty()) {
                    item {
                        Text(
                            text = "Top Categories",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    item {
                        CategoryBreakdownChart(categories = uiState.topCategories)
                    }
                }

                if (uiState.dailySpending.isNotEmpty()) {
                    item {
                        Text(
                            text = "Weekly Pattern",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    item {
                        WeeklySpendingChart(dailySpending = uiState.dailySpending)
                    }
                }

                if (uiState.insights.isEmpty() && uiState.topCategories.isEmpty()) {
                    item {
                        EmptyInsightsCard()
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@Composable
fun TimePeriodSelector(
    selectedPeriod: TimePeriod,
    onSelectPeriod: (TimePeriod) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(TimePeriod.entries.toList()) { period ->
            FilterChip(
                selected = selectedPeriod == period,
                onClick = { onSelectPeriod(period) },
                label = { Text(period.displayName) }
            )
        }
    }
}

@Composable
fun SpendingOverviewCard(
    currentExpenses: Double,
    previousExpenses: Double
) {
    val change = if (previousExpenses > 0) {
        ((currentExpenses - previousExpenses) / previousExpenses * 100)
    } else 0.0
    val isIncrease = change > 0

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
                text = "Total Spending",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = formatCurrency(currentExpenses),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (previousExpenses > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (isIncrease) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (isIncrease) RedExpense else GreenIncome,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${String.format("%.1f", kotlin.math.abs(change))}% vs last period",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isIncrease) RedExpense else GreenIncome
                    )
                }
            }
        }
    }
}

@Composable
fun InsightCard(insight: SpendingInsight) {
    val (icon, color) = when (insight.type) {
        InsightType.SPENDING_INCREASE -> Icons.Default.TrendingUp to RedExpense
        InsightType.SPENDING_DECREASE -> Icons.Default.TrendingDown to GreenIncome
        InsightType.CATEGORY_DOMINANCE -> Icons.Default.PieChart to Color(0xFFFF9800)
        InsightType.WEEKEND_SPENDING -> Icons.Default.CalendarMonth to Color(0xFF9C27B0)
        InsightType.DAILY_AVERAGE -> Icons.Default.Today to Color(0xFF2196F3)
        InsightType.BUDGET_WARNING -> Icons.Default.Warning to Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = color)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = insight.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CategoryBreakdownChart(categories: List<CategorySpending>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val total = categories.sumOf { it.amount }
                
                Box(
                    modifier = Modifier.size(120.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        categories.forEach { cat ->
                            val sweepAngle = (cat.amount / total * 360).toFloat()
                            drawArc(
                                color = Color(cat.color),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true,
                                topLeft = Offset(0f, 0f),
                                size = Size(size.width, size.height)
                            )
                            startAngle += sweepAngle
                        }
                    }
                    
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${categories.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Categories",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    categories.take(5).forEach { cat ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .background(Color(cat.color), CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${cat.icon} ${cat.categoryName}",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${cat.percentage.toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            categories.forEach { cat ->
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = cat.icon, fontSize = 16.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cat.categoryName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Text(
                            text = formatCurrency(cat.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { cat.percentage / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(cat.color),
                        trackColor = Color(cat.color).copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
fun WeeklySpendingChart(dailySpending: List<DailySpending>) {
    val maxAmount = dailySpending.maxOfOrNull { it.amount } ?: 1.0

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                dailySpending.forEach { day ->
                    val height = if (maxAmount > 0) (day.amount / maxAmount * 80).toFloat().coerceAtLeast(4f) else 4f
                    val isHigh = day.amount > maxAmount * 0.7

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (day.amount > 0) {
                            Text(
                                text = formatCurrency(day.amount),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 8.sp,
                                color = if (isHigh) RedExpense else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                        Box(
                            modifier = Modifier
                                .width(24.dp)
                                .height(height.dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(
                                    if (day.amount > 0) 
                                        if (isHigh) RedExpense.copy(alpha = 0.8f) 
                                        else MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                dailySpending.forEach { day ->
                    Text(
                        text = day.dayLabel,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyInsightsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Insights,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "No insights yet",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "Add some expenses to see insights",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
