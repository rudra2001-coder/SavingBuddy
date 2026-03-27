package com.example.savingbuddy.ui.screen.workreport

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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
import kotlin.math.roundToInt

data class TimelineItem(
    val id: String,
    val timestamp: Long,
    val type: TimelineType,
    val title: String,
    val subtitle: String,
    val amount: Double? = null,
    val icon: String,
    val color: Color
)

enum class TimelineType {
    WORK_LOG, INCOME, EXPENSE, TRANSFER, SAVINGS
}

data class WorkReportUiState(
    val isLoading: Boolean = true,
    val workLogs: List<WorkLog> = emptyList(),
    val filteredLogs: List<WorkLog> = emptyList(),
    val summary: WorkLogSummary? = null,
    val selectedFilter: ReportFilter = ReportFilter.ALL,
    val dateRange: Pair<Long, Long>? = null,
    val selectedDayTypes: Set<WorkDayType> = emptySet(),
    val reportSections: List<ReportSection> = emptyList(),
    val dayTypeBreakdown: Map<WorkDayType, Int> = emptyMap(),
    val monthlyData: List<MonthlyWorkData> = emptyList(),
    val productivityScore: Int = 0,
    val showExportDialog: Boolean = false,
    val isExporting: Boolean = false,
    val exportMessage: String? = null,
    val timelineItems: List<TimelineItem> = emptyList(),
    val showTimeline: Boolean = true
)

data class MonthlyWorkData(
    val month: String,
    val workDays: Int,
    val officeDays: Int,
    val homeOfficeDays: Int,
    val totalHours: Float,
    val overtimeHours: Float
)

data class ReportSection(
    val id: String,
    val title: String,
    val icon: String,
    val value: String,
    val subtitle: String,
    val color: Color,
    val percentage: Float? = null
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
    OFFICE("Office", "🏢"),
    HOME("Home", "🏠"),
    REMOTE("Remote", "📡"),
    HOLIDAY("Holiday", "🎉"),
    LEAVES("Leaves", "🏖️")
}

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
            val thirtyDaysAgo = calendar.apply {
                add(Calendar.DAY_OF_YEAR, -30)
                set(Calendar.HOUR_OF_DAY, 0)
            }.timeInMillis
            
            val startOfYear = calendar.apply {
                set(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
            }.timeInMillis
            
            val logs = workLogRepository.getWorkLogsForDateRange(startOfYear, now).first()
            val summary = workLogRepository.getWorkLogSummary(startOfYear, now)
            
            val sections = generateReportSections(logs, summary)
            val breakdown = logs.groupBy { it.dayType }.mapValues { it.value.size }
            val monthlyData = generateMonthlyData(logs)
            val productivityScore = calculateProductivityScore(summary, logs.size)
            val timelineItems = generateTimelineItems(logs)
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                workLogs = logs,
                filteredLogs = logs,
                summary = summary,
                reportSections = sections,
                dayTypeBreakdown = breakdown,
                monthlyData = monthlyData,
                productivityScore = productivityScore,
                timelineItems = timelineItems
            )
        }
    }

    private fun generateTimelineItems(logs: List<WorkLog>): List<TimelineItem> {
        return logs.sortedByDescending { it.date }.take(30).map { log ->
            TimelineItem(
                id = log.id,
                timestamp = log.date,
                type = TimelineType.WORK_LOG,
                title = log.dayType.displayName,
                subtitle = "${log.workHours.toInt()}h work${if (log.overtimeHours > 0) " + ${log.overtimeHours.toInt()}h OT" else ""}",
                icon = log.dayType.icon,
                color = getDayTypeColor(log.dayType)
            )
        }
    }

    private fun generateMonthlyData(logs: List<WorkLog>): List<MonthlyWorkData> {
        val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        val grouped = logs.groupBy { 
            Calendar.getInstance().apply { timeInMillis = it.date }.get(Calendar.MONTH) 
        }
        
        return grouped.map { (month, monthLogs) ->
            MonthlyWorkData(
                month = monthNames[month],
                workDays = monthLogs.size,
                officeDays = monthLogs.count { it.dayType == WorkDayType.OFFICE },
                homeOfficeDays = monthLogs.count { it.dayType == WorkDayType.HOME_OFFICE },
                totalHours = monthLogs.sumOf { it.workHours.toDouble() }.toFloat(),
                overtimeHours = monthLogs.sumOf { it.overtimeHours.toDouble() }.toFloat()
            )
        }.sortedBy { monthNames.indexOf(it.month) }
    }

    private fun calculateProductivityScore(summary: WorkLogSummary, totalDays: Int): Int {
        var score = 50
        if (summary.totalWorkDays > 0) score += 20
        if (summary.totalOvertimeHours > 0) score += 15
        if (summary.totalOfficeDays > summary.totalHomeOfficeDays) score += 10
        if (totalDays >= 20) score += 5
        return score.coerceIn(0, 100)
    }

    private fun generateReportSections(logs: List<WorkLog>, summary: WorkLogSummary): List<ReportSection> {
        val sections = mutableListOf<ReportSection>()
        
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
            id = "off_days", title = "Off Days", icon = "🌴",
            value = "${summary.totalOffDays}", subtitle = "Weekends/days off",
            color = Color(0xFFFF9800)
        ))
        
        val avgWorkHoursPerDay = if (summary.totalWorkDays > 0) summary.totalWorkHours / summary.totalWorkDays else 0f
        sections.add(ReportSection(
            id = "avg_hours", title = "Avg Hours/Day", icon = "📊",
            value = "${String.format("%.1f", avgWorkHoursPerDay)}h", subtitle = "Average per work day",
            color = Color(0xFF673AB7)
        ))
        
        return sections
    }

    fun applyFilter(filter: ReportFilter) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(selectedFilter = filter, isLoading = true)
            
            val (startDate, endDate) = getDateRangeForFilter(filter)
            val logs = workLogRepository.getWorkLogsForDateRange(startDate, endDate).first()
            val summary = workLogRepository.getWorkLogSummary(startDate, endDate)
            
            val filtered = when (filter) {
                ReportFilter.OFFICE -> logs.filter { it.dayType == WorkDayType.OFFICE }
                ReportFilter.HOME -> logs.filter { it.dayType == WorkDayType.HOME || it.dayType == WorkDayType.HOME_OFFICE }
                ReportFilter.REMOTE -> logs.filter { it.dayType == WorkDayType.REMOTE }
                ReportFilter.HOLIDAY -> logs.filter { it.dayType == WorkDayType.HOLIDAY || it.dayType == WorkDayType.OFF_DAY }
                ReportFilter.LEAVES -> logs.filter { it.dayType in listOf(WorkDayType.SICK_LEAVE, WorkDayType.PAID_LEAVE, WorkDayType.UNPAID_LEAVE) }
                ReportFilter.OVERTIME -> logs.filter { it.dayType == WorkDayType.OVERTIME || it.overtimeHours > 0 }
                else -> logs
            }
            
            val sections = generateReportSections(filtered, summary)
            val breakdown = filtered.groupBy { it.dayType }.mapValues { it.value.size }
            val monthlyData = generateMonthlyData(filtered)
            val productivityScore = calculateProductivityScore(summary, filtered.size)
            val timelineItems = generateTimelineItems(filtered)
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                workLogs = logs,
                filteredLogs = filtered,
                summary = summary,
                dateRange = startDate to endDate,
                reportSections = sections,
                dayTypeBreakdown = breakdown,
                monthlyData = monthlyData,
                productivityScore = productivityScore,
                timelineItems = timelineItems
            )
        }
    }

    fun toggleTimeline() {
        _uiState.value = _uiState.value.copy(showTimeline = !_uiState.value.showTimeline)
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
            ReportFilter.ALL, ReportFilter.CUSTOM -> 0L to System.currentTimeMillis()
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

    fun showExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = true)
    }

    fun hideExportDialog() {
        _uiState.value = _uiState.value.copy(showExportDialog = false)
    }

    fun exportToCsv() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            try {
                val logs = _uiState.value.filteredLogs
                val csvContent = buildString {
                    appendLine("Date,Day Type,Work Hours,Overtime Hours,Note")
                    logs.forEach { log ->
                        appendLine("${dateFormat.format(Date(log.date))},${log.dayType.displayName},${log.workHours},${log.overtimeHours},${log.note ?: ""}")
                    }
                }
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    showExportDialog = false,
                    exportMessage = "Exported ${logs.size} records to CSV"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isExporting = false,
                    exportMessage = "Export failed: ${e.message}"
                )
            }
        }
    }

    fun clearExportMessage() {
        _uiState.value = _uiState.value.copy(exportMessage = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkReportScreen(
    viewModel: WorkReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.exportMessage) {
        uiState.exportMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearExportMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Work Reports", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.showExportDialog() }) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Export")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.workLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "📋",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No work logs yet",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Add entries from the Work Calendar",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "This Year",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${uiState.workLogs.size} days logged",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Work Days",
                            value = "${uiState.summary?.totalWorkDays ?: 0}",
                            icon = "💼",
                            color = Color(0xFF2196F3),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Hours",
                            value = "${uiState.summary?.totalWorkHours?.toInt() ?: 0}h",
                            icon = "⏱️",
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Overtime",
                            value = "${uiState.summary?.totalOvertimeHours?.toInt() ?: 0}h",
                            icon = "⏰",
                            color = Color(0xFFF44336),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(ReportFilter.entries.toList()) { filter ->
                            FilterChip(
                                selected = uiState.selectedFilter == filter,
                                onClick = { viewModel.applyFilter(filter) },
                                label = { Text(filter.displayName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }

                if (uiState.dayTypeBreakdown.isNotEmpty()) {
                    item {
                        Text(
                            text = "Work Type Distribution",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    item {
                        SimpleBarChart(breakdown = uiState.dayTypeBreakdown)
                    }
                }

                item {
                    Text(
                        text = "Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            title = "Office",
                            value = "${uiState.summary?.totalOfficeDays ?: 0}",
                            icon = "🏢",
                            color = Color(0xFF2196F3),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Home",
                            value = "${uiState.summary?.totalHomeOfficeDays ?: 0}",
                            icon = "🏠",
                            color = Color(0xFF9C27B0),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Off",
                            value = "${uiState.summary?.totalOffDays ?: 0}",
                            icon = "🌴",
                            color = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (uiState.filteredLogs.isNotEmpty()) {
                    item {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Recent Logs",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${uiState.filteredLogs.size} entries",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    items(uiState.filteredLogs.sortedByDescending { it.date }.take(15)) { log ->
                        WorkLogItem(log = log)
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }

    if (uiState.showExportDialog) {
        ExportDialog(
            onExport = { viewModel.exportToCsv() },
            onDismiss = { viewModel.hideExportDialog() },
            isExporting = uiState.isExporting
        )
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = icon, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ProductivityScoreCard(score: Int) {
    val color = when {
        score >= 80 -> Color(0xFF4CAF50)
        score >= 60 -> Color(0xFFFF9800)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(64.dp)) {
                CircularProgressIndicator(
                    progress = { score / 100f },
                    modifier = Modifier.fillMaxSize(),
                    strokeWidth = 6.dp,
                    color = color,
                    trackColor = color.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Productivity Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when {
                        score >= 80 -> "Excellent! Keep it up!"
                        score >= 60 -> "Good progress. Room for improvement."
                        else -> "Let's improve your work consistency."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SimpleBarChart(breakdown: Map<WorkDayType, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            val total = breakdown.values.sum()
            val sortedBreakdown = breakdown.entries.sortedByDescending { it.value }.take(6)
            
            sortedBreakdown.forEach { (type, count) ->
                val percentage = if (total > 0) (count.toFloat() / total) else 0f
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = type.icon,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.width(32.dp)
                    )
                    Text(
                        text = type.displayName,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(80.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(getDayTypeColor(type).copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(percentage)
                                .height(20.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(getDayTypeColor(type))
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MonthlyBarChart(data: List<MonthlyWorkData>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Work Hours by Month",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            val maxHours = data.maxOfOrNull { it.totalHours } ?: 1f

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                data.forEach { monthData ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.6f)
                                .height(((monthData.totalHours / maxHours) * 120).dp)
                                .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .background(Color(0xFF2196F3))
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = monthData.month,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
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
        Column(modifier = Modifier.padding(16.dp)) {
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
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = section.color,
                    trackColor = section.color.copy(alpha = 0.2f)
                )
            }
        }
    }
}

@Composable
fun WorkLogItem(log: WorkLog) {
    val dateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
    
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1
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

@Composable
fun ExportDialog(onExport: () -> Unit, onDismiss: () -> Unit, isExporting: Boolean) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Work Report") },
        text = {
            Column {
                Text("Export your work logs to CSV format for external analysis.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Includes: Date, Day Type, Work Hours, Overtime Hours, Notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(onClick = onExport, enabled = !isExporting) {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                } else {
                    Text("Export CSV")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun TimelineItemCard(item: TimelineItem) {
    val dateFormat = SimpleDateFormat("EEE, MMM dd • HH:mm", Locale.getDefault())
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = item.color.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(item.color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = item.icon, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = dateFormat.format(Date(item.timestamp)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            item.amount?.let { amount ->
                Text(
                    text = formatCurrency(amount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (amount >= 0) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
        }
    }
}
