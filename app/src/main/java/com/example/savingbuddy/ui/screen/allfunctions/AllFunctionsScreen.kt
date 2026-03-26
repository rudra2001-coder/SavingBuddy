package com.example.savingbuddy.ui.screen.allfunctions

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
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.savingbuddy.ui.navigation.Screen
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

data class FunctionCategory(
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val functions: List<AppFunction>
)

data class AppFunction(
    val id: String,
    val name: String,
    val description: String,
    val icon: ImageVector,
    val iconOutline: ImageVector,
    val color: Color,
    val route: String,
    val category: String
)

@HiltViewModel
class AllFunctionsViewModel @Inject constructor() : ViewModel() {
    
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()
    
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }
    
    fun selectCategory(category: String?) {
        _selectedCategory.value = category
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllFunctionsScreen(
    navController: NavHostController,
    viewModel: AllFunctionsViewModel = hiltViewModel()
) {
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    
    val categories = remember { getAllCategories() }
    val allFunctions = remember { categories.flatMap { it.functions } }
    
    val filteredFunctions = remember(searchQuery, selectedCategory) {
        allFunctions.filter { function ->
            val matchesSearch = searchQuery.isBlank() || 
                function.name.contains(searchQuery, ignoreCase = true) ||
                function.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategory == null || function.category == selectedCategory
            matchesSearch && matchesCategory
        }
    }
    
    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { 
                        Text(
                            "All Functions",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    placeholder = { Text("Search functions...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                    )
                )
                
                // Category Chips
                LazyRow(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategory == null,
                            onClick = { viewModel.selectCategory(null) },
                            label = { Text("All") },
                            leadingIcon = if (selectedCategory == null) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                    items(categories) { category ->
                        FilterChip(
                            selected = selectedCategory == category.name,
                            onClick = { viewModel.selectCategory(category.name) },
                            label = { Text(category.name) },
                            leadingIcon = if (selectedCategory == category.name) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else {
                                { Icon(category.icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            }
                        )
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (filteredFunctions.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.SearchOff,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No functions found",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                // Group by category
                val groupedFunctions = filteredFunctions.groupBy { it.category }
                
                groupedFunctions.forEach { (category, functions) ->
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val catColor = categories.find { it.name == category }?.color ?: MaterialTheme.colorScheme.primary
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(catColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${functions.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    items(functions.chunked(2)) { rowFunctions ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowFunctions.forEach { function ->
                                FunctionCard(
                                    function = function,
                                    onClick = { navController.navigate(function.route) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (rowFunctions.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FunctionCard(
    function: AppFunction,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = function.color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(function.color.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    function.icon,
                    contentDescription = null,
                    tint = function.color,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = function.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = function.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2
            )
        }
    }
}

fun getAllCategories(): List<FunctionCategory> = listOf(
    FunctionCategory(
        name = "Finance",
        icon = Icons.Default.AccountBalance,
        color = Color(0xFF2196F3),
        functions = listOf(
            AppFunction(
                id = "dashboard",
                name = "Dashboard",
                description = "Overview of your finances",
                icon = Icons.Filled.Home,
                iconOutline = Icons.Outlined.Home,
                color = Color(0xFF2196F3),
                route = Screen.Dashboard.route,
                category = "Finance"
            ),
            AppFunction(
                id = "transactions",
                name = "Transactions",
                description = "View all your transactions",
                icon = Icons.Filled.SwapHoriz,
                iconOutline = Icons.Outlined.SwapHoriz,
                color = Color(0xFF2196F3),
                route = Screen.Transactions.route,
                category = "Finance"
            ),
            AppFunction(
                id = "add_transaction",
                name = "Add Transaction",
                description = "Quickly record a transaction",
                icon = Icons.Filled.Add,
                iconOutline = Icons.Outlined.Add,
                color = Color(0xFF2196F3),
                route = Screen.AddTransaction.route,
                category = "Finance"
            ),
            AppFunction(
                id = "add_income",
                name = "Add Income",
                description = "Record your income",
                icon = Icons.Filled.TrendingUp,
                iconOutline = Icons.Outlined.TrendingUp,
                color = Color(0xFF4CAF50),
                route = Screen.AddIncome.route,
                category = "Finance"
            ),
            AppFunction(
                id = "add_expense",
                name = "Add Expense",
                description = "Record your spending",
                icon = Icons.Filled.TrendingDown,
                iconOutline = Icons.Outlined.TrendingDown,
                color = Color(0xFFF44336),
                route = Screen.AddExpense.route,
                category = "Finance"
            ),
            AppFunction(
                id = "accounts",
                name = "Accounts",
                description = "Manage your accounts",
                icon = Icons.Filled.AccountBalance,
                iconOutline = Icons.Outlined.AccountBalance,
                color = Color(0xFF9C27B0),
                route = Screen.Accounts.route,
                category = "Finance"
            ),
            AppFunction(
                id = "transfer",
                name = "Transfer",
                description = "Transfer between accounts",
                icon = Icons.Filled.SwapHorizontalCircle,
                iconOutline = Icons.Outlined.SwapHorizontalCircle,
                color = Color(0xFFFF9800),
                route = Screen.Transfer.route,
                category = "Finance"
            ),
            AppFunction(
                id = "budget",
                name = "Budget",
                description = "Set monthly budgets",
                icon = Icons.Filled.PieChart,
                iconOutline = Icons.Outlined.PieChart,
                color = Color(0xFF00BCD4),
                route = Screen.Budget.route,
                category = "Finance"
            ),
            AppFunction(
                id = "recurring",
                name = "Recurring",
                description = "Auto transactions",
                icon = Icons.Filled.Repeat,
                iconOutline = Icons.Outlined.Repeat,
                color = Color(0xFF673AB7),
                route = Screen.Recurring.route,
                category = "Finance"
            )
        )
    ),
    FunctionCategory(
        name = "Savings & Loans",
        icon = Icons.Default.Savings,
        color = Color(0xFF4CAF50),
        functions = listOf(
            AppFunction(
                id = "savings",
                name = "Savings Goals",
                description = "Track savings targets",
                icon = Icons.Filled.Savings,
                iconOutline = Icons.Outlined.Savings,
                color = Color(0xFF4CAF50),
                route = Screen.Savings.route,
                category = "Savings & Loans"
            ),
            AppFunction(
                id = "add_saving",
                name = "Add Savings Goal",
                description = "Create new goal",
                icon = Icons.Filled.AddCircle,
                iconOutline = Icons.Outlined.AddCircle,
                color = Color(0xFF8BC34A),
                route = Screen.AddSaving.route,
                category = "Savings & Loans"
            ),
            AppFunction(
                id = "loan",
                name = "Loans & Debts",
                description = "Track your loans",
                icon = Icons.Filled.AccountBalance,
                iconOutline = Icons.Outlined.AccountBalance,
                color = Color(0xFFF44336),
                route = Screen.Loan.route,
                category = "Savings & Loans"
            ),
            AppFunction(
                id = "credit_card",
                name = "Credit Cards",
                description = "Manage credit cards",
                icon = Icons.Filled.CreditCard,
                iconOutline = Icons.Outlined.CreditCard,
                color = Color(0xFFFF5722),
                route = Screen.CreditCard.route,
                category = "Savings & Loans"
            )
        )
    ),
    FunctionCategory(
        name = "Life",
        icon = Icons.Default.Favorite,
        color = Color(0xFFE91E63),
        functions = listOf(
            AppFunction(
                id = "work_calendar",
                name = "Work Calendar",
                description = "Track work days & office/home",
                icon = Icons.Filled.CalendarMonth,
                iconOutline = Icons.Outlined.CalendarMonth,
                color = Color(0xFF3F51B5),
                route = Screen.WorkCalendar.route,
                category = "Life"
            ),
            AppFunction(
                id = "work_report",
                name = "Work Reports",
                description = "Analyze work logs & trends",
                icon = Icons.Filled.Assessment,
                iconOutline = Icons.Outlined.Assessment,
                color = Color(0xFF673AB7),
                route = Screen.WorkReport.route,
                category = "Life"
            ),
            AppFunction(
                id = "life",
                name = "Life Dashboard",
                description = "Health, habits & more",
                icon = Icons.Filled.Favorite,
                iconOutline = Icons.Outlined.FavoriteBorder,
                color = Color(0xFFE91E63),
                route = Screen.Life.route,
                category = "Life"
            ),
            AppFunction(
                id = "achievements",
                name = "Achievements",
                description = "Your accomplishments",
                icon = Icons.Filled.EmojiEvents,
                iconOutline = Icons.Outlined.EmojiEvents,
                color = Color(0xFFFFD700),
                route = Screen.Achievements.route,
                category = "Life"
            )
        )
    ),
    FunctionCategory(
        name = "Insights",
        icon = Icons.Default.Insights,
        color = Color(0xFF607D8B),
        functions = listOf(
            AppFunction(
                id = "analytics",
                name = "Analytics",
                description = "Comprehensive data analysis",
                icon = Icons.Filled.Analytics,
                iconOutline = Icons.Outlined.Analytics,
                color = Color(0xFF3F51B5),
                route = Screen.Analytics.route,
                category = "Insights"
            ),
            AppFunction(
                id = "insights",
                name = "Spending Insights",
                description = "Analyze your patterns",
                icon = Icons.Filled.Insights,
                iconOutline = Icons.Outlined.Insights,
                color = Color(0xFF607D8B),
                route = Screen.Insights.route,
                category = "Insights"
            ),
            AppFunction(
                id = "net_worth",
                name = "Net Worth",
                description = "Track your assets & debts",
                icon = Icons.Filled.Assessment,
                iconOutline = Icons.Outlined.Assessment,
                color = Color(0xFF3F51B5),
                route = Screen.NetWorth.route,
                category = "Insights"
            ),
            AppFunction(
                id = "health_score",
                name = "Financial Health",
                description = "Check your financial health",
                icon = Icons.Filled.Favorite,
                iconOutline = Icons.Outlined.FavoriteBorder,
                color = Color(0xFFE91E63),
                route = Screen.HealthScore.route,
                category = "Insights"
            ),
            AppFunction(
                id = "export",
                name = "Export Data",
                description = "Export CSV or PDF",
                icon = Icons.Default.Download,
                iconOutline = Icons.Outlined.Download,
                color = Color(0xFF4CAF50),
                route = Screen.Export.route,
                category = "Insights"
            )
        )
    ),
    FunctionCategory(
        name = "System",
        icon = Icons.Default.Settings,
        color = Color(0xFF607D8B),
        functions = listOf(
            AppFunction(
                id = "settings",
                name = "Settings",
                description = "App preferences & backup",
                icon = Icons.Filled.Settings,
                iconOutline = Icons.Outlined.Settings,
                color = Color(0xFF607D8B),
                route = Screen.Settings.route,
                category = "System"
            ),
            AppFunction(
                id = "about",
                name = "About",
                description = "App information & version",
                icon = Icons.Filled.Info,
                iconOutline = Icons.Outlined.Info,
                color = Color(0xFF607D8B),
                route = Screen.About.route,
                category = "System"
            )
        )
    )
)
