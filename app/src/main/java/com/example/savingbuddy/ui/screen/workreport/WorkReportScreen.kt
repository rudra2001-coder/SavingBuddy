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
    val exportMessage: String? = null
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
    OFFICE("Office Days", "🏢"),
    HOME_OFFICE("Home Office", "🏠"),
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
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                workLogs = logs,
                filteredLogs = logs,
                summary = summary,
                reportSections = sections,
                dayTypeBreakdown = breakdown,
                monthlyData = monthlyData,
                productivityScore = productivityScore
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
                ReportFilter.HOME_OFFICE -> logs.filter { it.dayType == WorkDayType.HOME_OFFICE }
                ReportFilter.LEAVES -> logs.filter { it.dayType in listOf(WorkDayType.SICK_LEAVE, WorkDayType.PAID_LEAVE, WorkDayType.UNPAID_LEAVE, WorkDayType.HOLIDAY) }
                ReportFilter.OVERTIME -> logs.filter { it.dayType == WorkDayType.OVERTIME || it.overtimeHours > 0 }
                else -> logs
            }
            
            val sections = generateReportSections(filtered, summary)
            val breakdown = filtered.groupBy { it.dayType }.mapValues { it.value.size }
            val monthlyData = generateMonthlyData(filtered)
            val productivityScore = calculateProductivityScore(summary, filtered.size)
            
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                workLogs = logs,
                filteredLogs = filtered,
                summary = summary,
                dateRange = startDate to endDate,
                reportSections = sections,
                dayTypeBreakdown = breakdown,
                monthlyData = monthlyData,
                productivityScore = productivityScore
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showExportDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = "Export")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = "Work Reports",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                item {
                    ProductivityScoreCard(score = uiState.productivityScore)
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

                uiState.dateRange?.let { (start, end) ->
                    item {
                        Text(
                            text = "${SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(start))} - ${SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(end))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (uiState.dayTypeBreakdown.isNotEmpty()) {
                    item {
                        DayTypePieChart(breakdown = uiState.dayTypeBreakdown)
                    }
                }

                if (uiState.monthlyData.isNotEmpty()) {
                    item {
                        Text(
                            text = "Monthly Overview",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    item {
                        MonthlyBarChart(data = uiState.monthlyData)
                    }
                }

                item {
                    Text(
                        text = "Statistics",
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
                            ReportCard(section = section, modifier = Modifier.weight(1f))
                        }
                        if (rowSections.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                if (uiState.filteredLogs.isNotEmpty()) {
                    item {
                        HorizontalDivider()
                        Text(
                            text = "Recent Logs (${uiState.filteredLogs.size} entries)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(uiState.filteredLogs.sortedByDescending { it.date }.take(20)) { log ->
                        WorkLogItem(log = log)
                    }
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
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
fun DayTypePieChart(breakdown: Map<WorkDayType, Int>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Day Type Distribution",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(120.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val total = breakdown.values.sum().toFloat()
                        var startAngle = -90f
                        breakdown.forEach { (type, count) ->
                            val sweepAngle = (count / total) * 360f
                            drawArc(
                                color = getDayTypeColor(type),
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = true
                            )
                            startAngle += sweepAngle
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    breakdown.forEach { (type, count) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(12.dp).background(getDayTypeColor(type), CircleShape))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${type.displayName}: $count",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
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
