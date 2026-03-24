package com.example.savingbuddy.ui.screen.achievements

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.model.Achievement
import com.example.savingbuddy.domain.repository.AchievementRepository
import com.example.savingbuddy.domain.repository.FocusSessionRepository
import com.example.savingbuddy.domain.repository.HabitRepository
import com.example.savingbuddy.domain.repository.JournalRepository
import com.example.savingbuddy.domain.repository.SavingsGoalRepository
import com.example.savingbuddy.domain.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class AchievementsUiState(
    val achievements: List<Achievement> = emptyList(),
    val progressMap: Map<String, Int> = emptyMap(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = true
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementRepository: AchievementRepository,
    private val journalRepository: JournalRepository,
    private val habitRepository: HabitRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val savingsGoalRepository: SavingsGoalRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AchievementsUiState())
    val uiState: StateFlow<AchievementsUiState> = _uiState.asStateFlow()

    init {
        loadAchievements()
    }

    private fun loadAchievements() {
        viewModelScope.launch {
            val achievements = achievementRepository.getAllAchievements().first()
            val progressMap = calculateProgress(achievements)
            val unlocked = achievements.count { it.unlocked }
            
            _uiState.value = AchievementsUiState(
                achievements = achievements,
                progressMap = progressMap,
                unlockedCount = unlocked,
                totalCount = achievements.size,
                isLoading = false
            )
        }
    }

    private suspend fun calculateProgress(achievements: List<Achievement>): Map<String, Int> {
        val map = mutableMapOf<String, Int>()
        
        achievements.forEach { achievement ->
            val progress = when (achievement.type) {
                "JOURNAL" -> journalRepository.getAllEntries().first().size
                "HABIT" -> {
                    val habits = habitRepository.getAllHabits().first()
                    habits.maxOfOrNull { it.currentStreak } ?: 0
                }
                "FOCUS" -> {
                    val calendar = Calendar.getInstance()
                    val startOfMonth = calendar.apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                    }.timeInMillis
                    val endOfMonth = calendar.apply {
                        set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                        set(Calendar.HOUR_OF_DAY, 23)
                    }.timeInMillis
                    focusSessionRepository.getCompletedSessionsCount(startOfMonth, endOfMonth).first()
                }
                "SAVINGS" -> savingsGoalRepository.getAllSavingsGoals().first()
                    .sumOf { it.currentAmount }.toInt()
                "TASK" -> {
                    val today = Calendar.getInstance()
                    today.set(Calendar.HOUR_OF_DAY, 0)
                    today.set(Calendar.MINUTE, 0)
                    taskRepository.getTodayTasks(today.timeInMillis).first()
                        .count { it.completed }
                }
                "BUDGET" -> 0 // Would need budget adherence tracking
                "TRANSACTION" -> 0 // Would need transaction count
                "HEALTH" -> 0 // Would need health entry tracking
                "MINDFUL" -> 0 // Would need mindful session tracking
                else -> 0
            }
            map[achievement.id] = progress
        }
        
        return map
    }

    fun refreshProgress() {
        loadAchievements()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    viewModel: AchievementsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Achievements",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            AchievementProgressCard(
                unlocked = uiState.unlockedCount,
                total = uiState.totalCount
            )
        }

        // Group by type
        val groupedAchievements = uiState.achievements.groupBy { it.type }
        
        groupedAchievements.forEach { (type, achievements) ->
            item {
                Text(
                    text = getTypeTitle(type),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            
            items(achievements) { achievement ->
                AchievementCard(
                    achievement = achievement,
                    progress = uiState.progressMap[achievement.id] ?: 0
                )
            }
        }
    }
}

@Composable
fun AchievementProgressCard(unlocked: Int, total: Int) {
    val progress = if (total > 0) unlocked.toFloat() / total else 0f
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 8.dp,
                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column {
                Text(
                    text = "$unlocked / $total Unlocked",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = getProgressMessage(unlocked, total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun AchievementCard(achievement: Achievement, progress: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = if (achievement.unlocked) 1f else (progress.toFloat() / achievement.requirement).coerceIn(0f, 1f),
        animationSpec = tween(800),
        label = "achievement_progress"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.unlocked) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Achievement icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(
                        if (achievement.unlocked) 
                            Color(0xFFFFD700).copy(alpha = 0.2f)
                        else 
                            MaterialTheme.colorScheme.surfaceVariant,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (achievement.unlocked) "🏆" else achievement.icon,
                    fontSize = 28.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = achievement.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (achievement.unlocked) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Unlocked",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (!achievement.unlocked) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier
                                .weight(1f)
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "$progress/${achievement.requirement}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else if (achievement.unlockedAt != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Unlocked ${formatDate(achievement.unlockedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}

private fun getTypeTitle(type: String): String {
    return when (type) {
        "JOURNAL" -> "📝 Journal"
        "HABIT" -> "🔥 Habits"
        "FOCUS" -> "🧘 Focus Sessions"
        "SAVINGS" -> "💰 Savings"
        "BUDGET" -> "📊 Budget"
        "TRANSACTION" -> "💳 Transactions"
        "HEALTH" -> "❤️ Health"
        "MINDFUL" -> "☯️ Mindfulness"
        "TASK" -> "✅ Tasks"
        else -> type
    }
}

private fun getProgressMessage(unlocked: Int, total: Int): String {
    return when {
        total == 0 -> "No achievements yet"
        unlocked == total -> "🎉 All achievements unlocked!"
        unlocked >= total * 0.75 -> "Almost there! Keep going!"
        unlocked >= total * 0.5 -> "Great progress! You're halfway there!"
        unlocked >= total * 0.25 -> "Good start! Keep building habits!"
        else -> "Start your achievement journey!"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}