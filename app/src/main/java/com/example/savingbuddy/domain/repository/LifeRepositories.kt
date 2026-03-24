package com.example.savingbuddy.domain.repository

import com.example.savingbuddy.domain.model.JournalEntry
import kotlinx.coroutines.flow.Flow

interface JournalRepository {
    fun getAllEntries(): Flow<List<JournalEntry>>
    fun getEntriesByDateRange(startDate: Long, endDate: Long): Flow<List<JournalEntry>>
    suspend fun getEntryById(id: String): JournalEntry?
    suspend fun saveEntry(entry: JournalEntry)
    suspend fun deleteEntry(entry: JournalEntry)
}

interface HabitRepository {
    fun getActiveHabits(): Flow<List<com.example.savingbuddy.domain.model.Habit>>
    fun getAllHabits(): Flow<List<com.example.savingbuddy.domain.model.Habit>>
    suspend fun getHabitById(id: String): com.example.savingbuddy.domain.model.Habit?
    fun getHabitLogs(habitId: String): Flow<List<com.example.savingbuddy.domain.model.HabitLog>>
    fun getLogsForDateRange(startDate: Long, endDate: Long): Flow<List<com.example.savingbuddy.domain.model.HabitLog>>
    suspend fun saveHabit(habit: com.example.savingbuddy.domain.model.Habit)
    suspend fun deleteHabit(habit: com.example.savingbuddy.domain.model.Habit)
    suspend fun logHabitCompletion(habitId: String, date: Long)
    suspend fun removeHabitCompletion(habitId: String, date: Long)
    suspend fun updateStreak(habitId: String, newStreak: Int)
}

interface AchievementRepository {
    fun getAllAchievements(): Flow<List<com.example.savingbuddy.domain.model.Achievement>>
    fun getUnlockedAchievements(): Flow<List<com.example.savingbuddy.domain.model.Achievement>>
    suspend fun getAchievementById(id: String): com.example.savingbuddy.domain.model.Achievement?
    suspend fun unlockAchievement(id: String)
    suspend fun initializeAchievements()
}

interface FocusSessionRepository {
    fun getSessionsForDateRange(startDate: Long, endDate: Long): Flow<List<com.example.savingbuddy.domain.model.FocusSession>>
    fun getTotalMinutesForDateRange(startDate: Long, endDate: Long): Flow<Int>
    fun getCompletedSessionsCount(startDate: Long, endDate: Long): Flow<Int>
    suspend fun saveSession(session: com.example.savingbuddy.domain.model.FocusSession)
}

interface TaskRepository {
    fun getTasksForDateRange(startDate: Long, endDate: Long): Flow<List<com.example.savingbuddy.domain.model.Task>>
    fun getTodayTasks(todayStart: Long): Flow<List<com.example.savingbuddy.domain.model.Task>>
    suspend fun getTaskById(id: String): com.example.savingbuddy.domain.model.Task?
    suspend fun saveTask(task: com.example.savingbuddy.domain.model.Task)
    suspend fun deleteTask(task: com.example.savingbuddy.domain.model.Task)
    suspend fun setTaskCompleted(id: String, completed: Boolean)
}

interface MindfulSessionRepository {
    fun getSessionsForDateRange(startDate: Long, endDate: Long): Flow<List<com.example.savingbuddy.domain.model.MindfulSession>>
    fun getCompletedSessionsCount(startDate: Long, endDate: Long): Flow<Int>
    suspend fun saveSession(session: com.example.savingbuddy.domain.model.MindfulSession)
}
