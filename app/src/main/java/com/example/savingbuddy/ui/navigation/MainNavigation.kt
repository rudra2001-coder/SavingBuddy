package com.example.savingbuddy.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.savingbuddy.ui.screen.dashboard.DashboardScreen
import com.example.savingbuddy.ui.screen.savings.SavingsScreen
import com.example.savingbuddy.ui.screen.transactions.AddTransactionScreen
import com.example.savingbuddy.ui.screen.transactions.TransactionsScreen
import com.example.savingbuddy.ui.screen.transfer.TransferScreen

data class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Dashboard.route, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavItem(Screen.Transactions.route, "Transactions", Icons.Filled.SwapHoriz, Icons.Outlined.SwapHoriz),
    BottomNavItem(Screen.Savings.route, "Savings", Icons.Filled.Savings, Icons.Outlined.Savings),
    BottomNavItem(Screen.Insights.route, "Insights", Icons.Filled.Insights, Icons.Outlined.Insights)
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
        floatingActionButton = {
            if (showBottomBar) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddTransaction.route) },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Transaction")
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Dashboard.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Dashboard.route) { DashboardScreen(navController = navController) }
            composable(Screen.Transactions.route) { TransactionsScreen(navController = navController) }
            composable(Screen.AddTransaction.route) { AddTransactionScreen(navController = navController) }
            composable(Screen.Savings.route) { SavingsScreen(navController = navController) }
            composable(Screen.Transfer.route) { TransferScreen(navController = navController) }
            composable(Screen.Insights.route) { InsightsScreen(navController = navController) }
        }
    }
}

@Composable
fun InsightsScreen(navController: NavHostController) {
    Text("Insights Screen - Coming Soon")
}