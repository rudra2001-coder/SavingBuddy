package com.example.savingbuddy.data.local.dao

import androidx.room.*
import com.example.savingbuddy.data.local.entity.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HealthDao {
    @Query("SELECT * FROM health_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getHealthEntries(startDate: Long, endDate: Long): Flow<List<HealthEntryEntity>>

    @Query("SELECT * FROM health_entries WHERE date >= :todayStart ORDER BY date DESC LIMIT 1")
    fun getTodayEntry(todayStart: Long): Flow<HealthEntryEntity?>

    @Query("SELECT * FROM health_entries WHERE id = :id")
    suspend fun getEntryById(id: String): HealthEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: HealthEntryEntity)

    @Update
    suspend fun updateEntry(entry: HealthEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: HealthEntryEntity)

    @Query("DELETE FROM health_entries")
    suspend fun deleteAllEntries()
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun getAllEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getEntriesByDateRange(startDate: Long, endDate: Long): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries WHERE id = :id")
    suspend fun getEntryById(id: String): JournalEntryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: JournalEntryEntity)

    @Update
    suspend fun updateEntry(entry: JournalEntryEntity)

    @Delete
    suspend fun deleteEntry(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries")
    suspend fun deleteAllEntries()
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdAt DESC")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: String): HabitEntity?

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY date DESC")
    fun getHabitLogs(habitId: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE date >= :startDate AND date <= :endDate")
    fun getLogsForDateRange(startDate: Long, endDate: Long): Flow<List<HabitLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: HabitLogEntity)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteLog(habitId: String, date: Long)

    @Query("DELETE FROM habits")
    suspend fun deleteAllHabits()

    @Query("DELETE FROM habit_logs")
    suspend fun deleteAllHabitLogs()
}

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY unlocked DESC, title ASC")
    fun getAllAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE unlocked = 1")
    fun getUnlockedAchievements(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :id")
    suspend fun getAchievementById(id: String): AchievementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievement(achievement: AchievementEntity)

    @Update
    suspend fun updateAchievement(achievement: AchievementEntity)

    @Query("UPDATE achievements SET unlocked = 1, unlockedAt = :timestamp WHERE id = :id")
    suspend fun unlockAchievement(id: String, timestamp: Long)

    @Query("DELETE FROM achievements")
    suspend fun deleteAllAchievements()
}

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getSessionsForDateRange(startDate: Long, endDate: Long): Flow<List<FocusSessionEntity>>

    @Query("SELECT SUM(duration) FROM focus_sessions WHERE date >= :startDate AND date <= :endDate AND completed = 1")
    fun getTotalMinutesForDateRange(startDate: Long, endDate: Long): Flow<Int?>

    @Query("SELECT COUNT(*) FROM focus_sessions WHERE date >= :startDate AND date <= :endDate AND completed = 1")
    fun getCompletedSessionsCount(startDate: Long, endDate: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity)

    @Update
    suspend fun updateSession(session: FocusSessionEntity)

    @Query("DELETE FROM focus_sessions")
    suspend fun deleteAllSessions()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE date >= :startDate AND date <= :endDate ORDER BY date ASC, priority DESC")
    fun getTasksForDateRange(startDate: Long, endDate: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE date >= :todayStart AND completed = 0 ORDER BY priority DESC")
    fun getTodayTasks(todayStart: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: String): TaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity)

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Delete
    suspend fun deleteTask(task: TaskEntity)

    @Query("UPDATE tasks SET completed = :completed, updatedAt = :timestamp WHERE id = :id")
    suspend fun setTaskCompleted(id: String, completed: Boolean, timestamp: Long)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}

@Dao
interface MindfulSessionDao {
    @Query("SELECT * FROM mindful_sessions WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getSessionsForDateRange(startDate: Long, endDate: Long): Flow<List<MindfulSessionEntity>>

    @Query("SELECT COUNT(*) FROM mindful_sessions WHERE date >= :startDate AND date <= :endDate AND completed = 1")
    fun getCompletedSessionsCount(startDate: Long, endDate: Long): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: MindfulSessionEntity)

    @Update
    suspend fun updateSession(session: MindfulSessionEntity)

    @Query("DELETE FROM mindful_sessions")
    suspend fun deleteAllSessions()
}

@Dao
interface WorkLogDao {
    @Query("SELECT * FROM work_logs WHERE date >= :startDate AND date <= :endDate ORDER BY date DESC")
    fun getWorkLogsForDateRange(startDate: Long, endDate: Long): Flow<List<WorkLogEntity>>

    @Query("SELECT * FROM work_logs WHERE date = :date LIMIT 1")
    suspend fun getWorkLogByDate(date: Long): WorkLogEntity?

    @Query("SELECT * FROM work_logs ORDER BY date DESC")
    fun getAllWorkLogs(): Flow<List<WorkLogEntity>>

    @Query("SELECT * FROM work_logs WHERE dayType = :dayType ORDER BY date DESC")
    fun getWorkLogsByType(dayType: String): Flow<List<WorkLogEntity>>

    @Query("SELECT COUNT(*) FROM work_logs WHERE dayType = :dayType")
    fun getCountByType(dayType: String): Flow<Int>

    @Query("SELECT SUM(workHours) FROM work_logs WHERE date >= :startDate AND date <= :endDate")
    fun getTotalWorkHours(startDate: Long, endDate: Long): Flow<Float?>

    @Query("SELECT SUM(overtimeHours) FROM work_logs WHERE date >= :startDate AND date <= :endDate")
    fun getTotalOvertimeHours(startDate: Long, endDate: Long): Flow<Float?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkLog(workLog: WorkLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkLogs(workLogs: List<WorkLogEntity>)

    @Update
    suspend fun updateWorkLog(workLog: WorkLogEntity)

    @Delete
    suspend fun deleteWorkLog(workLog: WorkLogEntity)

    @Query("DELETE FROM work_logs WHERE date >= :startDate AND date <= :endDate")
    suspend fun deleteWorkLogsForRange(startDate: Long, endDate: Long)

    @Query("DELETE FROM work_logs WHERE date = :date")
    suspend fun deleteWorkLogByDate(date: Long)

    @Query("DELETE FROM work_logs")
    suspend fun deleteAllWorkLogs()
}
