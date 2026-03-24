package com.example.savingbuddy.ui.screen.creditcard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class CreditCardUiState(
    val cards: List<CreditCard> = emptyList(),
    val totalBalance: Double = 0.0,
    val totalAvailableCredit: Double = 0.0,
    val totalCreditLimit: Double = 0.0,
    val showAddDialog: Boolean = false,
    val showPaymentDialog: Boolean = false,
    val selectedCard: CreditCard? = null,
    val paymentAmount: String = "",
    val newCardName: String = "",
    val newCardNumber: String = "",
    val newCreditLimit: String = "",
    val newMinimumPayment: String = "",
    val newDueDate: String = "",
    val newInterestRate: String = "",
    val isLoading: Boolean = true,
    val upcomingPayments: List<UpcomingPayment> = emptyList()
)

data class UpcomingPayment(
    val cardName: String,
    val amount: Double,
    val dueDate: Int,
    val cardId: String
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
                val upcomingPayments = cards.filter { it.currentBalance > 0 && it.dueDate > 0 }
                    .map { card ->
                        UpcomingPayment(
                            cardName = card.name,
                            amount = card.minimumPayment,
                            dueDate = card.dueDate,
                            cardId = card.id
                        )
                    }
                    .sortedBy { it.dueDate }
                
                _uiState.value = _uiState.value.copy(
                    cards = cards,
                    isLoading = false,
                    upcomingPayments = upcomingPayments.take(3)
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
        
        viewModelScope.launch {
            creditCardRepository.getActiveCards().collect { cards ->
                _uiState.value = _uiState.value.copy(
                    totalCreditLimit = cards.sumOf { it.creditLimit }
                )
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
    
    fun showPaymentDialog(card: CreditCard) {
        _uiState.value = _uiState.value.copy(
            showPaymentDialog = true,
            selectedCard = card,
            paymentAmount = ""
        )
    }
    
    fun hidePaymentDialog() {
        _uiState.value = _uiState.value.copy(
            showPaymentDialog = false,
            selectedCard = null,
            paymentAmount = ""
        )
    }
    
    fun updatePaymentAmount(amount: String) {
        _uiState.value = _uiState.value.copy(paymentAmount = amount)
    }
    
    fun makePayment() {
        val amount = _uiState.value.paymentAmount.toDoubleOrNull()
        val card = _uiState.value.selectedCard
        if (amount != null && amount > 0 && card != null) {
            val newBalance = (card.currentBalance - amount).coerceAtLeast(0.0)
            updateCardBalance(card, newBalance)
            hidePaymentDialog()
        }
    }

    fun updateNewCardName(name: String) {
        _uiState.value = _uiState.value.copy(newCardName = name)
    }

    fun updateNewCardNumber(number: String) {
        _uiState.value = _uiState.value.copy(newCardNumber = number.filter { it.isDigit() }.takeLast(4))
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
        val dueDate = state.newDueDate.toIntOrNull() ?: 1
        
        viewModelScope.launch {
            val card = CreditCard(
                id = UUID.randomUUID().toString(),
                name = state.newCardName,
                cardType = com.example.savingbuddy.domain.model.CardType.VISA,
                lastFourDigits = state.newCardNumber.takeLast(4).ifEmpty { "0000" },
                creditLimit = limit,
                currentBalance = 0.0,
                availableCredit = limit,
                minimumPayment = state.newMinimumPayment.toDoubleOrNull() ?: (limit * 0.05),
                dueDate = dueDate.coerceIn(1, 31),
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
    val listState = rememberLazyListState()
    val configuration = LocalConfiguration.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape,
                elevation = FloatingActionButtonDefaults.elevation(8.dp)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Card",
                    modifier = Modifier.size(24.dp)
                )
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF8F9FF),
                            Color(0xFFFFFFFF)
                        )
                    )
                ),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                AnimatedCreditHeader()
            }

            item {
                CreditSummaryCards(
                    totalBalance = uiState.totalBalance,
                    totalAvailableCredit = uiState.totalAvailableCredit,
                    totalCreditLimit = uiState.totalCreditLimit,
                    utilizationRate = if (uiState.totalCreditLimit > 0) 
                        (uiState.totalBalance / uiState.totalCreditLimit * 100).toFloat() 
                        else 0f
                )
            }

            if (uiState.upcomingPayments.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = "Upcoming Payments",
                        icon = Icons.Outlined.Schedule,
                        actionText = "View All"
                    )
                }
                items(uiState.upcomingPayments) { payment ->
                    AnimatedUpcomingPaymentItem(payment = payment)
                }
            }

            if (uiState.cards.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyCreditCardsView()
                }
            } else {
                item {
                    SectionHeader(
                        title = "Your Cards",
                        icon = Icons.Outlined.CreditCard,
                        actionText = null
                    )
                }
                items(uiState.cards) { card ->
                    AnimatedCreditCardItem(
                        card = card,
                        onDelete = { viewModel.deleteCard(card) },
                        onMakePayment = { viewModel.showPaymentDialog(card) },
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
    
    if (uiState.showPaymentDialog && uiState.selectedCard != null) {
        MakePaymentDialog(
            card = uiState.selectedCard!!,
            paymentAmount = uiState.paymentAmount,
            onAmountChange = { viewModel.updatePaymentAmount(it) },
            onDismiss = { viewModel.hidePaymentDialog() },
            onConfirm = { viewModel.makePayment() }
        )
    }
}

@Composable
fun AnimatedCreditHeader() {
    var showHeader by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        showHeader = true
    }
    
    AnimatedVisibility(
        visible = showHeader,
        enter = fadeIn() + slideInVertically()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp)
        ) {
            Text(
                text = "Credit Cards",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Manage your credit cards and track payments",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun CreditSummaryCards(
    totalBalance: Double,
    totalAvailableCredit: Double,
    totalCreditLimit: Double,
    utilizationRate: Float
) {
    var showCards by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        showCards = true
    }
    
    AnimatedVisibility(
        visible = showCards,
        enter = fadeIn() + scaleIn()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CreditStatCard(
                title = "Total Balance",
                value = formatCurrency(totalBalance),
                icon = Icons.Default.AccountBalance,
                color = Color(0xFFF44336),
                modifier = Modifier.weight(1f)
            )
            CreditStatCard(
                title = "Available Credit",
                value = formatCurrency(totalAvailableCredit),
                icon = Icons.Default.CreditCard,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
        }
    }
    
    Spacer(modifier = Modifier.height(8.dp))
    
    AnimatedVisibility(
        visible = showCards,
        enter = fadeIn() + slideInVertically()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                    Text(
                        text = "Credit Utilization",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${String.format("%.1f", utilizationRate)}%",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = when {
                            utilizationRate > 80 -> Color(0xFFF44336)
                            utilizationRate > 50 -> Color(0xFFFF9800)
                            else -> Color(0xFF4CAF50)
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LinearProgressIndicator(
                    progress = { (utilizationRate / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when {
                        utilizationRate > 80 -> Color(0xFFF44336)
                        utilizationRate > 50 -> Color(0xFFFF9800)
                        else -> Color(0xFF4CAF50)
                    },
                    trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = if (utilizationRate > 80) 
                        "High utilization may affect credit score. Try to keep below 30%."
                    else if (utilizationRate > 50)
                        "Moderate utilization. Consider paying down balance."
                    else
                        "Good! Your credit utilization is healthy.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun CreditStatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
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
fun AnimatedUpcomingPaymentItem(payment: UpcomingPayment) {
    var showItem by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        showItem = true
    }
    
    AnimatedVisibility(
        visible = showItem,
        enter = fadeIn() + slideInHorizontally()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                        .clip(CircleShape)
                        .background(Color(0xFFFF9800).copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = Color(0xFFFF9800),
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = payment.cardName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Due on ${getDueDateText(payment.dueDate)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = formatCurrency(payment.amount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                    Text(
                        text = "Minimum Payment",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun AnimatedCreditCardItem(
    card: CreditCard,
    onDelete: () -> Unit,
    onMakePayment: () -> Unit,
    onUpdateBalance: (Double) -> Unit
) {
    var showItem by remember { mutableStateOf(false) }
    var showDetails by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        showItem = true
    }
    
    AnimatedVisibility(
        visible = showItem,
        enter = fadeIn() + slideInHorizontally()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .shadow(4.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    Color(0xFF667eea),
                                    Color(0xFF764ba2)
                                )
                            ),
                            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                        )
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
                                    tint = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = card.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.White.copy(alpha = 0.7f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "**** **** **** ${card.lastFourDigits}",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f),
                            letterSpacing = 2.sp
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Balance",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = formatCurrency(card.currentBalance),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Limit",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = formatCurrency(card.creditLimit),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Utilization",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format("%.1f", card.utilizationPercentage)}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = when {
                                    card.utilizationPercentage > 80 -> Color(0xFFF44336)
                                    card.utilizationPercentage > 50 -> Color(0xFFFF9800)
                                    else -> Color(0xFF4CAF50)
                                }
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        LinearProgressIndicator(
                            progress = { (card.utilizationPercentage.toFloat() / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = when {
                                card.utilizationPercentage > 80 -> Color(0xFFF44336)
                                card.utilizationPercentage > 50 -> Color(0xFFFF9800)
                                else -> Color(0xFF4CAF50)
                            },
                            trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Min. Payment",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = formatCurrency(card.minimumPayment),
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Due Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${card.dueDate}th",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "Interest Rate",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${String.format("%.1f", card.interestRate)}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = onMakePayment,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Payment,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Make Payment")
                        }
                        
                        OutlinedButton(
                            onClick = { showDetails = !showDetails },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                if (showDetails) Icons.Default.KeyboardArrowUp 
                                else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (showDetails) "Hide Details" else "View Details")
                        }
                    }
                    
                    AnimatedVisibility(
                        visible = showDetails,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier.padding(top = 12.dp)
                        ) {
                            Divider()
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "Payment History",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "No payment history available",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text(
                                text = "Tips",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Pay more than minimum to reduce interest\n• Keep utilization below 30% for good credit score\n• Set up auto-pay to avoid late fees",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyCreditCardsView() {
    var showContent by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        showContent = true
    }
    
    AnimatedVisibility(
        visible = showContent,
        enter = fadeIn() + scaleIn()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.CreditCard,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No Credit Cards Added",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Add your first credit card to start tracking",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                textAlign = TextAlign.Center
            )
        }
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
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Add Credit Card",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = cardName,
                        onValueChange = onNameChange,
                        label = { Text("Card Name") },
                        placeholder = { Text("e.g., City Bank Visa") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { onNumberChange(it.filter { c -> c.isDigit() }) },
                        label = { Text("Card Number (last 4 digits)") },
                        placeholder = { Text("1234") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = creditLimit,
                        onValueChange = onLimitChange,
                        label = { Text("Credit Limit") },
                        placeholder = { Text("50000") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = minimumPayment,
                        onValueChange = onMinimumPaymentChange,
                        label = { Text("Minimum Payment") },
                        placeholder = { Text("2500") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = onDueDateChange,
                        label = { Text("Due Date (1-31)") },
                        placeholder = { Text("15") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    OutlinedTextField(
                        value = interestRate,
                        onValueChange = onInterestRateChange,
                        label = { Text("Interest Rate (%)") },
                        placeholder = { Text("24.99") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        enabled = cardName.isNotBlank() && (creditLimit.toDoubleOrNull() ?: 0.0) > 0,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Add Card")
                    }
                }
            }
        }
    }
}

@Composable
fun MakePaymentDialog(
    card: CreditCard,
    paymentAmount: String,
    onAmountChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text(
                    text = "Make Payment",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = card.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Current Balance: ${formatCurrency(card.currentBalance)}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Minimum Payment: ${formatCurrency(card.minimumPayment)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = paymentAmount,
                    onValueChange = onAmountChange,
                    label = { Text("Payment Amount") },
                    placeholder = { Text("Enter amount") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                
                Spacer(modifier = Modifier.height(20.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f),
                        enabled = paymentAmount.toDoubleOrNull()?.let { 
                            it > 0 && it <= card.currentBalance 
                        } ?: false,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Pay Now")
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    actionText: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        
        if (actionText != null) {
            TextButton(
                onClick = { },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = actionText,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}

private fun getDueDateText(dueDate: Int): String {
    val today = Calendar.getInstance()
    val currentDay = today.get(Calendar.DAY_OF_MONTH)
    
    return if (dueDate > currentDay) {
        "in ${dueDate - currentDay} days"
    } else if (dueDate < currentDay) {
        "next month"
    } else {
        "today"
    }
}
