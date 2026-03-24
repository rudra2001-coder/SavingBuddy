package com.example.savingbuddy.data.repository

import com.example.savingbuddy.data.local.dao.*
import com.example.savingbuddy.data.local.entity.AchievementEntity
import com.example.savingbuddy.data.local.entity.HabitLogEntity
import com.example.savingbuddy.data.local.entity.HealthEntryEntity
import com.example.savingbuddy.data.local.entity.JournalEntryEntity
import com.example.savingbuddy.data.local.entity.HabitEntity
import com.example.savingbuddy.data.local.entity.FocusSessionEntity
import com.example.savingbuddy.data.local.entity.TaskEntity
import com.example.savingbuddy.data.local.entity.MindfulSessionEntity
import com.example.savingbuddy.data.local.entity.WorkLogEntity
import com.example.savingbuddy.data.local.mapper.toDomain
import com.example.savingbuddy.data.local.mapper.toEntity
import com.example.savingbuddy.domain.model.*
import com.example.savingbuddy.domain.repository.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class HealthRepositoryImpl @Inject constructor(
    private val healthDao: HealthDao
) : HealthRepository {
    override fun getHealthEntries(startDate: Long, endDate: Long): Flow<List<HealthEntry>> =
        healthDao.getHealthEntries(startDate, endDate).map { list -> list.map { it.toDomain() } }

    override fun getTodayEntry(todayStart: Long): Flow<HealthEntry?> =
        healthDao.getTodayEntry(todayStart).map { it?.toDomain() }

    override suspend fun getEntryById(id: String): HealthEntry? =
        healthDao.getEntryById(id)?.toDomain()

    override suspend fun saveEntry(entry: HealthEntry) {
        healthDao.insertEntry(entry.toEntity())
    }

    override suspend fun deleteEntry(entry: HealthEntry) {
        healthDao.deleteEntry(entry.toEntity())
    }
}

class JournalRepositoryImpl @Inject constructor(
    private val journalDao: JournalDao
) : JournalRepository {
    override fun getAllEntries(): Flow<List<JournalEntry>> =
        journalDao.getAllEntries().map { list -> list.map { it.toDomain() } }

    override fun getEntriesByDateRange(startDate: Long, endDate: Long): Flow<List<JournalEntry>> =
        journalDao.getEntriesByDateRange(startDate, endDate).map { list -> list.map { it.toDomain() } }

    override suspend fun getEntryById(id: String): JournalEntry? =
        journalDao.getEntryById(id)?.toDomain()

    override suspend fun saveEntry(entry: JournalEntry) {
        journalDao.insertEntry(entry.toEntity())
    }

    override suspend fun deleteEntry(entry: JournalEntry) {
        journalDao.deleteEntry(entry.toEntity())
    }
}

class HabitRepositoryImpl @Inject constructor(
    private val habitDao: HabitDao
) : HabitRepository {
    override fun getActiveHabits(): Flow<List<Habit>> =
        habitDao.getActiveHabits().map { list -> list.map { it.toDomain() } }

    override fun getAllHabits(): Flow<List<Habit>> =
        habitDao.getAllHabits().map { list -> list.map { it.toDomain() } }

    override suspend fun getHabitById(id: String): Habit? =
        habitDao.getHabitById(id)?.toDomain()

    override fun getHabitLogs(habitId: String): Flow<List<HabitLog>> =
        habitDao.getHabitLogs(habitId).map { list -> list.map { it.toDomain() } }

    override fun getLogsForDateRange(startDate: Long, endDate: Long): Flow<List<HabitLog>> =
        habitDao.getLogsForDateRange(startDate, endDate).map { list -> list.map { it.toDomain() } }

    override suspend fun saveHabit(habit: Habit) {
        habitDao.insertHabit(habit.toEntity())
    }

    override suspend fun deleteHabit(habit: Habit) {
        habitDao.deleteHabit(habit.toEntity())
    }

    override suspend fun logHabitCompletion(habitId: String, date: Long) {
        habitDao.insertLog(HabitLogEntity(habitId, date, true))
    }

    override suspend fun removeHabitCompletion(habitId: String, date: Long) {
        habitDao.deleteLog(habitId, date)
    }

    override suspend fun updateStreak(habitId: String, newStreak: Int) {
        habitDao.getHabitById(habitId)?.let { habit ->
            val updated = habit.copy(
                currentStreak = newStreak,
                longestStreak = maxOf(habit.longestStreak, newStreak),
                updatedAt = System.currentTimeMillis()
            )
            habitDao.updateHabit(updated)
        }
    }
}

class AchievementRepositoryImpl @Inject constructor(
    private val achievementDao: AchievementDao
) : AchievementRepository {
    override fun getAllAchievements(): Flow<List<Achievement>> =
        achievementDao.getAllAchievements().map { list -> list.map { it.toDomain() } }

    override fun getUnlockedAchievements(): Flow<List<Achievement>> =
        achievementDao.getUnlockedAchievements().map { list -> list.map { it.toDomain() } }

    override suspend fun getAchievementById(id: String): Achievement? =
        achievementDao.getAchievementById(id)?.toDomain()

    override suspend fun unlockAchievement(id: String) {
        achievementDao.unlockAchievement(id, System.currentTimeMillis())
    }

    override suspend fun initializeAchievements() {
        val defaultAchievements = listOf(
            // Journal achievements
            AchievementEntity("first_entry", "First Step", "Write your first journal entry", "📝", 1, "JOURNAL", false, null),
            AchievementEntity("journal_5", "Regular Writer", "Write 5 journal entries", "📓", 5, "JOURNAL", false, null),
            AchievementEntity("journal_10", "Reflective Mind", "Write 10 journal entries", "🧠", 10, "JOURNAL", false, null),
            AchievementEntity("journal_30", "Deep Reflection", "Write 30 journal entries", "📖", 30, "JOURNAL", false, null),
            AchievementEntity("journal_100", "Journal Master", "Write 100 journal entries", "🎖️", 100, "JOURNAL", false, null),
            
            // Habit achievements
            AchievementEntity("habit_3", "Getting Started", "Maintain a 3-day habit streak", "🌱", 3, "HABIT", false, null),
            AchievementEntity("habit_7", "Week Warrior", "Maintain a 7-day habit streak", "🔥", 7, "HABIT", false, null),
            AchievementEntity("habit_14", "Two Week Champion", "Maintain a 14-day habit streak", "⭐", 14, "HABIT", false, null),
            AchievementEntity("habit_30", "Consistent", "Maintain a 30-day habit streak", "💪", 30, "HABIT", false, null),
            AchievementEntity("habit_100", "Habit Legend", "Maintain a 100-day habit streak", "🏆", 100, "HABIT", false, null),
            
            // Focus session achievements
            AchievementEntity("focus_1", "First Focus", "Complete your first focus session", "🎯", 1, "FOCUS", false, null),
            AchievementEntity("focus_10", "Focused Mind", "Complete 10 focus sessions", "🧘", 10, "FOCUS", false, null),
            AchievementEntity("focus_50", "Deep Worker", "Complete 50 focus sessions", "⏱️", 50, "FOCUS", false, null),
            AchievementEntity("focus_100", "Concentration Master", "Complete 100 focus sessions", "🧠", 100, "FOCUS", false, null),
            
            // Savings achievements
            AchievementEntity("save_first", "First Saver", "Save your first amount", "💰", 1, "SAVINGS", false, null),
            AchievementEntity("save_1000", "Getting Started", "Save 1,000 total", "🌟", 1000, "SAVINGS", false, null),
            AchievementEntity("save_10000", "Money Saver", "Save 10,000 total", "💵", 10000, "SAVINGS", false, null),
            AchievementEntity("save_50000", "Savings Pro", "Save 50,000 total", "💎", 50000, "SAVINGS", false, null),
            AchievementEntity("save_goal", "Goal Getter", "Complete your first savings goal", "🎯", 1, "SAVINGS", false, null),
            
            // Budget achievements
            AchievementEntity("budget_7", "Budget Master", "Stay under budget for 7 days", "📊", 7, "BUDGET", false, null),
            AchievementEntity("budget_30", "Budget Pro", "Stay under budget for 30 days", "📈", 30, "BUDGET", false, null),
            
            // Transaction achievements
            AchievementEntity("transaction_10", "Active User", "Log 10 transactions", "📝", 10, "TRANSACTION", false, null),
            AchievementEntity("transaction_50", "Transaction Pro", "Log 50 transactions", "💳", 50, "TRANSACTION", false, null),
            AchievementEntity("transaction_100", "Record Keeper", "Log 100 transactions", "📚", 100, "TRANSACTION", false, null),
            
            // Health achievements
            AchievementEntity("health_7", "Healthy Week", "Log health data for 7 days", "❤️", 7, "HEALTH", false, null),
            AchievementEntity("health_30", "Health Champion", "Log health data for 30 days", "💚", 30, "HEALTH", false, null),
            
            // Mindful achievements
            AchievementEntity("mindful_1", "First Breath", "Complete your first mindful session", "🧘", 1, "MINDFUL", false, null),
            AchievementEntity("mindful_10", "Calm Mind", "Complete 10 mindful sessions", "☯️", 10, "MINDFUL", false, null),
            AchievementEntity("mindful_50", "Zen Master", "Complete 50 mindful sessions", "🌸", 50, "MINDFUL", false, null),
            
            // Task achievements
            AchievementEntity("task_done_5", "Task Starter", "Complete 5 tasks", "✅", 5, "TASK", false, null),
            AchievementEntity("task_done_25", "Productive", "Complete 25 tasks", "🎯", 25, "TASK", false, null),
            AchievementEntity("task_done_100", "Task Master", "Complete 100 tasks", "🏅", 100, "TASK", false, null)
        )
        defaultAchievements.forEach { achievementDao.insertAchievement(it) }
    }
}

class FocusSessionRepositoryImpl @Inject constructor(
    private val focusSessionDao: FocusSessionDao
) : FocusSessionRepository {
    override fun getSessionsForDateRange(startDate: Long, endDate: Long): Flow<List<FocusSession>> =
        focusSessionDao.getSessionsForDateRange(startDate, endDate).map { list -> list.map { it.toDomain() } }

    override fun getTotalMinutesForDateRange(startDate: Long, endDate: Long): Flow<Int> =
        focusSessionDao.getTotalMinutesForDateRange(startDate, endDate).map { it ?: 0 }

    override fun getCompletedSessionsCount(startDate: Long, endDate: Long): Flow<Int> =
        focusSessionDao.getCompletedSessionsCount(startDate, endDate)

    override suspend fun saveSession(session: FocusSession) {
        focusSessionDao.insertSession(session.toEntity())
    }
}

class TaskRepositoryImpl @Inject constructor(
    private val taskDao: TaskDao
) : TaskRepository {
    override fun getTasksForDateRange(startDate: Long, endDate: Long): Flow<List<Task>> =
        taskDao.getTasksForDateRange(startDate, endDate).map { list -> list.map { it.toDomain() } }

    override fun getTodayTasks(todayStart: Long): Flow<List<Task>> =
        taskDao.getTodayTasks(todayStart).map { list -> list.map { it.toDomain() } }

    override suspend fun getTaskById(id: String): Task? =
        taskDao.getTaskById(id)?.toDomain()

    override suspend fun saveTask(task: Task) {
        taskDao.insertTask(task.toEntity())
    }

    override suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task.toEntity())
    }

    override suspend fun setTaskCompleted(id: String, completed: Boolean) {
        taskDao.setTaskCompleted(id, completed, System.currentTimeMillis())
    }
}

class MindfulSessionRepositoryImpl @Inject constructor(
    private val mindfulSessionDao: MindfulSessionDao
) : MindfulSessionRepository {
    override fun getSessionsForDateRange(startDate: Long, endDate: Long): Flow<List<MindfulSession>> =
        mindfulSessionDao.getSessionsForDateRange(startDate, endDate).map { list -> list.map { it.toDomain() } }

    override fun getCompletedSessionsCount(startDate: Long, endDate: Long): Flow<Int> =
        mindfulSessionDao.getCompletedSessionsCount(startDate, endDate)

    override suspend fun saveSession(session: MindfulSession) {
        mindfulSessionDao.insertSession(session.toEntity())
    }
}

class WorkLogRepositoryImpl @Inject constructor(
    private val workLogDao: WorkLogDao
) : WorkLogRepository {
    override fun getWorkLogsForDateRange(startDate: Long, endDate: Long): Flow<List<WorkLog>> =
        workLogDao.getWorkLogsForDateRange(startDate, endDate).map { list -> list.map { it.toDomain() } }

    override fun getAllWorkLogs(): Flow<List<WorkLog>> =
        workLogDao.getAllWorkLogs().map { list -> list.map { it.toDomain() } }

    override fun getWorkLogsByType(dayType: String): Flow<List<WorkLog>> =
        workLogDao.getWorkLogsByType(dayType).map { list -> list.map { it.toDomain() } }

    override fun getCountByType(dayType: String): Flow<Int> =
        workLogDao.getCountByType(dayType)

    override fun getTotalWorkHours(startDate: Long, endDate: Long): Flow<Float> =
        workLogDao.getTotalWorkHours(startDate, endDate).map { it ?: 0f }

    override fun getTotalOvertimeHours(startDate: Long, endDate: Long): Flow<Float> =
        workLogDao.getTotalOvertimeHours(startDate, endDate).map { it ?: 0f }

    override suspend fun getWorkLogByDate(date: Long): WorkLog? =
        workLogDao.getWorkLogByDate(date)?.toDomain()

    override suspend fun addWorkLog(workLog: WorkLog) {
        workLogDao.insertWorkLog(workLog.toEntity())
    }

    override suspend fun addWorkLogs(workLogs: List<WorkLog>) {
        workLogDao.insertWorkLogs(workLogs.map { it.toEntity() })
    }

    override suspend fun updateWorkLog(workLog: WorkLog) {
        workLogDao.updateWorkLog(workLog.toEntity())
    }

    override suspend fun deleteWorkLog(workLog: WorkLog) {
        workLogDao.deleteWorkLog(workLog.toEntity())
    }

    override suspend fun deleteWorkLogsForRange(startDate: Long, endDate: Long) {
        workLogDao.deleteWorkLogsForRange(startDate, endDate)
    }

    override suspend fun deleteWorkLogByDate(date: Long) {
        workLogDao.deleteWorkLogByDate(date)
    }

    override suspend fun getWorkLogSummary(startDate: Long, endDate: Long): WorkLogSummary {
        var totalWorkDays = 0
        var totalOfficeDays = 0
        var totalHomeOfficeDays = 0
        var totalOffDays = 0
        var totalOvertimeDays = 0
        var totalHolidays = 0
        var totalSickLeaves = 0
        var totalPaidLeaves = 0
        var totalUnpaidLeaves = 0
        var totalBusinessTrips = 0
        var totalWorkHours = 0f
        var totalOvertimeHours = 0f

        workLogDao.getWorkLogsForDateRange(startDate, endDate).collect { logs ->
            logs.forEach { log ->
                totalWorkHours += log.workHours
                totalOvertimeHours += log.overtimeHours

                when (log.dayType) {
                    "WORKDAY" -> totalWorkDays++
                    "OFFICE" -> totalOfficeDays++
                    "HOME_OFFICE" -> totalHomeOfficeDays++
                    "OFF_DAY" -> totalOffDays++
                    "OVERTIME" -> {
                        totalOvertimeDays++
                        totalWorkDays++
                    }
                    "HOLIDAY" -> totalHolidays++
                    "SICK_LEAVE" -> totalSickLeaves++
                    "PAID_LEAVE" -> totalPaidLeaves++
                    "UNPAID_LEAVE" -> totalUnpaidLeaves++
                    "BUSINESS_TRIP" -> totalBusinessTrips++
                }
            }
        }

        val totalDays = totalWorkDays + totalOffDays + totalHolidays + totalSickLeaves + totalPaidLeaves + totalUnpaidLeaves + totalBusinessTrips
        val workPercentage = if (totalDays > 0) (totalWorkDays.toFloat() / totalDays * 100) else 0f
        val remotePercentage = if (totalWorkDays > 0) (totalHomeOfficeDays.toFloat() / totalWorkDays * 100) else 0f

        return WorkLogSummary(
            totalWorkDays = totalWorkDays,
            totalOfficeDays = totalOfficeDays,
            totalHomeOfficeDays = totalHomeOfficeDays,
            totalOffDays = totalOffDays,
            totalOvertimeDays = totalOvertimeDays,
            totalHolidays = totalHolidays,
            totalSickLeaves = totalSickLeaves,
            totalPaidLeaves = totalPaidLeaves,
            totalUnpaidLeaves = totalUnpaidLeaves,
            totalBusinessTrips = totalBusinessTrips,
            totalWorkHours = totalWorkHours,
            totalOvertimeHours = totalOvertimeHours,
            workPercentage = workPercentage,
            remotePercentage = remotePercentage
        )
    }
}
