package com.example.savingbuddy.ui.screen.workcalendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import com.example.savingbuddy.domain.model.WorkCalendarDay
import com.example.savingbuddy.domain.model.WorkDayType
import com.example.savingbuddy.domain.model.WorkLog
import com.example.savingbuddy.domain.model.WorkLogSummary
import com.example.savingbuddy.domain.repository.WorkLogRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class WorkCalendarUiState(
    val isLoading: Boolean = true,
    val currentMonth: Int = 0,
    val currentYear: Int = 0,
    val calendarDays: List<WorkCalendarDay> = emptyList(),
    val workLogs: Map<Long, WorkLog> = emptyMap(),
    val selectedDates: Set<Long> = emptySet(),
    val isSelectionMode: Boolean = false,
    val showDayTypeDialog: Boolean = false,
    val selectedDayType: WorkDayType = WorkDayType.WORKDAY,
    val summary: WorkLogSummary? = null,
    val monthNames: List<String> = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )
)

@HiltViewModel
class WorkCalendarViewModel @Inject constructor(
    private val workLogRepository: WorkLogRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(WorkCalendarUiState())
    val uiState: StateFlow<WorkCalendarUiState> = _uiState.asStateFlow()
    
    private val calendar = Calendar.getInstance()

    init {
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        _uiState.value = _uiState.value.copy(currentMonth = month, currentYear = year)
        loadCalendar()
    }

    private fun loadCalendar() {
        viewModelScope.launch {
            val state = _uiState.value
            val (startOfMonth, endOfMonth) = getMonthRange(state.currentMonth, state.currentYear)
            
            val workLogs = workLogRepository.getWorkLogsForDateRange(startOfMonth, endOfMonth).first()
            val logsMap = workLogs.associateBy { it.date }
            
            val calendarDays = generateCalendarDays(state.currentMonth, state.currentYear, logsMap)
            
            val summary = workLogRepository.getWorkLogSummary(startOfMonth, endOfMonth)
            
            _uiState.value = state.copy(
                isLoading = false,
                calendarDays = calendarDays,
                workLogs = logsMap,
                summary = summary
            )
        }
    }

    private fun getMonthRange(month: Int, year: Int): Pair<Long, Long> {
        val startCal = Calendar.getInstance().apply {
            set(year, month, 1, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val endCal = Calendar.getInstance().apply {
            set(year, month, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
        }
        return startCal.timeInMillis to endCal.timeInMillis
    }

    private fun generateCalendarDays(month: Int, year: Int, workLogs: Map<Long, WorkLog>): List<WorkCalendarDay> {
        val days = mutableListOf<WorkCalendarDay>()
        val cal = Calendar.getInstance()
        val today = Calendar.getInstance()
        
        cal.set(year, month, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val dayNames = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
        
        // Previous month days
        val prevMonth = if (month == 0) 11 else month - 1
        val prevYear = if (month == 0) year - 1 else year
        cal.set(prevMonth, 1)
        val daysInPrevMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        
        for (i in firstDayOfWeek - 2 downTo 0) {
            val day = daysInPrevMonth - i
            cal.set(prevYear, prevMonth, day)
            val date = normalizeDate(cal.timeInMillis)
            days.add(
                WorkCalendarDay(
                    date = date,
                    dayOfMonth = day,
                    dayOfWeek = dayNames[dayNames.indexOfFirst { it == dayNames[(day + firstDayOfWeek - 2) % 7] }.takeIf { it >= 0 } ?: 0],
                    month = prevMonth,
                    year = prevYear,
                    workLog = workLogs[date],
                    isCurrentMonth = false,
                    isToday = false
                )
            )
        }
        
        // Current month days
        for (day in 1..daysInMonth) {
            cal.set(year, month, day)
            val date = normalizeDate(cal.timeInMillis)
            val isToday = today.get(Calendar.YEAR) == year && 
                         today.get(Calendar.MONTH) == month && 
                         today.get(Calendar.DAY_OF_MONTH) == day
            days.add(
                WorkCalendarDay(
                    date = date,
                    dayOfMonth = day,
                    dayOfWeek = dayNames[(firstDayOfWeek - 1 + day - 1) % 7],
                    month = month,
                    year = year,
                    workLog = workLogs[date],
                    isCurrentMonth = true,
                    isToday = isToday
                )
            )
        }
        
        // Next month days
        val nextMonth = if (month == 11) 0 else month + 1
        val nextYear = if (month == 11) year + 1 else year
        var nextDay = 1
        while (days.size < 42) {
            cal.set(nextYear, nextMonth, nextDay)
            val date = normalizeDate(cal.timeInMillis)
            days.add(
                WorkCalendarDay(
                    date = date,
                    dayOfMonth = nextDay,
                    dayOfWeek = dayNames[(firstDayOfWeek - 1 + daysInMonth + nextDay - 1) % 7],
                    month = nextMonth,
                    year = nextYear,
                    workLog = workLogs[date],
                    isCurrentMonth = false,
                    isToday = false
                )
            )
            nextDay++
        }
        
        return days
    }

    private fun normalizeDate(timestamp: Long): Long {
        val cal = Calendar.getInstance().apply {
            timeInMillis = timestamp
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun previousMonth() {
        val state = _uiState.value
        val newMonth = if (state.currentMonth == 0) 11 else state.currentMonth - 1
        val newYear = if (state.currentMonth == 0) state.currentYear - 1 else state.currentYear
        _uiState.value = state.copy(currentMonth = newMonth, currentYear = newYear, isLoading = true)
        loadCalendar()
    }

    fun nextMonth() {
        val state = _uiState.value
        val newMonth = if (state.currentMonth == 11) 0 else state.currentMonth + 1
        val newYear = if (state.currentMonth == 11) state.currentYear + 1 else state.currentYear
        _uiState.value = state.copy(currentMonth = newMonth, currentYear = newYear, isLoading = true)
        loadCalendar()
    }

    fun toggleDateSelection(date: Long) {
        val current = _uiState.value.selectedDates.toMutableSet()
        if (current.contains(date)) {
            current.remove(date)
        } else {
            current.add(date)
        }
        _uiState.value = _uiState.value.copy(selectedDates = current)
    }

    fun enterSelectionMode() {
        _uiState.value = _uiState.value.copy(isSelectionMode = true)
    }

    fun exitSelectionMode() {
        _uiState.value = _uiState.value.copy(isSelectionMode = false, selectedDates = emptySet())
    }

    fun selectAllCurrentMonth() {
        val state = _uiState.value
        val allDates = state.calendarDays
            .filter { it.isCurrentMonth }
            .map { it.date }
            .toSet()
        _uiState.value = state.copy(selectedDates = allDates)
    }

    fun showDayTypeDialog() {
        _uiState.value = _uiState.value.copy(showDayTypeDialog = true)
    }

    fun hideDayTypeDialog() {
        _uiState.value = _uiState.value.copy(showDayTypeDialog = false)
    }

    fun setSelectedDayType(type: WorkDayType) {
        _uiState.value = _uiState.value.copy(selectedDayType = type)
    }

    fun applyDayTypeToSelectedDates() {
        viewModelScope.launch {
            val state = _uiState.value
            val dates = state.selectedDates.toList()
            val dayType = state.selectedDayType
            
            val workLogs = dates.map { date ->
                WorkLog(
                    date = date,
                    dayType = dayType,
                    workHours = dayType.workHoursDefault,
                    overtimeHours = if (dayType == WorkDayType.OVERTIME) 2f else 0f
                )
            }
            
            workLogRepository.addWorkLogs(workLogs)
            
            _uiState.value = state.copy(
                showDayTypeDialog = false,
                selectedDates = emptySet(),
                isSelectionMode = false,
                isLoading = true
            )
            loadCalendar()
        }
    }

    fun updateSingleDay(date: Long, dayType: WorkDayType) {
        viewModelScope.launch {
            val workLog = WorkLog(
                date = date,
                dayType = dayType,
                workHours = dayType.workHoursDefault,
                overtimeHours = if (dayType == WorkDayType.OVERTIME) 2f else 0f
            )
            workLogRepository.addWorkLog(workLog)
            loadCalendar()
        }
    }

    fun removeSelectedDates() {
        viewModelScope.launch {
            val dates = _uiState.value.selectedDates.toList()
            dates.forEach { date ->
                workLogRepository.deleteWorkLogByDate(date)
            }
            _uiState.value = _uiState.value.copy(
                selectedDates = emptySet(),
                isSelectionMode = false,
                isLoading = true
            )
            loadCalendar()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkCalendarScreen(
    viewModel: WorkCalendarViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Work Calendar", fontWeight = FontWeight.Bold) },
                actions = {
                    if (uiState.isSelectionMode) {
                        IconButton(onClick = { viewModel.exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                        IconButton(onClick = { viewModel.selectAllCurrentMonth() }) {
                            Icon(Icons.Default.SelectAll, contentDescription = "Select All")
                        }
                        IconButton(onClick = { viewModel.removeSelectedDates() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove Selected")
                        }
                    } else {
                        IconButton(onClick = { viewModel.enterSelectionMode() }) {
                            Icon(Icons.Default.Edit, contentDescription = "Select Days")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Month navigation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.previousMonth() }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
                }
                Text(
                    text = "${uiState.monthNames[uiState.currentMonth]} ${uiState.currentYear}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { viewModel.nextMonth() }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Summary cards
            uiState.summary?.let { summary ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SummaryChip(label = "Work", value = "${summary.totalWorkDays}", color = Color(0xFF2196F3))
                    SummaryChip(label = "Home", value = "${summary.totalHomeOfficeDays}", color = Color(0xFF9C27B0))
                    SummaryChip(label = "Office", value = "${summary.totalOfficeDays}", color = Color(0xFF4CAF50))
                    SummaryChip(label = "Off", value = "${summary.totalOffDays}", color = Color(0xFFFF9800))
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Total: ${summary.totalWorkHours.toInt()}h work, ${summary.totalOvertimeHours.toInt()}h overtime",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Day headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Calendar grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(uiState.calendarDays) { day ->
                    CalendarDayItem(
                        day = day,
                        isSelected = uiState.selectedDates.contains(day.date),
                        isSelectionMode = uiState.isSelectionMode,
                        onClick = {
                            if (uiState.isSelectionMode) {
                                viewModel.toggleDateSelection(day.date)
                            } else {
                                day.workLog?.let {
                                    viewModel.updateSingleDay(day.date, it.dayType)
                                }
                            }
                        },
                        onLongClick = {
                            if (!uiState.isSelectionMode) {
                                viewModel.toggleDateSelection(day.date)
                                viewModel.enterSelectionMode()
                            }
                        }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                LegendItem(color = Color(0xFF2196F3), label = "Work")
                LegendItem(color = Color(0xFF9C27B0), label = "Home")
                LegendItem(color = Color(0xFF4CAF50), label = "Office")
                LegendItem(color = Color(0xFFFF9800), label = "Off")
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Apply button
            if (uiState.isSelectionMode && uiState.selectedDates.isNotEmpty()) {
                Button(
                    onClick = { viewModel.showDayTypeDialog() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.selectedDates.isNotEmpty()
                ) {
                    Text("Apply to ${uiState.selectedDates.size} day(s)")
                }
            }
        }
    }

    // Day Type Selection Dialog
    if (uiState.showDayTypeDialog) {
        DayTypeSelectionDialog(
            selectedType = uiState.selectedDayType,
            onTypeSelected = { viewModel.setSelectedDayType(it) },
            onConfirm = { viewModel.applyDayTypeToSelectedDates() },
            onDismiss = { viewModel.hideDayTypeDialog() }
        )
    }
}

@Composable
fun SummaryChip(label: String, value: String, color: Color) {
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = color)
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = color)
        }
    }
}

@Composable
fun CalendarDayItem(
    day: WorkCalendarDay,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        day.workLog != null -> getDayTypeColor(day.workLog.dayType)
        else -> Color.Transparent
    }
    
    val textColor = when {
        isSelected -> Color.White
        !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor.copy(alpha = if (day.workLog != null) 0.3f else 0f))
            .then(
                if (day.isToday) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (day.isToday) FontWeight.Bold else FontWeight.Normal,
                color = textColor
            )
            if (day.workLog != null) {
                Text(
                    text = day.workLog.dayType.icon,
                    style = MaterialTheme.typography.labelSmall
                )
            }
            if (isSelectionMode && isSelected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

fun getDayTypeColor(type: WorkDayType): Color {
    return when (type) {
        WorkDayType.WORKDAY -> Color(0xFF2196F3)
        WorkDayType.HOME_OFFICE -> Color(0xFF9C27B0)
        WorkDayType.OFFICE -> Color(0xFF4CAF50)
        WorkDayType.OFF_DAY -> Color(0xFFFF9800)
        WorkDayType.OVERTIME -> Color(0xFFF44336)
        WorkDayType.HOLIDAY -> Color(0xFFE91E63)
        WorkDayType.SICK_LEAVE -> Color(0xFF795548)
        WorkDayType.PAID_LEAVE -> Color(0xFF00BCD4)
        WorkDayType.UNPAID_LEAVE -> Color(0xFF607D8B)
        WorkDayType.BUSINESS_TRIP -> Color(0xFF3F51B5)
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).background(color, CircleShape))
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun DayTypeSelectionDialog(
    selectedType: WorkDayType,
    onTypeSelected: (WorkDayType) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Day Type") },
        text = {
            LazyColumn {
                items(WorkDayType.entries.toList()) { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onTypeSelected(type) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = type == selectedType,
                            onClick = { onTypeSelected(type) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = type.icon, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = type.displayName, style = MaterialTheme.typography.bodyLarge)
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "${type.workHoursDefault.toInt()}h",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
