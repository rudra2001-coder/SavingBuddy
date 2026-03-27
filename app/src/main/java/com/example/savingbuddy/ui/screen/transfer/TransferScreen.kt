package com.example.savingbuddy.ui.screen.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.savingbuddy.domain.model.Account
import com.example.savingbuddy.domain.model.SavingsGoal
import com.example.savingbuddy.ui.screen.dashboard.formatCurrency
import com.example.savingbuddy.ui.theme.BlueNeutral
import com.example.savingbuddy.ui.theme.GreenIncome
import com.example.savingbuddy.ui.theme.RedExpense

enum class TransferType(
    val displayName: String,
    val icon: ImageVector,
    val color: Color,
    val description: String
) {
    ACCOUNT_TO_ACCOUNT(
        "Account to Account",
        Icons.Default.SwapHoriz,
        BlueNeutral,
        "Transfer between your accounts"
    ),
    ACCOUNT_TO_GENERAL_SAVINGS(
        "To General Savings",
        Icons.Default.TrendingUp,
        GreenIncome,
        "Transfer to general savings"
    ),
    GENERAL_SAVINGS_TO_ACCOUNT(
        "From General Savings",
        Icons.Default.TrendingDown,
        RedExpense,
        "Withdraw from general savings"
    ),
    INCOME_TO_SAVINGS(
        "Income to Goal",
        Icons.Default.Flag,
        Color(0xFF4CAF50),
        "Move income to savings goal"
    ),
    SAVINGS_TO_INCOME(
        "Goal to Income",
        Icons.Default.AccountBalance,
        Color(0xFFFF9800),
        "Withdraw from goal to account"
    ),
    ACCOUNT_TO_SAVINGS(
        "Account to Goal",
        Icons.Default.AccountBalance,
        Color(0xFF2196F3),
        "Transfer to savings goal"
    ),
    SAVINGS_TO_SAVINGS(
        "Goal to Goal",
        Icons.Default.Savings,
        Color(0xFFE91E63),
        "Move between savings goals"
    ),
    INCOME_TO_INCOME(
        "Income to Income",
        Icons.Default.Paid,
        Color(0xFF9C27B0),
        "Move income between accounts"
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    navController: NavHostController,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTransferType by remember { mutableStateOf(TransferType.ACCOUNT_TO_ACCOUNT) }

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Transfer Type Selection
            Text(
                text = "Transfer Type",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(TransferType.entries.toList()) { type ->
                    TransferTypeCard(
                        type = type,
                        isSelected = selectedTransferType == type,
                        onClick = {
                            selectedTransferType = type
                            viewModel.setTransferType(type)
                        }
                    )
                }
            }

            HorizontalDivider()

            // Based on transfer type, show different UI
            when (selectedTransferType) {
                TransferType.ACCOUNT_TO_ACCOUNT -> {
                    AccountTransferSection(uiState = uiState, viewModel = viewModel)
                }
                TransferType.ACCOUNT_TO_GENERAL_SAVINGS -> {
                    GeneralSavingsTransferSection(uiState = uiState, viewModel = viewModel, isToSavings = true)
                }
                TransferType.GENERAL_SAVINGS_TO_ACCOUNT -> {
                    GeneralSavingsTransferSection(uiState = uiState, viewModel = viewModel, isToSavings = false)
                }
                TransferType.INCOME_TO_SAVINGS, TransferType.SAVINGS_TO_INCOME -> {
                    IncomeSavingsTransferSection(uiState = uiState, viewModel = viewModel, isIncomeToSavings = selectedTransferType == TransferType.INCOME_TO_SAVINGS)
                }
                TransferType.ACCOUNT_TO_SAVINGS -> {
                    AccountToSavingsSection(uiState = uiState, viewModel = viewModel)
                }
                TransferType.SAVINGS_TO_SAVINGS -> {
                    SavingsToSavingsSection(uiState = uiState, viewModel = viewModel)
                }
                TransferType.INCOME_TO_INCOME -> {
                    IncomeToIncomeSection(uiState = uiState, viewModel = viewModel)
                }
            }

            // Error message
            uiState.errorMessage?.let { error ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = error, color = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.clearError() }) {
                            Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = MaterialTheme.colorScheme.onErrorContainer)
                        }
                    }
                }
            }

            // Confirmation Dialog
            if (uiState.showConfirmation) {
                TransferConfirmationDialog(
                    uiState = uiState,
                    onConfirm = { viewModel.transfer() },
                    onDismiss = { viewModel.hideConfirmation() }
                )
            }
        }
    }
}

@Composable
fun TransferConfirmationDialog(
    uiState: TransferUiState,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val amount = uiState.amount.toDoubleOrNull() ?: 0.0
    
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                Icons.Default.SwapHoriz,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )
        },
        title = { Text("Confirm Transfer", textAlign = TextAlign.Center) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "৳${String.format("%.2f", amount)}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // From
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowUpward, contentDescription = null, tint = RedExpense)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("From", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                when (uiState.transferType) {
                                    TransferType.ACCOUNT_TO_ACCOUNT, TransferType.INCOME_TO_INCOME -> uiState.fromAccount?.name ?: "Select account"
                                    TransferType.INCOME_TO_SAVINGS, TransferType.ACCOUNT_TO_SAVINGS, TransferType.ACCOUNT_TO_GENERAL_SAVINGS -> uiState.fromAccount?.name ?: "Select account"
                                    TransferType.SAVINGS_TO_INCOME, TransferType.GENERAL_SAVINGS_TO_ACCOUNT -> "General Savings"
                                    TransferType.SAVINGS_TO_SAVINGS -> uiState.selectedSavingsGoal?.name ?: "Select savings"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // To
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = null, tint = GreenIncome)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("To", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                when (uiState.transferType) {
                                    TransferType.ACCOUNT_TO_ACCOUNT, TransferType.INCOME_TO_INCOME -> uiState.toAccount?.name ?: "Select account"
                                    TransferType.INCOME_TO_SAVINGS, TransferType.ACCOUNT_TO_SAVINGS, TransferType.SAVINGS_TO_SAVINGS -> uiState.selectedSavingsGoal?.name ?: "Select savings"
                                    TransferType.SAVINGS_TO_INCOME -> uiState.toAccount?.name ?: "Select account"
                                    TransferType.ACCOUNT_TO_GENERAL_SAVINGS -> "General Savings"
                                    TransferType.GENERAL_SAVINGS_TO_ACCOUNT -> uiState.toAccount?.name ?: "Select account"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
                
                if (uiState.note.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Note: ${uiState.note}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirm")
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
fun TransferTypeCard(
    type: TransferType,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(140.dp)
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(2.dp, type.color, RoundedCornerShape(16.dp))
                else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) type.color.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(type.color.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(type.icon, contentDescription = null, tint = type.color, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = type.displayName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun AccountTransferSection(uiState: TransferUiState, viewModel: TransferViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "From Account",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AccountSelector(
            selectedAccount = uiState.fromAccount,
            accounts = uiState.accounts.filter { it.id != uiState.toAccount?.id },
            onSelect = { viewModel.selectFromAccount(it) }
        )

        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = BlueNeutral,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(32.dp)
        )

        Text(
            text = "To Account",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AccountSelector(
            selectedAccount = uiState.toAccount,
            accounts = uiState.accounts.filter { it.id != uiState.fromAccount?.id },
            onSelect = { viewModel.selectToAccount(it) }
        )

        AmountInput(
            amount = uiState.amount,
            onAmountChange = { viewModel.updateAmount(it) },
            availableBalance = uiState.fromAccount?.balance ?: 0.0
        )
    }
}

@Composable
fun IncomeSavingsTransferSection(
    uiState: TransferUiState,
    viewModel: TransferViewModel,
    isIncomeToSavings: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Source section
        if (isIncomeToSavings) {
            Text(
                text = "From Income/Account",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AccountSelector(
                selectedAccount = uiState.fromAccount,
                accounts = uiState.accounts,
                onSelect = { viewModel.selectFromAccount(it) }
            )

            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = GreenIncome,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(32.dp)
            )

            Text(
                text = "To Savings Goal",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SavingsGoalSelector(
                selectedGoal = uiState.selectedSavingsGoal,
                savingsGoals = uiState.savingsGoals,
                onSelect = { viewModel.selectSavingsGoal(it) }
            )
        } else {
            Text(
                text = "From Savings Goal",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            SavingsGoalSelector(
                selectedGoal = uiState.selectedSavingsGoal,
                savingsGoals = uiState.savingsGoals,
                onSelect = { viewModel.selectSavingsGoal(it) }
            )

            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = RedExpense,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(32.dp)
            )

            Text(
                text = "To Account",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AccountSelector(
                selectedAccount = uiState.toAccount,
                accounts = uiState.accounts,
                onSelect = { viewModel.selectToAccount(it) }
            )
        }

        AmountInput(
            amount = uiState.amount,
            onAmountChange = { viewModel.updateAmount(it) },
            availableBalance = if (isIncomeToSavings) uiState.fromAccount?.balance ?: 0.0 else uiState.selectedSavingsGoal?.currentAmount ?: 0.0
        )
    }
}

@Composable
fun AccountToSavingsSection(uiState: TransferUiState, viewModel: TransferViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "From Account",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AccountSelector(
            selectedAccount = uiState.fromAccount,
            accounts = uiState.accounts,
            onSelect = { viewModel.selectFromAccount(it) }
        )

        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(32.dp)
        )

        Text(
            text = "To Savings Goal",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SavingsGoalSelector(
            selectedGoal = uiState.selectedSavingsGoal,
            savingsGoals = uiState.savingsGoals,
            onSelect = { viewModel.selectSavingsGoal(it) }
        )

        AmountInput(
            amount = uiState.amount,
            onAmountChange = { viewModel.updateAmount(it) },
            availableBalance = uiState.fromAccount?.balance ?: 0.0
        )
    }
}

@Composable
fun SavingsToSavingsSection(uiState: TransferUiState, viewModel: TransferViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "From Savings Goal",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SavingsGoalSelector(
            selectedGoal = uiState.selectedSavingsGoal,
            savingsGoals = uiState.savingsGoals,
            onSelect = { viewModel.selectSavingsGoal(it) }
        )

        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = Color(0xFFE91E63),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(32.dp)
        )

        Text(
            text = "To Savings Goal",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SavingsGoalSelector(
            selectedGoal = uiState.toSavingsGoal,
            savingsGoals = uiState.savingsGoals.filter { it.id != uiState.selectedSavingsGoal?.id },
            onSelect = { viewModel.selectToSavingsGoal(it) }
        )

        AmountInput(
            amount = uiState.amount,
            onAmountChange = { viewModel.updateAmount(it) },
            availableBalance = uiState.selectedSavingsGoal?.currentAmount ?: 0.0
        )
    }
}

@Composable
fun IncomeToIncomeSection(uiState: TransferUiState, viewModel: TransferViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "From Account (Income)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AccountSelector(
            selectedAccount = uiState.fromAccount,
            accounts = uiState.accounts,
            onSelect = { viewModel.selectFromAccount(it) }
        )

        Icon(
            Icons.Default.KeyboardArrowDown,
            contentDescription = null,
            tint = Color(0xFF9C27B0),
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(32.dp)
        )

        Text(
            text = "To Account",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        AccountSelector(
            selectedAccount = uiState.toAccount,
            accounts = uiState.accounts.filter { it.id != uiState.fromAccount?.id },
            onSelect = { viewModel.selectToAccount(it) }
        )

        AmountInput(
            amount = uiState.amount,
            onAmountChange = { viewModel.updateAmount(it) },
            availableBalance = uiState.fromAccount?.balance ?: 0.0
        )
    }
}

@Composable
fun GeneralSavingsTransferSection(
    uiState: TransferUiState,
    viewModel: TransferViewModel,
    isToSavings: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (isToSavings) {
            Text(
                text = "From Account",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AccountSelector(
                selectedAccount = uiState.fromAccount,
                accounts = uiState.accounts,
                onSelect = { viewModel.selectFromAccount(it) }
            )

            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = GreenIncome,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(32.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Savings,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "General Savings",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Your general savings bucket",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            AmountInput(
                amount = uiState.amount,
                onAmountChange = { viewModel.updateAmount(it) },
                availableBalance = uiState.fromAccount?.balance ?: 0.0
            )
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Savings,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(40.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "General Savings",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Your general savings bucket",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Icon(
                Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = RedExpense,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(32.dp)
            )

            Text(
                text = "To Account",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AccountSelector(
                selectedAccount = uiState.toAccount,
                accounts = uiState.accounts,
                onSelect = { viewModel.selectToAccount(it) }
            )

            AmountInput(
                amount = uiState.amount,
                onAmountChange = { viewModel.updateAmount(it) },
                availableBalance = 0.0
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelector(
    selectedAccount: Account?,
    accounts: List<Account>,
    onSelect: (Account) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { expanded = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedAccount != null) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(selectedAccount.color).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = selectedAccount.name.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = Color(selectedAccount.color)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = selectedAccount.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(text = formatCurrency(selectedAccount.balance), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text("Select Account", style = MaterialTheme.typography.bodyLarge)
                }
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }

            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                accounts.forEach { acc ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(acc.name)
                                }
                                Text(formatCurrency(acc.balance), fontWeight = FontWeight.SemiBold, color = GreenIncome)
                            }
                        },
                        onClick = {
                            onSelect(acc)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsGoalSelector(
    selectedGoal: SavingsGoal?,
    savingsGoals: List<SavingsGoal>,
    onSelect: (SavingsGoal) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { expanded = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (selectedGoal != null) {
                    Text(text = selectedGoal.icon, style = MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = selectedGoal.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                        Text(
                            text = "${formatCurrency(selectedGoal.currentAmount)} / ${formatCurrency(selectedGoal.targetAmount)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    LinearProgressIndicator(
                        progress = { selectedGoal.progress },
                        modifier = Modifier
                            .width(60.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = Color(selectedGoal.color),
                        trackColor = Color(selectedGoal.color).copy(alpha = 0.2f)
                    )
                } else {
                    Text("Select Savings Goal", style = MaterialTheme.typography.bodyLarge)
                }
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            }

            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                if (savingsGoals.isEmpty()) {
                    DropdownMenuItem(
                        text = { Text("No savings goals yet") },
                        onClick = { expanded = false }
                    )
                } else {
                    savingsGoals.forEach { goal ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(goal.icon)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(goal.name)
                                    }
                                    Text(formatCurrency(goal.currentAmount), fontWeight = FontWeight.SemiBold, color = GreenIncome)
                                }
                            },
                            onClick = {
                                onSelect(goal)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AmountInput(
    amount: String,
    onAmountChange: (String) -> Unit,
    availableBalance: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Amount to Transfer",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "৳",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                TextField(
                    value = amount,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            onAmountChange(newValue)
                        }
                    },
                    textStyle = MaterialTheme.typography.displaySmall.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.widthIn(min = 100.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Available: ${formatCurrency(availableBalance)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Quick amount buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf(100, 500, 1000, 5000).forEach { quickAmount ->
                    FilterChip(
                        selected = false,
                        onClick = { onAmountChange(quickAmount.toString()) },
                        label = { Text("৳$quickAmount") }
                    )
                }
            }
        }
    }
}