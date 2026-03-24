package com.example.savingbuddy.ui.screen.health

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
import androidx.navigation.NavHostController
import com.example.savingbuddy.domain.model.*
import com.example.savingbuddy.ui.theme.GreenIncome
import com.example.savingbuddy.ui.theme.RedExpense
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    navController: NavHostController,
    viewModel: HealthViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Life Dashboard") },
                actions = {
                    IconButton(onClick = { viewModel.checkAchievements() }) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = "Achievements")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Today") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Journal") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Habits") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 }, text = { Text("Tasks") })
            }

            when (selectedTab) {
                0 -> TodayTab(uiState, viewModel)
                1 -> JournalTab(uiState, viewModel)
                2 -> HabitsTab(uiState, viewModel)
                3 -> TasksTab(uiState, viewModel)
            }
        }
    }

    if (uiState.showAddTaskDialog) {
        AddTaskDialog(
            onDismiss = { viewModel.hideAddTaskDialog() },
            onSave = { title, priority -> viewModel.addTask(title, priority) }
        )
    }

    if (uiState.showAddHabitDialog) {
        AddHabitDialog(
            onDismiss = { viewModel.hideAddHabitDialog() },
            onSave = { name, icon, color -> viewModel.addHabit(name, icon, color) }
        )
    }

    if (uiState.showJournalDialog) {
        JournalEntryDialog(
            onDismiss = { viewModel.hideJournalDialog() },
            onSave = { title, content, mood, tags -> viewModel.saveJournalEntry(title, content, mood, tags) }
        )
    }
}

@Composable
fun TodayTab(uiState: HealthUiState, viewModel: HealthViewModel) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            HealthScoreCard(healthScore = uiState.todayHealth?.healthScore ?: 0)
        }

        item {
            QuickActionsRow(uiState, viewModel)
        }

        item {
            Text("Daily Input", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }

        item {
            DailyInputCard(uiState, viewModel)
        }

        item {
            FocusAndMindfulRow(uiState, viewModel)
        }

        item {
            AchievementsPreview(achievements = uiState.achievements)
        }
    }
}

@Composable
fun HealthScoreCard(healthScore: Int) {
    val scoreColor = when {
        healthScore >= 80 -> Color(0xFF4CAF50)
        healthScore >= 60 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Health Score",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = "$healthScore/100",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    color = scoreColor
                )
            }
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(scoreColor.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when {
                        healthScore >= 80 -> Icons.Default.Favorite
                        healthScore >= 60 -> Icons.Default.FavoriteBorder
                        else -> Icons.Default.HeartBroken
                    },
                    contentDescription = null,
                    tint = scoreColor,
                    modifier = Modifier.size(40.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionsRow(uiState: HealthUiState, viewModel: HealthViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickActionButton(
            icon = Icons.Default.Bedtime,
            label = "Sleep",
            onClick = { viewModel.showSleepDialog() },
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.WaterDrop,
            label = "Water",
            onClick = { viewModel.addWater(0.25f) },
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.SelfImprovement,
            label = "Mindful",
            onClick = { viewModel.startMindfulSession(MindfulType.BREATHING) },
            modifier = Modifier.weight(1f)
        )
        QuickActionButton(
            icon = Icons.Default.Timer,
            label = "Focus",
            onClick = { viewModel.startFocusSession(25) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun DailyInputCard(uiState: HealthUiState, viewModel: HealthViewModel) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Sleep", style = MaterialTheme.typography.bodySmall)
                    Text("${uiState.todayHealth?.sleepHours?.toString() ?: "0"}h", fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Water", style = MaterialTheme.typography.bodySmall)
                    Text("${uiState.todayHealth?.waterIntake?.toString() ?: "0"}L", fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Mood", style = MaterialTheme.typography.bodySmall)
                    Text(
                        uiState.todayHealth?.let { Mood.entries.getOrNull(it.mood - 1)?.emoji } ?: "😐",
                        fontSize = 20.sp
                    )
                }
                Column {
                    Text("Stress", style = MaterialTheme.typography.bodySmall)
                    Text("${uiState.todayHealth?.stressLevel ?: 3}/5", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FocusAndMindfulRow(uiState: HealthUiState, viewModel: HealthViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1976D2).copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Watch,
                    contentDescription = "Focus time",
                    tint = Color(0xFF1976D2)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Focus Today", style = MaterialTheme.typography.labelSmall)
                    Text("${uiState.todayFocusMinutes} min", fontWeight = FontWeight.Bold)
                }
            }
        }
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF9C27B0).copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.Spa,
                    contentDescription = "Mindful session",
                    tint = Color(0xFF9C27B0)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Mindful", style = MaterialTheme.typography.labelSmall)
                    Text("${uiState.todayMindfulSessions} sessions", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AchievementsPreview(achievements: List<Achievement>) {
    val unlocked = achievements.filter { it.unlocked }
    if (unlocked.isNotEmpty()) {
        Column {
            Text("Recent Achievements", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(unlocked.take(5)) { achievement ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = achievement.icon,
                            modifier = Modifier.padding(8.dp),
                            fontSize = 24.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JournalTab(uiState: HealthUiState, viewModel: HealthViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Journal", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { viewModel.showJournalDialog() }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Entry")
            }
        }

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.journalEntries) { entry ->
                JournalEntryCard(entry = entry)
            }
        }
    }
}

@Composable
fun JournalEntryCard(entry: JournalEntry) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = Mood.entries.getOrNull(entry.mood - 1)?.emoji ?: "😐",
                    fontSize = 24.sp
                )
                Text(
                    text = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(entry.date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (entry.title != null) {
                Text(entry.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(entry.content, maxLines = 3, style = MaterialTheme.typography.bodyMedium)
            if (entry.tags.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(entry.tags) { tag ->
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "#$tag",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HabitsTab(uiState: HealthUiState, viewModel: HealthViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Habits", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { viewModel.showAddHabitDialog() }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Habit")
            }
        }

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(uiState.habits) { habit ->
                HabitCard(
                    habit = habit,
                    onToggle = { viewModel.toggleHabit(habit) },
                    onDelete = { viewModel.deleteHabit(habit) }
                )
            }
        }
    }
}

@Composable
fun HabitCard(habit: Habit, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(habit.color).copy(alpha = 0.2f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(habit.icon, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(habit.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "${habit.currentStreak} day streak 🔥",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (habit.currentStreak > 0) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row {
                IconButton(onClick = onToggle) {
                    Icon(
                        if (habit.currentStreak > 0) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Toggle",
                        tint = if (habit.currentStreak > 0) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun TasksTab(uiState: HealthUiState, viewModel: HealthViewModel) {
    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Today's Tasks", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Button(onClick = { viewModel.showAddTaskDialog() }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Task")
            }
        }

        LazyColumn(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(uiState.todayTasks) { task ->
                TaskCard(
                    task = task,
                    onToggle = { viewModel.toggleTask(task) },
                    onDelete = { viewModel.deleteTask(task) }
                )
            }
        }
    }
}

@Composable
fun TaskCard(task: Task, onToggle: () -> Unit, onDelete: () -> Unit) {
    val priorityColor = when (task.priority) {
        "HIGH" -> Color(0xFFF44336)
        "MEDIUM" -> Color(0xFFFF9800)
        else -> Color(0xFF4CAF50)
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = task.completed, onCheckedChange = { onToggle() })
                Column {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = priorityColor.copy(alpha = 0.1f)
                    ) {
                        Text(
                            text = task.priority,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor
                        )
                    }
                }
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
fun AddTaskDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("MEDIUM") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LOW", "MEDIUM", "HIGH").forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, priority) }, enabled = title.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AddHabitDialog(onDismiss: () -> Unit, onSave: (String, String, Long) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedIcon by remember { mutableStateOf("✓") }

    val icons = listOf("✓", "💪", "📚", "🏃", "💧", "🧘", "✍️", "🍎", "😴", "🎯")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Choose Icon", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(icons) { icon ->
                        FilterChip(
                            selected = selectedIcon == icon,
                            onClick = { selectedIcon = icon },
                            label = { Text(icon, fontSize = 20.sp) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, selectedIcon, 0xFF4CAF50) }, enabled = name.isNotBlank()) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun JournalEntryDialog(onDismiss: () -> Unit, onSave: (String?, String, Int, List<String>) -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var mood by remember { mutableStateOf(3) }
    var tags by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Journal Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("How are you feeling?", style = MaterialTheme.typography.labelSmall)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(Mood.entries.size) { index ->
                        val m = Mood.entries[index]
                        FilterChip(
                            selected = mood == index + 1,
                            onClick = { mood = index + 1 },
                            label = { Text(m.emoji, fontSize = 24.sp) }
                        )
                    }
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text("What's on your mind?") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        title.ifBlank { null },
                        content,
                        mood,
                        tags.split(",").map { it.trim() }.filter { it.isNotBlank() }
                    )
                },
                enabled = content.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
