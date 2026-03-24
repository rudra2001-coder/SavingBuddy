package com.example.savingbuddy.ui.screen.health

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.model.*
import com.example.savingbuddy.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject

data class HealthUiState(
    val todayHealth: HealthEntry? = null,
    val journalEntries: List<JournalEntry> = emptyList(),
    val habits: List<Habit> = emptyList(),
    val todayTasks: List<Task> = emptyList(),
    val achievements: List<Achievement> = emptyList(),
    val todayFocusMinutes: Int = 0,
    val todayMindfulSessions: Int = 0,
    val showAddTaskDialog: Boolean = false,
    val showAddHabitDialog: Boolean = false,
    val showJournalDialog: Boolean = false,
    val showSleepDialog: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class HealthViewModel @Inject constructor(
    private val healthRepository: HealthRepository,
    private val journalRepository: JournalRepository,
    private val habitRepository: HabitRepository,
    private val achievementRepository: AchievementRepository,
    private val focusSessionRepository: FocusSessionRepository,
    private val taskRepository: TaskRepository,
    private val mindfulSessionRepository: MindfulSessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(HealthUiState())
    val uiState: StateFlow<HealthUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        val (todayStart, todayEnd) = getTodayRange()
        
        viewModelScope.launch {
            healthRepository.getTodayEntry(todayStart).collect { entry ->
                if (entry == null) {
                    val newEntry = HealthEntry(
                        id = UUID.randomUUID().toString(),
                        date = todayStart,
                        sleepHours = 0f,
                        sleepQuality = 3,
                        waterIntake = 0f,
                        steps = null,
                        calories = null,
                        screenTime = null,
                        workHours = 0f,
                        breakTime = 0f,
                        mood = 3,
                        stressLevel = 3,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    healthRepository.saveEntry(newEntry)
                    _uiState.value = _uiState.value.copy(todayHealth = newEntry)
                } else {
                    _uiState.value = _uiState.value.copy(todayHealth = entry)
                }
            }
        }

        viewModelScope.launch {
            journalRepository.getAllEntries().collect { entries ->
                _uiState.value = _uiState.value.copy(journalEntries = entries.take(20))
            }
        }

        viewModelScope.launch {
            habitRepository.getActiveHabits().collect { habits ->
                _uiState.value = _uiState.value.copy(habits = habits)
            }
        }

        viewModelScope.launch {
            taskRepository.getTodayTasks(todayStart).collect { tasks ->
                _uiState.value = _uiState.value.copy(todayTasks = tasks)
            }
        }

        viewModelScope.launch {
            achievementRepository.getAllAchievements().collect { achievements ->
                _uiState.value = _uiState.value.copy(achievements = achievements)
            }
        }

        viewModelScope.launch {
            focusSessionRepository.getTotalMinutesForDateRange(todayStart, todayEnd).collect { minutes ->
                _uiState.value = _uiState.value.copy(todayFocusMinutes = minutes)
            }
        }

        viewModelScope.launch {
            mindfulSessionRepository.getCompletedSessionsCount(todayStart, todayEnd).collect { count ->
                _uiState.value = _uiState.value.copy(todayMindfulSessions = count, isLoading = false)
            }
        }

        viewModelScope.launch {
            achievementRepository.initializeAchievements()
        }
    }

    private fun getTodayRange(): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val start = calendar.timeInMillis
        
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        val end = calendar.timeInMillis
        
        return start to end
    }

    fun showAddTaskDialog() { _uiState.value = _uiState.value.copy(showAddTaskDialog = true) }
    fun hideAddTaskDialog() { _uiState.value = _uiState.value.copy(showAddTaskDialog = false) }
    fun showAddHabitDialog() { _uiState.value = _uiState.value.copy(showAddHabitDialog = true) }
    fun hideAddHabitDialog() { _uiState.value = _uiState.value.copy(showAddHabitDialog = false) }
    fun showJournalDialog() { _uiState.value = _uiState.value.copy(showJournalDialog = true) }
    fun hideJournalDialog() { _uiState.value = _uiState.value.copy(showJournalDialog = false) }
    fun showSleepDialog() { _uiState.value = _uiState.value.copy(showSleepDialog = true) }
    fun hideSleepDialog() { _uiState.value = _uiState.value.copy(showSleepDialog = false) }

    fun addTask(title: String, priority: String) {
        val (todayStart, _) = getTodayRange()
        viewModelScope.launch {
            val task = Task(
                id = UUID.randomUUID().toString(),
                title = title,
                date = todayStart,
                priority = priority,
                completed = false,
                category = null,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            taskRepository.saveTask(task)
            hideAddTaskDialog()
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            taskRepository.setTaskCompleted(task.id, !task.completed)
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            taskRepository.deleteTask(task)
        }
    }

    fun addHabit(name: String, icon: String, color: Long) {
        viewModelScope.launch {
            val habit = Habit(
                id = UUID.randomUUID().toString(),
                name = name,
                type = "DAILY",
                target = 1,
                currentStreak = 0,
                longestStreak = 0,
                icon = icon,
                color = color,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            habitRepository.saveHabit(habit)
            hideAddHabitDialog()
        }
    }

    fun toggleHabit(habit: Habit) {
        val (todayStart, _) = getTodayRange()
        viewModelScope.launch {
            val isCompleted = habit.currentStreak > 0
            if (!isCompleted) {
                habitRepository.logHabitCompletion(habit.id, todayStart)
                habitRepository.updateStreak(habit.id, habit.currentStreak + 1)
                checkAchievements()
            } else {
                habitRepository.removeHabitCompletion(habit.id, todayStart)
                habitRepository.updateStreak(habit.id, 0)
            }
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            habitRepository.deleteHabit(habit)
        }
    }

    fun saveJournalEntry(title: String?, content: String, mood: Int, tags: List<String>) {
        viewModelScope.launch {
            val entry = JournalEntry(
                id = UUID.randomUUID().toString(),
                date = System.currentTimeMillis(),
                mood = mood,
                title = title,
                content = content,
                tags = tags,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            journalRepository.saveEntry(entry)
            hideJournalDialog()
            checkAchievements()
        }
    }

    fun addWater(amount: Float) {
        val current = _uiState.value.todayHealth ?: return
        viewModelScope.launch {
            val updated = current.copy(
                waterIntake = current.waterIntake + amount,
                updatedAt = System.currentTimeMillis()
            )
            healthRepository.saveEntry(updated)
        }
    }

    fun updateHealthEntry(entry: HealthEntry) {
        viewModelScope.launch {
            healthRepository.saveEntry(entry.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun startFocusSession(durationMinutes: Int) {
        viewModelScope.launch {
            val session = FocusSession(
                id = UUID.randomUUID().toString(),
                duration = durationMinutes,
                completed = false,
                date = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            focusSessionRepository.saveSession(session)
            checkAchievements()
        }
    }

    fun startMindfulSession(type: MindfulType) {
        viewModelScope.launch {
            val session = MindfulSession(
                id = UUID.randomUUID().toString(),
                type = type.name,
                durationSeconds = type.durationSeconds,
                completed = true,
                date = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            mindfulSessionRepository.saveSession(session)
        }
    }

    fun checkAchievements() {
        viewModelScope.launch {
            val journalCount = journalRepository.getAllEntries().first().size
            val habits = habitRepository.getActiveHabits().first()
            val maxStreak = habits.maxOfOrNull { it.currentStreak } ?: 0
            val focusSessions = focusSessionRepository.getSessionsForDateRange(
                getTodayRange().first, getTodayRange().second
            ).first().size

            if (journalCount >= 1) achievementRepository.unlockAchievement("first_entry")
            if (journalCount >= 10) achievementRepository.unlockAchievement("journal_10")
            if (journalCount >= 30) achievementRepository.unlockAchievement("journal_30")
            if (maxStreak >= 7) achievementRepository.unlockAchievement("habit_7")
            if (maxStreak >= 30) achievementRepository.unlockAchievement("habit_30")
            if (focusSessions >= 10) achievementRepository.unlockAchievement("focus_10")
            if (focusSessions >= 50) achievementRepository.unlockAchievement("focus_50")
        }
    }
}
