package com.example.savingbuddy.ui.screen.achievements

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
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
import kotlin.math.pow

data class AchievementsUiState(
    val achievements: List<Achievement> = emptyList(),
    val progressMap: Map<String, Int> = emptyMap(),
    val unlockedCount: Int = 0,
    val totalCount: Int = 0,
    val isLoading: Boolean = true,
    val recentlyUnlocked: Achievement? = null,
    val totalPoints: Int = 0
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

            // Calculate total points (each unlocked achievement gives 10 points)
            val totalPoints = unlocked * 10

            _uiState.value = AchievementsUiState(
                achievements = achievements,
                progressMap = progressMap,
                unlockedCount = unlocked,
                totalCount = achievements.size,
                isLoading = false,
                totalPoints = totalPoints
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
                "BUDGET" -> 0
                "TRANSACTION" -> 0
                "HEALTH" -> 0
                "MINDFUL" -> 0
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
    val listState = rememberLazyListState()
    var showConfetti by remember { mutableStateOf(false) }

    // Check for newly unlocked achievements
    LaunchedEffect(uiState.unlockedCount) {
        val justUnlocked = uiState.achievements.find { it.unlocked && it.unlockedAt != null &&
                it.unlockedAt!! > System.currentTimeMillis() - 5000 }
        if (justUnlocked != null) {
            showConfetti = true
            // Hide confetti after animation
            kotlinx.coroutines.delay(3000)
            showConfetti = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF667eea).copy(alpha = 0.05f),
                        Color(0xFF764ba2).copy(alpha = 0.05f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(1000f, 1000f)
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            item {
                AnimatedHeader()
            }

            // Stats Overview Card
            item {
                AnimatedStatsCard(
                    unlocked = uiState.unlockedCount,
                    total = uiState.totalCount,
                    points = uiState.totalPoints
                )
            }

            // Achievement Categories
            val groupedAchievements = uiState.achievements.groupBy { it.type }
                .toList()
                .sortedBy { (_, achievements) ->
                    achievements.count { it.unlocked }.toDouble() / achievements.size
                }
                .reversed()

            groupedAchievements.forEach { (type, achievements) ->
                item {
                    CategoryHeader(
                        type = type,
                        unlockedCount = achievements.count { it.unlocked },
                        totalCount = achievements.size
                    )
                }

                items(achievements) { achievement ->
                    AnimatedAchievementCard(
                        achievement = achievement,
                        progress = uiState.progressMap[achievement.id] ?: 0,
                        onUnlock = { viewModel.refreshProgress() }
                    )
                }
            }
        }

        // Confetti Overlay
        if (showConfetti) {
            ConfettiEffect()
        }

        // Floating Refresh Button
        FloatingActionButton(
            onClick = { viewModel.refreshProgress() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(56.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            shape = CircleShape,
            elevation = FloatingActionButtonDefaults.elevation(8.dp)
        ) {
            Icon(
                Icons.Default.Refresh,
                contentDescription = "Refresh",
                tint = Color.White
            )
        }
    }
}

@Composable
fun AnimatedHeader() {
    var showHeader by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showHeader = true
    }

    AnimatedVisibility(
        visible = showHeader,
        enter = fadeIn(animationSpec = tween(500)) +
                slideInVertically(initialOffsetY = { -100 })
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            Text(
                text = "🏆",
                fontSize = 48.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Achievements",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "Track your progress and earn rewards",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AnimatedStatsCard(unlocked: Int, total: Int, points: Int) {
    var showCard by remember { mutableStateOf(false) }
    val progress = if (total > 0) unlocked.toFloat() / total else 0f

    LaunchedEffect(Unit) {
        showCard = true
    }

    AnimatedVisibility(
        visible = showCard,
        enter = fadeIn(animationSpec = tween(600)) +
                scaleIn(initialScale = 0.8f, animationSpec = spring())
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Progress Circle and Stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Animated Circular Progress
                    val animatedProgress by animateFloatAsState(
                        targetValue = progress,
                        animationSpec = tween(
                            durationMillis = 1500,
                            easing = FastOutSlowInEasing
                        ),
                        label = "progress_animation"
                    )

                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 10.dp,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            strokeCap = StrokeCap.Round
                        )
                        AnimatedNumber(
                            value = (progress * 100).toDouble(),
                            modifier = Modifier,
                            textStyle = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            ),
                            format = { "${it.toInt()}%" }
                        )
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "$unlocked / $total",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp
                        )
                        Text(
                            text = "Achievements Unlocked",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Points",
                                modifier = Modifier.size(16.dp),
                                tint = Color(0xFFFFD700)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$points Points Earned",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFFFFD700)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Progress Message
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = getProgressMessage(unlocked, total),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
fun CategoryHeader(
    type: String,
    unlockedCount: Int,
    totalCount: Int
) {
    val categoryInfo = getCategoryInfo(type)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = categoryInfo.icon,
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = categoryInfo.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
        }

        Badge(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ) {
            Text(
                text = "$unlockedCount/$totalCount",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AnimatedAchievementCard(
    achievement: Achievement,
    progress: Int,
    onUnlock: () -> Unit
) {
    var showCard by remember { mutableStateOf(false) }
    val isUnlocked = achievement.unlocked

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(100)
        showCard = true
    }

    AnimatedVisibility(
        visible = showCard,
        enter = fadeIn(animationSpec = tween(400)) +
                slideInHorizontally(animationSpec = tween(400))
    ) {
        val animatedProgress by animateFloatAsState(
            targetValue = if (isUnlocked) 1f else (progress.toFloat() / achievement.requirement).coerceIn(0f, 1f),
            animationSpec = tween(1000, easing = FastOutSlowInEasing),
            label = "achievement_progress"
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUnlocked)
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else
                    MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = if (isUnlocked) 2.dp else 1.dp
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Achievement icon with animation
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(
                            if (isUnlocked)
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFD700),
                                        Color(0xFFFFA500)
                                    )
                                )
                            else
                                Brush.linearGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.surfaceVariant,
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    )
                                )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val scale by animateFloatAsState(
                        targetValue = if (isUnlocked) 1.1f else 1f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        )
                    )

                    Text(
                        text = if (isUnlocked) "🏆" else achievement.icon,
                        fontSize = 32.sp,
                        modifier = Modifier.scale(scale)
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
                        if (isUnlocked) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = "Unlocked",
                                tint = Color(0xFF4CAF50),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Text(
                        text = achievement.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )

                    if (!isUnlocked) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LinearProgressIndicator(
                                    progress = { animatedProgress },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$progress/${achievement.requirement}",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = getProgressHint(achievement.type, achievement.requirement - progress),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        }
                    } else if (achievement.unlockedAt != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.AccessTime,
                                contentDescription = "Unlocked date",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Unlocked ${formatDate(achievement.unlockedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Points indicator
                if (isUnlocked) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color(0xFFFFD700).copy(alpha = 0.2f),
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Points",
                                modifier = Modifier.size(14.dp),
                                tint = Color(0xFFFFD700)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "+10",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFD700)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimatedNumber(
    value: Double,
    modifier: Modifier = Modifier,
    textStyle: TextStyle,
    format: (Double) -> String
) {
    var animatedValue by remember { mutableStateOf(0.0) }

    LaunchedEffect(value) {
        animate(
            initialValue = animatedValue,
            targetValue = value,
            animationSpec = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            )
        ) { animValue, _ ->
            animatedValue = animValue
        }
    }

    Text(
        text = format(animatedValue),
        style = textStyle,
        modifier = modifier
    )
}

@Composable
fun ConfettiEffect() {
    // Simple confetti effect - in production, use a proper confetti library
    androidx.compose.animation.AnimatedVisibility(
        visible = true,
        enter = fadeIn(animationSpec = tween(500)),
        exit = fadeOut(animationSpec = tween(500))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            // Placeholder for confetti animation
            // You can implement a proper confetti effect using Canvas or a library
        }
    }
}

data class CategoryInfo(
    val title: String,
    val icon: String
)

fun getCategoryInfo(type: String): CategoryInfo {
    return when (type) {
        "JOURNAL" -> CategoryInfo("Journaling", "📝")
        "HABIT" -> CategoryInfo("Habits", "🔥")
        "FOCUS" -> CategoryInfo("Focus Sessions", "🧘")
        "SAVINGS" -> CategoryInfo("Savings", "💰")
        "BUDGET" -> CategoryInfo("Budget", "📊")
        "TRANSACTION" -> CategoryInfo("Transactions", "💳")
        "HEALTH" -> CategoryInfo("Health", "❤️")
        "MINDFUL" -> CategoryInfo("Mindfulness", "☯️")
        "TASK" -> CategoryInfo("Tasks", "✅")
        else -> CategoryInfo(type, "🏆")
    }
}

fun getProgressHint(type: String, remaining: Int): String {
    return when (type) {
        "JOURNAL" -> "$remaining more journal entries needed"
        "HABIT" -> "$remaining more days to build this habit"
        "FOCUS" -> "$remaining more focus sessions needed"
        "SAVINGS" -> "Save ${formatCurrency(remaining)} more"
        "TASK" -> "$remaining more tasks to complete"
        else -> "Keep going! You're almost there!"
    }
}

fun getProgressMessage(unlocked: Int, total: Int): String {
    val percentage = if (total > 0) (unlocked.toDouble() / total * 100).toInt() else 0
    return when {
        total == 0 -> "Start earning achievements by using the app!"
        unlocked == total -> "🎉 AMAZING! You've unlocked every achievement! You're a true champion!"
        percentage >= 75 -> "🌟 Outstanding! You're so close to completing all achievements!"
        percentage >= 50 -> "💪 Great work! You're halfway to becoming a legend!"
        percentage >= 25 -> "✨ Good start! Keep going and unlock more achievements!"
        else -> "🚀 Every achievement you unlock brings you closer to your goals!"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatCurrency(amount: Int): String {
    return String.format("৳%,d", amount)
}

private fun animate(
    initialValue: Double,
    targetValue: Double,
    animationSpec: TweenSpec<Double>,
    onValueChange: (Double, Float) -> Unit
) {
    // Animation implementation - in production, use proper animation APIs
    // This is a simplified version
    val duration = animationSpec.durationMillis
    val startTime = System.currentTimeMillis()

    // You would implement proper animation interpolation here
    // For now, we're just setting the target value
    onValueChange(targetValue, 1f)
}