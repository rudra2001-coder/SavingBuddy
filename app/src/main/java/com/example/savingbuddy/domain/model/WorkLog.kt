package com.example.savingbuddy.domain.model

import java.util.UUID

data class WorkLog(
    val id: String = UUID.randomUUID().toString(),
    val date: Long,
    val dayType: WorkDayType,
    val workHours: Float = 8f,
    val overtimeHours: Float = 0f,
    val note: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class WorkDayType(val displayName: String, val icon: String, val workHoursDefault: Float) {
    WORKDAY("Workday", "💼", 8f),
    OFFICE("Office", "🏢", 8f),
    HOME("Home", "🏠", 8f),
    REMOTE("Remote", "📡", 8f),
    HOME_OFFICE("Home Office", "🏡", 8f),
    OFF_DAY("Off Day", "🌴", 0f),
    HOLIDAY("Holiday", "🎉", 0f),
    SICK_LEAVE("Sick Leave", "🤒", 0f),
    PAID_LEAVE("Paid Leave", "🏖️", 0f),
    UNPAID_LEAVE("Unpaid Leave", "❌", 0f),
    OVERTIME("Overtime", "⏰", 8f),
    BUSINESS_TRIP("Business Trip", "✈️", 8f),
    TRAVEL("Travel", "🚄", 8f),
    CUSTOM("Custom", "⭐", 8f)
}

data class WorkLogSummary(
    val totalWorkDays: Int = 0,
    val totalOfficeDays: Int = 0,
    val totalHomeOfficeDays: Int = 0,
    val totalOffDays: Int = 0,
    val totalOvertimeDays: Int = 0,
    val totalHolidays: Int = 0,
    val totalSickLeaves: Int = 0,
    val totalPaidLeaves: Int = 0,
    val totalUnpaidLeaves: Int = 0,
    val totalBusinessTrips: Int = 0,
    val totalWorkHours: Float = 0f,
    val totalOvertimeHours: Float = 0f,
    val workPercentage: Float = 0f,
    val remotePercentage: Float = 0f
)

data class WorkCalendarDay(
    val date: Long,
    val dayOfMonth: Int,
    val dayOfWeek: String,
    val month: Int,
    val year: Int,
    val workLog: WorkLog?,
    val isCurrentMonth: Boolean = true,
    val isToday: Boolean = false
)