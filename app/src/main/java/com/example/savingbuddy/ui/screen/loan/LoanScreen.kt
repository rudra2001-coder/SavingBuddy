package com.example.savingbuddy.ui.screen.loan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.savingbuddy.domain.model.Loan
import com.example.savingbuddy.domain.model.LoanType
import com.example.savingbuddy.ui.screen.dashboard.formatCurrency
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.repository.LoanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class LoanUiState(
    val loans: List<Loan> = emptyList(),
    val totalDebt: Double = 0.0,
    val showAddDialog: Boolean = false,
    val newLoanName: String = "",
    val newLenderName: String = "",
    val newOriginalAmount: String = "",
    val newMonthlyPayment: String = "",
    val newInterestRate: String = "",
    val newLoanType: LoanType = LoanType.PERSONAL,
    val isLoading: Boolean = true
)

@HiltViewModel
class LoanViewModel @Inject constructor(
    private val loanRepository: LoanRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoanUiState())
    val uiState: StateFlow<LoanUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            loanRepository.getActiveLoans().collect { loans ->
                _uiState.value = _uiState.value.copy(
                    loans = loans,
                    isLoading = false
                )
            }
        }
        viewModelScope.launch {
            loanRepository.getTotalDebt().collect { debt ->
                _uiState.value = _uiState.value.copy(totalDebt = debt)
            }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = false,
            newLoanName = "",
            newLenderName = "",
            newOriginalAmount = "",
            newMonthlyPayment = "",
            newInterestRate = "",
            newLoanType = LoanType.PERSONAL
        )
    }

    fun updateNewLoanName(name: String) {
        _uiState.value = _uiState.value.copy(newLoanName = name)
    }

    fun updateNewLenderName(name: String) {
        _uiState.value = _uiState.value.copy(newLenderName = name)
    }

    fun updateNewOriginalAmount(amount: String) {
        _uiState.value = _uiState.value.copy(newOriginalAmount = amount)
    }

    fun updateNewMonthlyPayment(payment: String) {
        _uiState.value = _uiState.value.copy(newMonthlyPayment = payment)
    }

    fun updateNewInterestRate(rate: String) {
        _uiState.value = _uiState.value.copy(newInterestRate = rate)
    }

    fun updateNewLoanType(type: LoanType) {
        _uiState.value = _uiState.value.copy(newLoanType = type)
    }

    fun addLoan() {
        val state = _uiState.value
        val originalAmount = state.newOriginalAmount.toDoubleOrNull() ?: return

        viewModelScope.launch {
            val loan = Loan(
                id = UUID.randomUUID().toString(),
                name = state.newLoanName,
                lenderName = state.newLenderName,
                originalAmount = originalAmount,
                remainingAmount = originalAmount,
                monthlyPayment = state.newMonthlyPayment.toDoubleOrNull() ?: 0.0,
                interestRate = state.newInterestRate.toDoubleOrNull() ?: 0.0,
                loanType = state.newLoanType,
                startDate = System.currentTimeMillis(),
                endDate = null,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            loanRepository.addLoan(loan)
            hideAddDialog()
        }
    }

    fun deleteLoan(loan: Loan) {
        viewModelScope.launch {
            loanRepository.deleteLoan(loan)
        }
    }

    fun updateRemainingAmount(loan: Loan, newAmount: Double) {
        viewModelScope.launch {
            val updatedLoan = loan.copy(
                remainingAmount = newAmount,
                updatedAt = System.currentTimeMillis()
            )
            loanRepository.updateLoan(updatedLoan)
        }
    }

    fun makePayment(loan: Loan, amount: Double) {
        viewModelScope.launch {
            val newRemaining = (loan.remainingAmount - amount).coerceAtLeast(0.0)
            val updatedLoan = loan.copy(
                remainingAmount = newRemaining,
                updatedAt = System.currentTimeMillis()
            )
            loanRepository.updateLoan(updatedLoan)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoanScreen(
    navController: NavHostController,
    viewModel: LoanViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Loan")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Loans & Debts",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                LoanSummaryCard(totalDebt = uiState.totalDebt)
            }

            if (uiState.loans.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No loans tracked",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Tap + to add a loan",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            } else {
                items(uiState.loans) { loan ->
                    LoanItem(
                        loan = loan,
                        onDelete = { viewModel.deleteLoan(loan) },
                        onPayment = { amount -> viewModel.makePayment(loan, amount) },
                        onUpdateRemaining = { newAmount -> viewModel.updateRemainingAmount(loan, newAmount) }
                    )
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        AddLoanDialog(
            loanName = uiState.newLoanName,
            lenderName = uiState.newLenderName,
            originalAmount = uiState.newOriginalAmount,
            monthlyPayment = uiState.newMonthlyPayment,
            interestRate = uiState.newInterestRate,
            loanType = uiState.newLoanType,
            onNameChange = { viewModel.updateNewLoanName(it) },
            onLenderChange = { viewModel.updateNewLenderName(it) },
            onAmountChange = { viewModel.updateNewOriginalAmount(it) },
            onPaymentChange = { viewModel.updateNewMonthlyPayment(it) },
            onInterestChange = { viewModel.updateNewInterestRate(it) },
            onTypeChange = { viewModel.updateNewLoanType(it) },
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { viewModel.addLoan() }
        )
    }
}

@Composable
fun LoanSummaryCard(totalDebt: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFB71C1C)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Total Debt",
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatCurrency(totalDebt),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LoanItem(
    loan: Loan,
    onDelete: () -> Unit,
    onPayment: (Double) -> Unit,
    onUpdateRemaining: (Double) -> Unit
) {
    var showPaymentDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0xFF1976D2).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = Color(0xFF1976D2)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = loan.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = loan.lenderName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { loan.progress.toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFF4CAF50).copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Remaining",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatCurrency(loan.remainingAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Original",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatCurrency(loan.originalAmount),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${(loan.progress * 100).toInt()}% paid off",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Button(
                    onClick = { showPaymentDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Pay")
                }
            }
        }
    }

    if (showPaymentDialog) {
        var paymentAmount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showPaymentDialog = false },
            title = { Text("Make Payment") },
            text = {
                Column {
                    Text("Remaining: ${formatCurrency(loan.remainingAmount)}")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = paymentAmount,
                        onValueChange = { paymentAmount = it },
                        label = { Text("Payment Amount (৳)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    paymentAmount.toDoubleOrNull()?.let { onPayment(it) }
                    showPaymentDialog = false
                }) {
                    Text("Pay")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanDialog(
    loanName: String,
    lenderName: String,
    originalAmount: String,
    monthlyPayment: String,
    interestRate: String,
    loanType: LoanType,
    onNameChange: (String) -> Unit,
    onLenderChange: (String) -> Unit,
    onAmountChange: (String) -> Unit,
    onPaymentChange: (String) -> Unit,
    onInterestChange: (String) -> Unit,
    onTypeChange: (LoanType) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Loan") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = loanName,
                    onValueChange = onNameChange,
                    label = { Text("Loan Name") },
                    placeholder = { Text("e.g., Home Loan") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = lenderName,
                    onValueChange = onLenderChange,
                    label = { Text("Lender Name") },
                    placeholder = { Text("e.g., BRAC Bank") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = loanType.name,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Loan Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        LoanType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.name) },
                                onClick = {
                                    onTypeChange(type)
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = originalAmount,
                    onValueChange = onAmountChange,
                    label = { Text("Loan Amount (৳)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = monthlyPayment,
                    onValueChange = onPaymentChange,
                    label = { Text("Monthly Payment (৳)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = interestRate,
                    onValueChange = onInterestChange,
                    label = { Text("Interest Rate (%)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = loanName.isNotBlank() && (originalAmount.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
