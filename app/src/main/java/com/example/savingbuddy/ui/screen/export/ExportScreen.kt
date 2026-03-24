package com.example.savingbuddy.ui.screen.export

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.data.export.ExportManager
import com.example.savingbuddy.data.export.ExportResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class ExportUiState(
    val isExporting: Boolean = false,
    val exportMessage: String? = null,
    val showDateRangeDialog: Boolean = false,
    val showMonthPickerDialog: Boolean = false
)

@HiltViewModel
class ExportViewModel @Inject constructor(
    private val exportManager: ExportManager
) : ViewModel() {
    private val _uiState = MutableStateFlow(ExportUiState())
    val uiState: StateFlow<ExportUiState> = _uiState.asStateFlow()

    fun showDateRangeDialog() { _uiState.value = _uiState.value.copy(showDateRangeDialog = true) }
    fun hideDateRangeDialog() { _uiState.value = _uiState.value.copy(showDateRangeDialog = false) }
    
    fun showMonthPickerDialog() { _uiState.value = _uiState.value.copy(showMonthPickerDialog = true) }
    fun hideMonthPickerDialog() { _uiState.value = _uiState.value.copy(showMonthPickerDialog = false) }

    fun exportTransactions(startDate: Long? = null, endDate: Long? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, showDateRangeDialog = false)
            val result = exportManager.exportTransactionsCsv(startDate, endDate)
            _uiState.value = _uiState.value.copy(
                isExporting = false,
                exportMessage = if (result.success) "Exported ${result.recordCount} transactions to ${result.fileName}"
                               else "Export failed: ${result.errorMessage}"
            )
        }
    }

    fun exportAccounts() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            val result = exportManager.exportAccountsCsv()
            _uiState.value = _uiState.value.copy(
                isExporting = false,
                exportMessage = if (result.success) "Exported ${result.recordCount} accounts to ${result.fileName}"
                               else "Export failed: ${result.errorMessage}"
            )
        }
    }

    fun exportSavingsGoals() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            val result = exportManager.exportSavingsGoalsCsv()
            _uiState.value = _uiState.value.copy(
                isExporting = false,
                exportMessage = if (result.success) "Exported ${result.recordCount} goals to ${result.fileName}"
                               else "Export failed: ${result.errorMessage}"
            )
        }
    }

    fun exportBudgets(month: Int, year: Int) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true, showMonthPickerDialog = false)
            val result = exportManager.exportBudgetsCsv(month, year)
            _uiState.value = _uiState.value.copy(
                isExporting = false,
                exportMessage = if (result.success) "Exported ${result.recordCount} budgets to ${result.fileName}"
                               else "Export failed: ${result.errorMessage}"
            )
        }
    }

    fun exportFullBackup() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isExporting = true)
            val result = exportManager.exportFullBackupCsv()
            _uiState.value = _uiState.value.copy(
                isExporting = false,
                exportMessage = if (result.success) "Exported all data to ${result.fileName}"
                               else "Export failed: ${result.errorMessage}"
            )
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(exportMessage = null)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportScreen(
    viewModel: ExportViewModel = hiltViewModel()
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
                text = "Export Data",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Text(
                text = "Export your data to CSV files for backup or analysis",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            ExportOptionCard(
                icon = Icons.Default.SwapHoriz,
                title = "Transactions",
                description = "Export all transactions with dates, categories, amounts",
                onClick = { viewModel.showDateRangeDialog() },
                isLoading = uiState.isExporting
            )
        }

        item {
            ExportOptionCard(
                icon = Icons.Default.AccountBalance,
                title = "Accounts",
                description = "Export account names and balances",
                onClick = { viewModel.exportAccounts() },
                isLoading = uiState.isExporting
            )
        }

        item {
            ExportOptionCard(
                icon = Icons.Default.Savings,
                title = "Savings Goals",
                description = "Export savings goals with progress",
                onClick = { viewModel.exportSavingsGoals() },
                isLoading = uiState.isExporting
            )
        }

        item {
            ExportOptionCard(
                icon = Icons.Default.PieChart,
                title = "Budgets",
                description = "Export monthly budget limits",
                onClick = { viewModel.showMonthPickerDialog() },
                isLoading = uiState.isExporting
            )
        }

        item {
            HorizontalDivider()
        }

        item {
            ExportOptionCard(
                icon = Icons.Default.Backup,
                title = "Full Backup Export",
                description = "Export all data (accounts, transactions, goals, budgets)",
                onClick = { viewModel.exportFullBackup() },
                isLoading = uiState.isExporting,
                isHighlighted = true
            )
        }
    }

    // Date Range Dialog
    if (uiState.showDateRangeDialog) {
        var startDate by remember { mutableStateOf("") }
        var endDate by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { viewModel.hideDateRangeDialog() },
            title = { Text("Export Transactions") },
            text = {
                Column {
                    Text("Enter date range (optional):")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("Start (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("End (YYYY-MM-DD)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val start = try { sdf.parse(startDate)?.time } catch (e: Exception) { null }
                        val end = try { sdf.parse(endDate)?.time } catch (e: Exception) { null }
                        viewModel.exportTransactions(start, end)
                    }
                ) {
                    Text("Export All")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideDateRangeDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Month Picker Dialog
    if (uiState.showMonthPickerDialog) {
        var selectedMonth by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.MONTH)) }
        var selectedYear by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.YEAR)) }

        AlertDialog(
            onDismissRequest = { viewModel.hideMonthPickerDialog() },
            title = { Text("Select Month") },
            text = {
                Column {
                    Text("Year: $selectedYear")
                    Slider(
                        value = selectedYear.toFloat(),
                        onValueChange = { selectedYear = it.toInt() },
                        valueRange = 2020f..2030f,
                        steps = 9
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Month: ${selectedMonth + 1}")
                    Slider(
                        value = selectedMonth.toFloat(),
                        onValueChange = { selectedMonth = it.toInt() },
                        valueRange = 0f..11f,
                        steps = 10
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.exportBudgets(selectedMonth, selectedYear) }) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideMonthPickerDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Export message snackbar
    uiState.exportMessage?.let { message ->
        LaunchedEffect(message) {
            // Could show a Snackbar here
        }
    }
}

@Composable
fun ExportOptionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    isHighlighted: Boolean = false
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isHighlighted) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = if (isHighlighted) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            } else {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}