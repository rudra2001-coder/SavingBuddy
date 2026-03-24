package com.example.savingbuddy.data.local.mapper

import com.example.savingbuddy.data.local.entity.HealthEntryEntity
import com.example.savingbuddy.data.local.entity.JournalEntryEntity
import com.example.savingbuddy.data.local.entity.HabitEntity
import com.example.savingbuddy.data.local.entity.HabitLogEntity
import com.example.savingbuddy.data.local.entity.AchievementEntity
import com.example.savingbuddy.data.local.entity.FocusSessionEntity
import com.example.savingbuddy.data.local.entity.TaskEntity
import com.example.savingbuddy.data.local.entity.MindfulSessionEntity
import com.example.savingbuddy.data.local.entity.UserPreferencesEntity
import com.example.savingbuddy.data.local.entity.WorkDayType as EntityWorkDayType
import com.example.savingbuddy.data.local.entity.WorkLogEntity
import com.example.savingbuddy.domain.model.*

fun HealthEntryEntity.toDomain() = HealthEntry(
    id = id,
    date = date,
    sleepHours = sleepHours,
    sleepQuality = sleepQuality,
    waterIntake = waterIntake,
    steps = steps,
    calories = calories,
    screenTime = screenTime,
    workHours = workHours,
    breakTime = breakTime,
    mood = mood,
    stressLevel = stressLevel,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun HealthEntry.toEntity() = HealthEntryEntity(
    id = id,
    date = date,
    sleepHours = sleepHours,
    sleepQuality = sleepQuality,
    waterIntake = waterIntake,
    steps = steps,
    calories = calories,
    screenTime = screenTime,
    workHours = workHours,
    breakTime = breakTime,
    mood = mood,
    stressLevel = stressLevel,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun JournalEntryEntity.toDomain() = JournalEntry(
    id = id,
    date = date,
    mood = mood,
    title = title,
    content = content,
    tags = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() },
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun JournalEntry.toEntity() = JournalEntryEntity(
    id = id,
    date = date,
    mood = mood,
    title = title,
    content = content,
    tags = tags.joinToString(","),
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun HabitEntity.toDomain() = Habit(
    id = id,
    name = name,
    type = type,
    target = target,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    icon = icon,
    color = color,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Habit.toEntity() = HabitEntity(
    id = id,
    name = name,
    type = type,
    target = target,
    currentStreak = currentStreak,
    longestStreak = longestStreak,
    icon = icon,
    color = color,
    isActive = isActive,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun HabitLogEntity.toDomain() = HabitLog(
    habitId = habitId,
    date = date,
    completed = completed
)

fun HabitLog.toEntity() = HabitLogEntity(
    habitId = habitId,
    date = date,
    completed = completed
)

fun AchievementEntity.toDomain() = Achievement(
    id = id,
    title = title,
    description = description,
    icon = icon,
    requirement = requirement,
    type = type,
    unlocked = unlocked,
    unlockedAt = unlockedAt
)

fun Achievement.toEntity() = AchievementEntity(
    id = id,
    title = title,
    description = description,
    icon = icon,
    requirement = requirement,
    type = type,
    unlocked = unlocked,
    unlockedAt = unlockedAt
)

fun FocusSessionEntity.toDomain() = FocusSession(
    id = id,
    duration = duration,
    completed = completed,
    date = date,
    createdAt = createdAt
)

fun FocusSession.toEntity() = FocusSessionEntity(
    id = id,
    duration = duration,
    completed = completed,
    date = date,
    createdAt = createdAt
)

fun TaskEntity.toDomain() = Task(
    id = id,
    title = title,
    date = date,
    priority = priority,
    completed = completed,
    category = category,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Task.toEntity() = TaskEntity(
    id = id,
    title = title,
    date = date,
    priority = priority,
    completed = completed,
    category = category,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun MindfulSessionEntity.toDomain() = MindfulSession(
    id = id,
    type = type,
    durationSeconds = durationSeconds,
    completed = completed,
    date = date,
    createdAt = createdAt
)

fun MindfulSession.toEntity() = MindfulSessionEntity(
    id = id,
    type = type,
    durationSeconds = durationSeconds,
    completed = completed,
    date = date,
    createdAt = createdAt
)

fun UserPreferencesEntity.toDomain() = UserPreferences(
    id = id,
    userName = userName,
    monthlyIncome = monthlyIncome,
    currency = currency,
    currencySymbol = currencySymbol,
    autoBackupEnabled = autoBackupEnabled,
    backupFrequency = backupFrequency,
    lastBackupDate = lastBackupDate,
    darkModeEnabled = darkModeEnabled,
    notificationsEnabled = notificationsEnabled,
    defaultAccountId = defaultAccountId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun UserPreferences.toEntity() = UserPreferencesEntity(
    id = id,
    userName = userName,
    monthlyIncome = monthlyIncome,
    currency = currency,
    currencySymbol = currencySymbol,
    autoBackupEnabled = autoBackupEnabled,
    backupFrequency = backupFrequency,
    lastBackupDate = lastBackupDate,
    darkModeEnabled = darkModeEnabled,
    notificationsEnabled = notificationsEnabled,
    defaultAccountId = defaultAccountId,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun WorkLogEntity.toDomain() = WorkLog(
    id = id,
    date = date,
    dayType = WorkDayType.valueOf(dayType),
    workHours = workHours,
    overtimeHours = overtimeHours,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun WorkLog.toEntity() = WorkLogEntity(
    id = id,
    date = date,
    dayType = dayType.name,
    workHours = workHours,
    overtimeHours = overtimeHours,
    note = note,
    createdAt = createdAt,
    updatedAt = updatedAt
)
