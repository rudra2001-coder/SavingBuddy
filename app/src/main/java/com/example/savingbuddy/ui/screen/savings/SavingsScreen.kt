package com.example.savingbuddy.ui.screen.savings

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.savingbuddy.domain.model.SavingsGoal
import com.example.savingbuddy.ui.screen.dashboard.formatCurrency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    navController: NavHostController,
    viewModel: SavingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Goal")
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
                    text = "Savings Goals",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            item {
                TotalSavingsCard(
                    totalSavings = uiState.totalSavings,
                    totalTarget = uiState.totalTarget
                )
            }

            if (uiState.goals.isEmpty() && !uiState.isLoading) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Savings,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No savings goals yet",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                            )
                            Text(
                                text = "Tap + to create your first goal",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            )
                        }
                    }
                }
            } else {
                items(uiState.goals) { goal ->
                    SavingsGoalCard(
                        goal = goal,
                        onDelete = { viewModel.deleteGoal(goal) },
                        onAddMoney = { amount -> viewModel.addToGoal(goal.id, amount) }
                    )
                }
            }
        }
    }

    if (uiState.showAddDialog) {
        AddGoalDialog(
            goalName = uiState.newGoalName,
            goalTarget = uiState.newGoalTarget,
            onNameChange = { viewModel.updateNewGoalName(it) },
            onTargetChange = { viewModel.updateNewGoalTarget(it) },
            onDismiss = { viewModel.hideAddDialog() },
            onConfirm = { viewModel.addGoal() }
        )
    }
}

@Composable
fun TotalSavingsCard(totalSavings: Double, totalTarget: Double) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = "Total Savings",
                color = Color.White.copy(alpha = 0.8f),
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = formatCurrency(totalSavings),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
            if (totalTarget > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { (totalSavings / totalTarget).toFloat().coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "of ${formatCurrency(totalTarget)} target",
                    color = Color.White.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun SavingsGoalCard(
    goal: SavingsGoal,
    onDelete: () -> Unit,
    onAddMoney: (Double) -> Unit
) {
    var showAddMoneyDialog by remember { mutableStateOf(false) }

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
                            .background(Color(goal.color).copy(alpha = 0.1f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Savings,
                            contentDescription = null,
                            tint = Color(goal.color)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = goal.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${(goal.progress * 100).toInt()}% complete",
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
                progress = { goal.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(goal.color),
                trackColor = Color(goal.color).copy(alpha = 0.2f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatCurrency(goal.currentAmount),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "of ${formatCurrency(goal.targetAmount)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Button(
                    onClick = { showAddMoneyDialog = true },
                    enabled = !goal.isCompleted,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(goal.color))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }
    }

    if (showAddMoneyDialog) {
        AddMoneyDialog(
            onDismiss = { showAddMoneyDialog = false },
            onConfirm = { amount ->
                onAddMoney(amount)
                showAddMoneyDialog = false
            }
        )
    }
}

@Composable
fun AddGoalDialog(
    goalName: String,
    goalTarget: String,
    onNameChange: (String) -> Unit,
    onTargetChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Savings Goal") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = goalName,
                    onValueChange = onNameChange,
                    label = { Text("Goal Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = goalTarget,
                    onValueChange = { newValue ->
                        if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            onTargetChange(newValue)
                        }
                    },
                    label = { Text("Target Amount (৳)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = goalName.isNotBlank() && (goalTarget.toDoubleOrNull() ?: 0.0) > 0
            ) {
                Text("Create")
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
fun AddMoneyDialog(
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amount by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Money") },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { newValue ->
                    if (newValue.isEmpty() || newValue.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                        amount = newValue
                    }
                },
                label = { Text("Amount (৳)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { amount.toDoubleOrNull()?.let { onConfirm(it) } },
                enabled = (amount.toDoubleOrNull() ?: 0.0) > 0
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