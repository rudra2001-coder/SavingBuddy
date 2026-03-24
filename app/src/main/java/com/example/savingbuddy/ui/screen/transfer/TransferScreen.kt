package com.example.savingbuddy.ui.screen.transfer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.savingbuddy.domain.model.Account
import com.example.savingbuddy.ui.screen.dashboard.formatCurrency
import com.example.savingbuddy.ui.theme.BlueNeutral
import com.example.savingbuddy.ui.theme.GreenIncome

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferScreen(
    navController: NavHostController,
    viewModel: TransferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Transfer") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                TransferCard(
                    account = uiState.fromAccount,
                    label = "From",
                    accounts = uiState.accounts,
                    onAccountSelected = { viewModel.selectFromAccount(it) },
                    isFrom = true
                )

                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Transfer",
                    tint = BlueNeutral,
                    modifier = Modifier
                        .size(40.dp)
                        .background(BlueNeutral.copy(alpha = 0.1f), CircleShape)
                        .padding(8.dp)
                )

                TransferCard(
                    account = uiState.toAccount,
                    label = "To",
                    accounts = uiState.accounts,
                    onAccountSelected = { viewModel.selectToAccount(it) },
                    isFrom = false
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "৳",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = BlueNeutral
                        )
                        TextField(
                            value = uiState.amount,
                            onValueChange = { newValue ->
                                if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                                    viewModel.updateAmount(newValue)
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
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = { viewModel.transfer() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = uiState.accounts.size >= 2
                ) {
                    Text("Transfer", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                if (uiState.accounts.size < 2) {
                    Text(
                        text = "Need at least 2 accounts to transfer",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransferCard(
    account: Account?,
    label: String,
    accounts: List<Account>,
    onAccountSelected: (Account) -> Unit,
    isFrom: Boolean
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { expanded = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (account != null) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color(account.color).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = account.name.take(1).uppercase(),
                                fontWeight = FontWeight.Bold,
                                color = Color(account.color)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = formatCurrency(account.balance),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        Text(
                            text = "Select Account",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    accounts.forEach { acc ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(acc.name)
                                    Text(formatCurrency(acc.balance), fontWeight = FontWeight.SemiBold)
                                }
                            },
                            onClick = {
                                onAccountSelected(acc)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}