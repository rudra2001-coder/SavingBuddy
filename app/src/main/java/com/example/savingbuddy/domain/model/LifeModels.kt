package com.example.savingbuddy.domain.model

data class HealthEntry(
    val id: String,
    val date: Long,
    val sleepHours: Float,
    val sleepQuality: Int,
    val waterIntake: Float,
    val steps: Int?,
    val calories: Int?,
    val screenTime: Float?,
    val workHours: Float,
    val breakTime: Float,
    val mood: Int,
    val stressLevel: Int,
    val createdAt: Long,
    val updatedAt: Long
) {
    val healthScore: Int
        get() {
            var score = 0
            if (sleepHours >= 7) score += 20
            else if (sleepHours >= 6) score += 15
            else if (sleepHours >= 5) score += 10
            score += sleepQuality * 4
            if (waterIntake >= 2.5f) score += 20
            else if (waterIntake >= 2f) score += 15
            else if (waterIntake >= 1.5f) score += 10
            score += (5 - stressLevel) * 4
            return score.coerceIn(0, 100)
        }
}

data class JournalEntry(
    val id: String,
    val date: Long,
    val mood: Int,
    val title: String?,
    val content: String,
    val tags: List<String>,
    val createdAt: Long,
    val updatedAt: Long
)

data class Habit(
    val id: String,
    val name: String,
    val type: String,
    val target: Int,
    val currentStreak: Int,
    val longestStreak: Int,
    val icon: String,
    val color: Long,
    val isActive: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)

data class HabitLog(
    val habitId: String,
    val date: Long,
    val completed: Boolean
)

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val icon: String,
    val requirement: Int,
    val type: String,
    val unlocked: Boolean,
    val unlockedAt: Long?
)

data class FocusSession(
    val id: String,
    val duration: Int,
    val completed: Boolean,
    val date: Long,
    val createdAt: Long
)

data class Task(
    val id: String,
    val title: String,
    val date: Long,
    val priority: String,
    val completed: Boolean,
    val category: String?,
    val createdAt: Long,
    val updatedAt: Long
)

data class MindfulSession(
    val id: String,
    val type: String,
    val durationSeconds: Int,
    val completed: Boolean,
    val date: Long,
    val createdAt: Long
)

enum class TaskPriority {
    LOW, MEDIUM, HIGH
}

enum class Mood(val emoji: String, val label: String) {
    GREAT("😄", "Great"),
    GOOD("🙂", "Good"),
    OKAY("😐", "Okay"),
    BAD("😔", "Bad"),
    TERRIBLE("😢", "Terrible")
}

enum class MindfulType(val label: String, val durationSeconds: Int) {
    BREATHING("Breathing", 60),
    MEDITATION("Meditation", 120),
    PAUSE("Quick Pause", 30),
    BODY_SCAN("Body Scan", 180)
}
