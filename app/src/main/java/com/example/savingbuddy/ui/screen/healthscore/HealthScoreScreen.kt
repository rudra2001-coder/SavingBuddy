package com.example.savingbuddy.ui.screen.healthscore

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.model.UserPreferences
import com.example.savingbuddy.domain.repository.UserPreferencesRepository
import com.example.savingbuddy.domain.usecase.CalculateFinancialHealthScore
import com.example.savingbuddy.domain.usecase.FinancialHealthScore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HealthScoreUiState(
    val healthScore: FinancialHealthScore? = null,
    val monthlyIncome: Double = 0.0,
    val isLoading: Boolean = true
)

@HiltViewModel
class HealthScoreViewModel @Inject constructor(
    private val calculateHealthScore: CalculateFinancialHealthScore,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(HealthScoreUiState())
    val uiState: StateFlow<HealthScoreUiState> = _uiState.asStateFlow()

    init {
        loadHealthScore()
    }

    private fun loadHealthScore() {
        viewModelScope.launch {
            val prefs = preferencesRepository.getPreferences().first()
            val income = prefs?.monthlyIncome ?: 50000.0
            val score = calculateHealthScore.calculate(income)
            _uiState.value = HealthScoreUiState(
                healthScore = score,
                monthlyIncome = income,
                isLoading = false
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScoreScreen(
    viewModel: HealthScoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            Text(
                text = "Financial Health Score",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        uiState.healthScore?.let { score ->
            item {
                OverallScoreCard(
                    score = score.overallScore,
                    grade = score.grade
                )
            }

            item {
                Text(
                    text = "Score Breakdown",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            item {
                ScoreItem(
                    title = "Savings Rate",
                    score = score.savingsRateScore,
                    description = getSavingsRateDescription(score.savingsRateScore),
                    icon = Icons.Default.Savings
                )
            }

            item {
                ScoreItem(
                    title = "Budget Adherence",
                    score = score.budgetAdherenceScore,
                    description = getBudgetDescription(score.budgetAdherenceScore),
                    icon = Icons.Default.PieChart
                )
            }

            item {
                ScoreItem(
                    title = "Debt Management",
                    score = score.debtScore,
                    description = getDebtDescription(score.debtScore),
                    icon = Icons.Default.CreditCard
                )
            }

            item {
                ScoreItem(
                    title = "Emergency Fund",
                    score = score.emergencyFundScore,
                    description = getEmergencyFundDescription(score.emergencyFundScore),
                    icon = Icons.Default.Security
                )
            }

            item {
                HorizontalDivider()
            }

            item {
                Text(
                    text = "Recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            score.recommendations.forEach { recommendation ->
                item {
                    RecommendationCard(recommendation = recommendation)
                }
            }
        }
    }
}

@Composable
fun OverallScoreCard(score: Int, grade: String) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(1000),
        label = "progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = getScoreColor(score).copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.fillMaxSize(),
                    color = getScoreColor(score),
                    strokeWidth = 12.dp,
                    trackColor = getScoreColor(score).copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = grade,
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = getScoreColor(score)
                    )
                    Text(
                        text = "$score/100",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = getScoreMessage(score),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun ScoreItem(
    title: String,
    score: Int,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    val animatedProgress by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(800),
        label = "score_progress"
    )

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
                    .size(48.dp)
                    .background(getScoreColor(score).copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = getScoreColor(score),
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier.size(48.dp),
                    color = getScoreColor(score),
                    strokeWidth = 4.dp,
                    trackColor = getScoreColor(score).copy(alpha = 0.2f)
                )
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun RecommendationCard(recommendation: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = recommendation,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

private fun getScoreColor(score: Int): Color {
    return when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFFFFC107)
        score >= 40 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }
}

private fun getScoreMessage(score: Int): String {
    return when {
        score >= 90 -> "Excellent! Your finances are thriving!"
        score >= 80 -> "Great job! Keep up the excellent work"
        score >= 70 -> "Good progress. Minor improvements possible"
        score >= 60 -> "Fair. Room for improvement"
        score >= 50 -> "Needs attention. Focus on key areas"
        else -> "Action required. Let's improve together"
    }
}

private fun getSavingsRateDescription(score: Int): String {
    return when {
        score >= 80 -> "Strong savings habit"
        score >= 60 -> "Moderate savings rate"
        else -> "Needs improvement"
    }
}

private fun getBudgetDescription(score: Int): String {
    return when {
        score >= 80 -> "Sticking to budgets well"
        score >= 60 -> "Some budget overruns"
        else -> "Overspending in categories"
    }
}

private fun getDebtDescription(score: Int): String {
    return when {
        score >= 80 -> "Minimal debt, healthy ratio"
        score >= 60 -> "Manageable debt levels"
        else -> "High debt burden"
    }
}

private fun getEmergencyFundDescription(score: Int): String {
    return when {
        score >= 80 -> "Well-funded emergency reserve"
        score >= 60 -> "Some emergency savings"
        else -> "Build emergency fund"
    }
}