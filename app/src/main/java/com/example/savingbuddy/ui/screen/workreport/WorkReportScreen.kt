package com.example.savingbuddy.ui.screen.workreport

import androidx.compose.foundation.background
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.model.WorkDayType
import com.example.savingbuddy.domain.model.WorkLog
import com.example.savingbuddy.domain.model.WorkLogSummary
import com.example.savingbuddy.domain.repository.WorkLogRepository
import com.example.savingbuddy.ui.screen.dashboard.formatCurrency
import com.example.savingbuddy.ui.screen.workcalendar.getDayTypeColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class WorkReportUiState(
    val isLoading: Boolean = true,
    val workLogs: List<WorkLog> = emptyList(),
    val filteredLogs: List<WorkLog> = emptyList(),
    val summary: WorkLogSummary? = null,
    
    // Filters
    val selectedFilter: ReportFilter = ReportFilter.ALL,
    val dateRange: Pair<Long, Long>? = null,
    val selectedDayTypes: Set<WorkDayType> = emptySet(),
    
    // Report sections
    val reportSections: List<ReportSection> = emptyList()
)

enum class ReportFilter(val displayName: String, val icon: String) {
    ALL("All", "📋"),
    TODAY("Today", "📅"),
    THIS_WEEK("This Week", "📆"),
    THIS_MONTH("This Month", "🗓️"),
    LAST_MONTH("Last Month", "⏮️"),
    THIS_YEAR("This Year", "📈"),
    CUSTOM("Custom Range", "🔍"),
    OVERTIME("Overtime", "⏰"),
    OFFICE("Office Days", "🏢"),
    HOME_OFFICE("Home Office", "🏠"),
    LEAVES("Leaves", "🏖️")
}

data class ReportSection(
    val id: String,
    val title: String,
    val icon: String,
    val value: String,
    val subtitle: String,
    val color: Color,
    val percentage: Float? = null
)

@HiltViewModel
class WorkReportViewModel @Inject constructor(
    private val workLogRepository: WorkLogRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WorkReportUiState())
    val uiState: StateFlow<WorkReportUiState> = _uiState.asStateFlow()
    
    private val calendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    init {
        loadReports()
    }

    private fun loadReports() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val startOfYear = calendar.apply {
                set(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
            }.timeInMillis
            
            val logs = workLogRepository.getWorkLogsForDateRange(startOfYear, now).first()
            val summary = workLogRepository.getWorkLogSummary(startOfYear, now)
            
            val sections = generateReportSections(logs, summary)
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                workLogs = logs,
                filteredLogs = logs,
                summary = summary,
                reportSections = sections
            )
        }
    }

    private fun generateReportSections(logs: List<WorkLog>, summary: WorkLogSummary): List<ReportSection> {
        val sections = mutableListOf<ReportSection>()
        
        // 1. Overview Section
        sections.add(ReportSection(
            id = "total_days", title = "Total Days Tracked", icon = "📅",
            value = "${logs.size}", subtitle = "Days logged this year",
            color = Color(0xFF2196F3)
        ))
        
        sections.add(ReportSection(
            id = "work_hours", title = "Total Work Hours", icon = "⏱️",
            value = "${summary.totalWorkHours.toInt()}h", subtitle = "Including overtime",
            color = Color(0xFF4CAF50)
        ))
        
        sections.add(ReportSection(
            id = "overtime", title = "Overtime Hours", icon = "⏰",
            value = "${summary.totalOvertimeHours.toInt()}h", subtitle = "Extra hours worked",
            color = Color(0xFFF44336)
        ))
        
        // 2. Work Location Breakdown
        sections.add(ReportSection(
            id = "office", title = "Office Days", icon = "🏢",
            value = "${summary.totalOfficeDays}", subtitle = "${summary.workPercentage.toInt()}% of work days",
            color = Color(0xFF4CAF50),
            percentage = if (summary.totalWorkDays > 0) summary.totalOfficeDays.toFloat() / summary.totalWorkDays * 100 else 0f
        ))
        
        sections.add(ReportSection(
            id = "home_office", title = "Home Office", icon = "🏠",
            value = "${summary.totalHomeOfficeDays}", subtitle = "${summary.remotePercentage.toInt()}% remote",
            color = Color(0xFF9C27B0),
            percentage = if (summary.totalWorkDays > 0) summary.totalHomeOfficeDays.toFloat() / summary.totalWorkDays * 100 else 0f
        ))
        
        sections.add(ReportSection(
            id = "workday", title = "General Workday", icon = "💼",
            value = "${summary.totalWorkDays}", subtitle = "Standard work days",
            color = Color(0xFF2196F3)
        ))
        
        // 3. Leaves & Absences
        sections.add(ReportSection(
            id = "off_days", title = "Off Days", icon = "🌴",
            value = "${summary.totalOffDays}", subtitle = "Weekends/days off",
            color = Color(0xFFFF9800)
        ))
        
        sections.add(ReportSection(
            id = "holidays", title = "Holidays", icon = "🎉",
            value = "${summary.totalHolidays}", subtitle = "Public holidays",
            color = Color(0xFFE91E63)
        ))
        
        sections.add(ReportSection(
            id = "sick_leave", title = "Sick Leave", icon = "🤒",
            value = "${summary.totalSickLeaves}", subtitle = "Sick days taken",
            color = Color(0xFF795548)
        ))
        
        sections.add(ReportSection(
            id = "paid_leave", title = "Paid Leave", icon = "🏖️",
            value = "${summary.totalPaidLeaves}", subtitle = "Vacation days",
            color = Color(0xFF00BCD4)
        ))
        
        sections.add(ReportSection(
            id = "unpaid_leave", title = "Unpaid Leave", icon = "❌",
            value = "${summary.totalUnpaidLeaves}", subtitle = "Unpaid days",
            color = Color(0xFF607D8B)
        ))
        
        sections.add(ReportSection(
            id = "business_trip", title = "Business Trip", icon = "✈️",
            value = "${summary.totalBusinessTrips}", subtitle = "Work trips",
            color = Color(0xFF3F51B5)
        ))
        
        // 4. Calculations
        val avgWorkHoursPerDay = if (summary.totalWorkDays > 0) summary.totalWorkHours / summary.totalWorkDays else 0f
        sections.add(ReportSection(
            id = "avg_hours", title = "Avg Hours/Day", icon = "📊",
            value = "${String.format("%.1f", avgWorkHoursPerDay)}h", subtitle = "Average per work day",
            color = Color(0xFF673AB7)
        ))
        
        val workDaysPerMonth = if (logs.isNotEmpty()) {
            val months = logs.groupBy { Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.MONTH) }
            summary.totalWorkDays.toFloat() / months.size.coerceAtLeast(1)
        } else 0f
        
        sections.add(ReportSection(
            id = "monthly_avg", title = "Monthly Avg", icon = "📈",
            value = "${String.format("%.1f", workDaysPerMonth)}", subtitle = "Work days per month",
            color = Color(0xFF00BCD4)
        ))
        
        return sections
    }

    fun applyFilter(filter: ReportFilter) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedFilter = filter, isLoading = true)
            
            val (startDate, endDate) = getDateRangeForFilter(filter)
            val logs = workLogRepository.getWorkLogsForDateRange(startDate, endDate).first()
            val summary = workLogRepository.getWorkLogSummary(startDate, endDate)
            
            val filtered = if (filter == ReportFilter.OFFICE) {
                logs.filter { it.dayType == WorkDayType.OFFICE }
            } else if (filter == ReportFilter.HOME_OFFICE) {
                logs.filter { it.dayType == WorkDayType.HOME_OFFICE }
            } else if (filter == ReportFilter.LEAVES) {
                logs.filter { it.dayType in listOf(
                    WorkDayType.SICK_LEAVE, WorkDayType.PAID_LEAVE, 
                    WorkDayType.UNPAID_LEAVE, WorkDayType.HOLIDAY
                )}
            } else if (filter == ReportFilter.OVERTIME) {
                logs.filter { it.dayType == WorkDayType.OVERTIME || it.overtimeHours > 0 }
            } else {
                logs
            }
            
            val sections = generateReportSections(filtered, summary)
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                workLogs = logs,
                filteredLogs = filtered,
                summary = summary,
                dateRange = startDate to endDate,
                reportSections = sections
            )
        }
    }

    private fun getDateRangeForFilter(filter: ReportFilter): Pair<Long, Long> {
        val now = Calendar.getInstance()
        val startCal = Calendar.getInstance()
        
        return when (filter) {
            ReportFilter.TODAY -> {
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.set(Calendar.MINUTE, 0)
                startCal.timeInMillis to System.currentTimeMillis()
            }
            ReportFilter.THIS_WEEK -> {
                startCal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.timeInMillis to System.currentTimeMillis()
            }
            ReportFilter.THIS_MONTH -> {
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.timeInMillis to System.currentTimeMillis()
            }
            ReportFilter.LAST_MONTH -> {
                startCal.add(Calendar.MONTH, -1)
                startCal.set(Calendar.DAY_OF_MONTH, 1)
                val start = startCal.timeInMillis
                startCal.set(Calendar.DAY_OF_MONTH, startCal.getActualMaximum(Calendar.DAY_OF_MONTH))
                start to startCal.timeInMillis
            }
            ReportFilter.THIS_YEAR -> {
                startCal.set(Calendar.DAY_OF_YEAR, 1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.timeInMillis to System.currentTimeMillis()
            }
            ReportFilter.ALL, ReportFilter.CUSTOM -> {
                0L to System.currentTimeMillis()
            }
            else -> {
                startCal.set(Calendar.DAY_OF_YEAR, 1)
                startCal.set(Calendar.HOUR_OF_DAY, 0)
                startCal.timeInMillis to System.currentTimeMillis()
            }
        }
    }

    fun setCustomDateRange(start: Long, end: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, selectedFilter = ReportFilter.CUSTOM)
            val logs = workLogRepository.getWorkLogsForDateRange(start, end).first()
            val summary = workLogRepository.getWorkLogSummary(start, end)
            val sections = generateReportSections(logs, summary)
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                workLogs = logs,
                filteredLogs = logs,
                summary = summary,
                dateRange = start to end,
                reportSections = sections
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkReportScreen(
    viewModel: WorkReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Work Reports",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Filter chips
        item {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(ReportFilter.entries.toList()) { filter ->
                    FilterChip(
                        selected = uiState.selectedFilter == filter,
                        onClick = { viewModel.applyFilter(filter) },
                        label = { Text(filter.displayName) },
                        leadingIcon = if (uiState.selectedFilter == filter) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }
        }
        
        // Date range display
        uiState.dateRange?.let { (start, end) ->
            item {
                Text(
                    text = "${dateFormat.format(Date(start))} - ${dateFormat.format(Date(end))}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        // Report Summary Cards
        item {
            Text(
                text = "Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        items(uiState.reportSections.chunked(2)) { rowSections ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowSections.forEach { section ->
                    ReportCard(
                        section = section,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (rowSections.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
        
        // Work Log List
        if (uiState.filteredLogs.isNotEmpty()) {
            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Log Details (${uiState.filteredLogs.size} entries)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            items(uiState.filteredLogs.sortedByDescending { it.date }) { log ->
                WorkLogItem(log = log)
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ReportCard(section: ReportSection, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = section.color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = section.icon, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = section.title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = section.value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = section.color
            )
            Text(
                text = section.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            section.percentage?.let { pct ->
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { pct / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = section.color,
                    trackColor = section.color.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun WorkLogItem(log: WorkLog) {
    val dateFormat = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(getDayTypeColor(log.dayType).copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = log.dayType.icon, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.dayType.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = dateFormat.format(Date(log.date)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!log.note.isNullOrBlank()) {
                    Text(
                        text = log.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${log.workHours.toInt()}h",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = getDayTypeColor(log.dayType)
                )
                if (log.overtimeHours > 0) {
                    Text(
                        text = "+${log.overtimeHours.toInt()}h OT",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())