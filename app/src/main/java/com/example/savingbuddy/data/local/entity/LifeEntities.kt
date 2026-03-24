package com.example.savingbuddy.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "health_entries")
data class HealthEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: Long,
    val sleepHours: Float = 0f,
    val sleepQuality: Int = 3,
    val waterIntake: Float = 0f,
    val steps: Int? = null,
    val calories: Int? = null,
    val screenTime: Float? = null,
    val workHours: Float = 0f,
    val breakTime: Float = 0f,
    val mood: Int = 3,
    val stressLevel: Int = 3,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "journal_entries")
data class JournalEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: Long,
    val mood: Int = 3,
    val title: String? = null,
    val content: String,
    val tags: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val type: String,
    val target: Int = 1,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val icon: String = "✓",
    val color: Long = 0xFF4CAF50,
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "habit_logs", primaryKeys = ["habitId", "date"])
data class HabitLogEntity(
    val habitId: String,
    val date: Long,
    val completed: Boolean = true
)

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val requirement: Int,
    val type: String,
    val unlocked: Boolean = false,
    val unlockedAt: Long? = null
)

@Entity(tableName = "focus_sessions")
data class FocusSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val duration: Int,
    val completed: Boolean = false,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val date: Long,
    val priority: String = "MEDIUM",
    val completed: Boolean = false,
    val category: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "mindful_sessions")
data class MindfulSessionEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: String,
    val durationSeconds: Int,
    val completed: Boolean = false,
    val date: Long,
    val createdAt: Long = System.currentTimeMillis()
)

enum class WorkDayType {
    WORKDAY,
    HOME_OFFICE,
    OFFICE,
    OFF_DAY,
    OVERTIME,
    HOLIDAY,
    SICK_LEAVE,
    PAID_LEAVE,
    UNPAID_LEAVE,
    BUSINESS_TRIP
}

@Entity(tableName = "work_logs")
data class WorkLogEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val date: Long,
    val dayType: String,
    val workHours: Float = 8f,
    val overtimeHours: Float = 0f,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
