package com.example.savingbuddy.ui.screen.creditcard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.savingbuddy.domain.model.CreditCard
import com.example.savingbuddy.ui.screen.dashboard.formatCurrency
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.domain.repository.CreditCardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CreditCardUiState(
    val cards: List<CreditCard> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalAvailableCredit: Double = 0.0,
    val showAddDialog: Boolean = false,
    val newCardName: String = "",
    val newCardNumber: String = "",
    val newCreditLimit: String = "",
    val newMinimumPayment: String = "",
    val newDueDate: String = "",
    val newInterestRate: String = "",
    val isLoading: Boolean = true
)

@HiltViewModel
class CreditCardViewModel @Inject constructor(
    private val creditCardRepository: CreditCardRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditCardUiState())
    val uiState: StateFlow<CreditCardUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            creditCardRepository.getActiveCards().collect { cards ->
                _uiState.value = _uiState.value.copy(
                    cards = cards,
                    isLoading = false
                )
            }
        }
        viewModelScope.launch {
            creditCardRepository.getTotalBalance().collect { balance ->
                _uiState.value = _uiState.value.copy(totalBalance = balance)
            }
        }
        viewModelScope.launch {
            creditCardRepository.getTotalAvailableCredit().collect { credit ->
                _uiState.value = _uiState.value.copy(totalAvailableCredit = credit)
            }
        }
    }

    fun showAddDialog() {
        _uiState.value = _uiState.value.copy(showAddDialog = true)
    }

    fun hideAddDialog() {
        _uiState.value = _uiState.value.copy(
            showAddDialog = false,
            newCardName = "",
            newCardNumber = "",
            newCreditLimit = "",
            newMinimumPayment = "",
            newDueDate = "",
            newInterestRate = ""
        )
    }

    fun updateNewCardName(name: String) {
        _uiState.value = _uiState.value.copy(newCardName = name)
    }

    fun updateNewCardNumber(number: String) {
        _uiState.value = _uiState.value.copy(newCardNumber = number.takeLast(4))
    }

    fun updateNewCreditLimit(limit: String) {
        _uiState.value = _uiState.value.copy(newCreditLimit = limit)
    }

    fun updateNewMinimumPayment(payment: String) {
        _uiState.value = _uiState.value.copy(newMinimumPayment = payment)
    }

    fun updateNewDueDate(date: String) {
        _uiState.value = _uiState.value.copy(newDueDate = date)
    }

    fun updateNewInterestRate(rate: String) {
        _uiState.value = _uiState.value.copy(newInterestRate = rate)
    }

    fun addCard() {
        val state = _uiState.value
        val limit = state.newCreditLimit.toDoubleOrNull() ?: return
        
        viewModelScope.launch {
            val card = CreditCard(
                id = UUID.randomUUID().toString(),
                name = state.newCardName,
                cardType = com.example.savingbuddy.domain.model.CardType.VISA,
                lastFourDigits = state.newCardNumber.takeLast(4),
                creditLimit = limit,
                currentBalance = 0.0,
                availableCredit = limit,
                minimumPayment = state.newMinimumPayment.toDoubleOrNull() ?: 0.0,
                dueDate = state.newDueDate.toIntOrNull() ?: 1,
                interestRate = state.newInterestRate.toDoubleOrNull() ?: 0.0,
                isActive = true,
                createdAt = System.currentTimeMillis(),
                updatedAt = System.currentTimeMillis()
            )
            creditCardRepository.addCard(card)
            hideAddDialog()
        }
    }

    fun deleteCard(card: CreditCard) {
        viewModelScope.launch {
            creditCardRepository.deleteCard(card)
        }
    }

    fun updateCardBalance(card: CreditCard, newBalance: Double) {
        viewModelScope.launch {
            val updatedCard = card.copy(
                currentBalance = newBalance,
                availableCredit = card.creditLimit - newBalance,
                updatedAt = System.currentTimeMillis()
            )
            creditCardRepository.updateCard(updatedCard)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardScreen(
    navController: NavHostController,
    viewModel: CreditCardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Card")
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
                    text = "Credit Cards",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                CreditSummaryCard(
                    totalBalance = uiState.totalBalance,
                    totalAvailableCredit = uiState.totalAvailableCredit
                )
            }

            if (uiState.cards.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CreditCard,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No credit cards added",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            } else {
                items(uiState.cards) { card ->
                    CreditCardItem(
                        card = card,
                        onDelete = { viewModel.deleteCard(card) },
                        onUpdateBalance = { newBalance -> viewModel.updateCardBalance(card, newBalance) }
                    )
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        AddCreditCardDialog(
            cardName = uiState.newCardName,
            cardNumber = uiState.newCardNumber,
            creditLimit = uiState.newCreditLimit,
            minimumPayment = uiState.newMinimumPayment,
            dueDate = uiState.newDueDate,
            interestRate = uiState.newInterestRate,
            onNameChange = { viewModel.updateNewCardName(it) },
            onNumberChange = { viewModel.updateNewCardNumber(it) },
            onLimitChange = { viewModel.updateNewCreditLimit(it) },
            onMinimumPaymentChange = { viewModel.updateNewMinimumPayment(it) },
            onDueDateChange = { viewModel.updateNewDueDate(it) },
            onInterestRateChange = { viewModel.updateNewInterestRate(it) },
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { viewModel.addCard() }
        )
    }
}

@Composable
fun CreditSummaryCard(totalBalance: Double, totalAvailableCredit: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Total Balance",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = formatCurrency(totalBalance),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = formatCurrency(totalAvailableCredit),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun CreditCardItem(
    card: CreditCard,
    onDelete: () -> Unit,
    onUpdateBalance: (Double) -> Unit
) {
    var showUpdateDialog by remember { mutableStateOf(false) }

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
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = card.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "•••• ${card.lastFourDigits}",
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
                progress = { (card.utilizationPercentage / 100.0).coerceIn(0.0, 1.0).toFloat() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = when {
                    card.utilizationPercentage > 80 -> Color.Red
                    card.utilizationPercentage > 50 -> Color(0xFFFF9800)
                    else -> Color(0xFF4CAF50)
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Balance",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatCurrency(card.currentBalance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Limit",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Text(
                        text = formatCurrency(card.creditLimit),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${String.format("%.0f", card.utilizationPercentage)}% used",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                TextButton(onClick = { showUpdateDialog = true }) {
                    Text("Update Balance")
                }
            }
        }
    }

    if (showUpdateDialog) {
        var newBalance by remember { mutableStateOf(card.currentBalance.toString()) }
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = { Text("Update Balance") },
            text = {
                OutlinedTextField(
                    value = newBalance,
                    onValueChange = { newBalance = it },
                    label = { Text("New Balance (৳)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    newBalance.toDoubleOrNull()?.let { onUpdateBalance(it) }
                    showUpdateDialog = false
                }) {
                    Text("Update")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpdateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun AddCreditCardDialog(
    cardName: String,
    cardNumber: String,
    creditLimit: String,
    minimumPayment: String,
    dueDate: String,
    interestRate: String,
    onNameChange: (String) -> Unit,
    onNumberChange: (String) -> Unit,
    onLimitChange: (String) -> Unit,
    onMinimumPaymentChange: (String) -> Unit,
    onDueDateChange: (String) -> Unit,
    onInterestRateChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Credit Card") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = cardName,
                    onValueChange = onNameChange,
                    label = { Text("Card Name") },
                    placeholder = { Text("e.g., City Bank Visa") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = cardNumber,
                    onValueChange = { onNumberChange(it.filter { c -> c.isDigit() }) },
                    label = { Text("Card Number (last 4 digits)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = creditLimit,
                    onValueChange = { onLimitChange(it) },
                    label = { Text("Credit Limit (৳)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = minimumPayment,
                    onValueChange = { onMinimumPaymentChange(it) },
                    label = { Text("Minimum Payment (৳)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = dueDate,
                    onValueChange = { onDueDateChange(it) },
                    label = { Text("Due Date (1-31)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = interestRate,
                    onValueChange = { onInterestRateChange(it) },
                    label = { Text("Interest Rate (%)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = cardName.isNotBlank() && (creditLimit.toDoubleOrNull() ?: 0.0) > 0
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
