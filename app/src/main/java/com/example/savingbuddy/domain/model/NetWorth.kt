package com.example.savingbuddy.domain.model

data class NetWorth(
    val totalAssets: Double = 0.0,
    val totalLiabilities: Double = 0.0,
    val netWorth: Double = 0.0,
    val assets: List<Asset> = emptyList(),
    val liabilities: List<Liability> = emptyList(),
    val lastUpdated: Long = System.currentTimeMillis()
)

data class Asset(
    val id: String,
    val name: String,
    val type: AssetType,
    val value: Double,
    val icon: String
)

enum class AssetType {
    ACCOUNT,
    SAVINGS_GOAL,
    INVESTMENT,
    PROPERTY,
    OTHER
}

data class Liability(
    val id: String,
    val name: String,
    val type: LiabilityType,
    val amount: Double,
    val icon: String
)

enum class LiabilityType {
    LOAN,
    CREDIT_CARD,
    MORTGAGE,
    OTHER
}

data class NetWorthTrend(
    val date: Long,
    val netWorth: Double,
    val assets: Double,
    val liabilities: Double
)