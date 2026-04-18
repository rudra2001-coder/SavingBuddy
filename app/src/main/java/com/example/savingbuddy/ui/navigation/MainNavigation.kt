package com.example.savingbuddy.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.savingbuddy.ui.screen.dashboard.DashboardScreen
import com.example.savingbuddy.ui.screen.dashboard.AccountsScreen
import com.example.savingbuddy.ui.screen.dashboard.BudgetScreen
import com.example.savingbuddy.ui.screen.loan.LoanScreen
import com.example.savingbuddy.ui.screen.savings.AddSavingScreen
import com.example.savingbuddy.ui.screen.savings.SavingsScreen
import com.example.savingbuddy.ui.screen.creditcard.CreditCardScreen
import com.example.savingbuddy.ui.screen.recurring.RecurringScreen
import com.example.savingbuddy.ui.screen.health.HealthScreen
import com.example.savingbuddy.ui.screen.allfunctions.AllFunctionsScreen
import com.example.savingbuddy.ui.screen.settings.SettingsScreen
import com.example.savingbuddy.ui.screen.settings.AboutScreen
import com.example.savingbuddy.ui.screen.expense.AddExpenseScreen
import com.example.savingbuddy.ui.screen.income.AddIncomeScreen
import com.example.savingbuddy.ui.screen.transactions.AddTransactionScreen
import com.example.savingbuddy.ui.screen.transactions.TransactionsScreen
import com.example.savingbuddy.ui.screen.transfer.TransferScreen
import com.example.savingbuddy.ui.screen.networth.NetWorthScreen
import com.example.savingbuddy.ui.screen.export.ExportScreen
import com.example.savingbuddy.ui.screen.healthscore.HealthScoreScreen
import com.example.savingbuddy.ui.screen.achievements.AchievementsScreen
import com.example.savingbuddy.ui.screen.analytics.AnalyticsScreen
import com.example.savingbuddy.ui.screen.workcalendar.WorkCalendarScreen
import com.example.savingbuddy.ui.screen.workreport.WorkReportScreen
import com.example.savingbuddy.ui.screen.backup.BackupScreen
import com.example.savingbuddy.ui.screen.addsavings.AddSavingsScreen
import com.example.savingbuddy.ui.screen.insights.InsightsScreen

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard.route, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Life.route, "Life", Icons.Filled.Favorite, Icons.Outlined.FavoriteBorder),
    BottomNavItem(Screen.AllFunctions.route, "All", Icons.Filled.Apps, Icons.Outlined.Apps)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = currentDestination?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        },

    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(navController = navController) }
            composable(Screen.Transactions.route) { TransactionsScreen(navController = navController) }
            composable(Screen.AddTransaction.route) { AddTransactionScreen(navController = navController) }
            composable(Screen.AddIncome.route) { AddIncomeScreen(navController = navController) }
            composable(Screen.AddExpense.route) { AddExpenseScreen(navController = navController) }
            composable(Screen.AddSaving.route) { AddSavingScreen(navController = navController) }
            composable(Screen.Savings.route) { SavingsScreen(navController = navController) }
            composable(Screen.Transfer.route) { TransferScreen(navController = navController) }
            composable(Screen.Accounts.route) { AccountsScreen(navController = navController) }
            composable(Screen.Budget.route) { BudgetScreen(navController = navController) }
            composable(Screen.Loan.route) { LoanScreen(navController = navController) }
            composable(Screen.CreditCard.route) { CreditCardScreen(navController = navController) }
            composable(Screen.Recurring.route) { RecurringScreen(navController = navController) }
            composable(Screen.Life.route) { HealthScreen(navController = navController) }
            composable(Screen.Health.route) { HealthScreen(navController = navController) }
            composable(Screen.AllFunctions.route) { AllFunctionsScreen(navController = navController) }
            composable(Screen.Settings.route) { SettingsScreen(navController = navController) }
            composable(Screen.About.route) { AboutScreen(navController = navController) }
            composable(Screen.NetWorth.route) { NetWorthScreen() }
            composable(Screen.Export.route) { ExportScreen() }
            composable(Screen.HealthScore.route) { HealthScoreScreen() }
            composable(Screen.Achievements.route) { AchievementsScreen() }
            composable(Screen.Analytics.route) { AnalyticsScreen() }
            composable(Screen.WorkCalendar.route) { WorkCalendarScreen() }
            composable(Screen.WorkReport.route) { WorkReportScreen() }
            composable(Screen.Backup.route) { BackupScreen() }
            composable(Screen.AddSavings.route) { AddSavingsScreen(onNavigateBack = { navController.popBackStack() }) }
            composable(Screen.Insights.route) { InsightsScreen() }
        }
    }
}


