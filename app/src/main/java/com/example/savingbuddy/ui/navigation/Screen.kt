package com.example.savingbuddy.ui.navigation

sealed class Screen(val route: String) {
    data object Dashboard : Screen("dashboard")
    data object Transactions : Screen("transactions")
    data object AddTransaction : Screen("add_transaction")
    data object Savings : Screen("savings")
    data object Transfer : Screen("transfer")
    data object Insights : Screen("insights")
    data object Accounts : Screen("accounts")
}