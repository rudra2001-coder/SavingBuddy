package com.example.savingbuddy.domain.model

enum class RecurringType {
    DAILY,
    WEEKLY,
    WEEKDAYS_ONLY,
    WEEKENDS_ONLY,
    MONTHLY,
    YEARLY
}

enum class DayOfWeek(val value: Int, val shortName: String, val fullName: String) {
    SUNDAY(0, "Sun", "Sunday"),
    MONDAY(1, "Mon", "Monday"),
    TUESDAY(2, "Tue", "Tuesday"),
    WEDNESDAY(3, "Wed", "Wednesday"),
    THURSDAY(4, "Thu", "Thursday"),
    FRIDAY(5, "Fri", "Friday"),
    SATURDAY(6, "Sat", "Saturday");

    companion object {
        fun fromInt(value: Int): DayOfWeek = entries.find { it.value == value } ?: MONDAY
    }
}

data class RecurringTransaction(
    val id: String,
    val title: String,
    val amount: Double,
    val type: TransactionType,
    val recurringType: RecurringType,
    val startDate: Long,
    val endDate: Long? = null,
    val selectedDays: List<Int>? = null,
    val selectedDate: Int? = null,
    val isActive: Boolean = true,
    val categoryId: String,
    val accountId: String,
    val note: String? = null,
    val lastProcessedDate: Long? = null,
    val excludeHolidays: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderMinutesBefore: Int = 60,
    val createdAt: Long,
    val updatedAt: Long
)
