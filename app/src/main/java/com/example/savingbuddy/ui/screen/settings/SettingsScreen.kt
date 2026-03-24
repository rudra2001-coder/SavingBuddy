package com.example.savingbuddy.ui.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.savingbuddy.ui.navigation.Screen
import com.example.savingbuddy.ui.screen.dashboard.formatCurrency
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.savingbuddy.data.backup.BackupManager
import com.example.savingbuddy.data.backup.BackupFile
import com.example.savingbuddy.data.backup.AutoBackupWorker
import com.example.savingbuddy.domain.model.UserPreferences
import com.example.savingbuddy.domain.repository.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

data class SettingsUiState(
    val preferences: UserPreferences? = null,
    val isLoading: Boolean = true,
    val isBackingUp: Boolean = false,
    val isRestoring: Boolean = false,
    val backupMessage: String? = null,
    val showNameDialog: Boolean = false,
    val showIncomeDialog: Boolean = false,
    val showBackupOptionsDialog: Boolean = false,
    val showRestoreOptionsDialog: Boolean = false,
    val backupList: List<BackupFile> = emptyList()
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferencesRepository: UserPreferencesRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadPreferences()
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            preferencesRepository.getPreferences().collect { prefs ->
                if (prefs == null) {
                    // Create default preferences
                    val defaultPrefs = UserPreferences(
                        id = 1,
                        userName = "User",
                        monthlyIncome = 0.0,
                        currency = "BDT",
                        currencySymbol = "৳",
                        autoBackupEnabled = false,
                        backupFrequency = "DAILY",
                        lastBackupDate = null,
                        darkModeEnabled = false,
                        notificationsEnabled = true,
                        defaultAccountId = null,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    preferencesRepository.savePreferences(defaultPrefs)
                    _uiState.value = _uiState.value.copy(preferences = defaultPrefs, isLoading = false)
                } else {
                    _uiState.value = _uiState.value.copy(preferences = prefs, isLoading = false)
                }
            }
        }
    }

    fun showNameDialog() { _uiState.value = _uiState.value.copy(showNameDialog = true) }
    fun hideNameDialog() { _uiState.value = _uiState.value.copy(showNameDialog = false) }
    fun showIncomeDialog() { _uiState.value = _uiState.value.copy(showIncomeDialog = true) }
    fun hideIncomeDialog() { _uiState.value = _uiState.value.copy(showIncomeDialog = false) }

    fun updateUserName(name: String) {
        viewModelScope.launch {
            preferencesRepository.updateUserName(name)
            hideNameDialog()
        }
    }

    fun updateMonthlyIncome(income: Double) {
        viewModelScope.launch {
            preferencesRepository.updateMonthlyIncome(income)
            hideIncomeDialog()
        }
    }

    fun toggleAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateAutoBackup(enabled)
        }
    }

    fun updateBackupFrequency(frequency: String) {
        viewModelScope.launch {
            preferencesRepository.updateBackupFrequency(frequency)
        }
    }

    fun toggleDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateDarkMode(enabled)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateNotifications(enabled)
        }
    }

    fun showBackupOptionsDialog() { _uiState.value = _uiState.value.copy(showBackupOptionsDialog = true) }
    fun hideBackupOptionsDialog() { _uiState.value = _uiState.value.copy(showBackupOptionsDialog = false) }
    fun showRestoreOptionsDialog() { _uiState.value = _uiState.value.copy(showRestoreOptionsDialog = true) }
    fun hideRestoreOptionsDialog() { _uiState.value = _uiState.value.copy(showRestoreOptionsDialog = false) }

    fun createBackup(includeCompression: Boolean = true, includeEncryption: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isBackingUp = true, showBackupOptionsDialog = false)
            try {
                val result = backupManager.createBackup(includeCompression, includeEncryption)
                if (result.success) {
                    preferencesRepository.updateLastBackupDate(System.currentTimeMillis())
                    _uiState.value = _uiState.value.copy(
                        isBackingUp = false,
                        backupMessage = "Backup saved: ${result.fileName}"
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isBackingUp = false,
                        backupMessage = result.errorMessage ?: "Backup failed"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isBackingUp = false,
                    backupMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun createBackupFromUri(uri: Uri, isEncrypted: Boolean = false, isCompressed: Boolean = false) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRestoring = true, showRestoreOptionsDialog = false)
            try {
                val result = backupManager.restoreBackup(uri, isEncrypted, isCompressed)
                _uiState.value = _uiState.value.copy(
                    isRestoring = false,
                    backupMessage = result.message
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRestoring = false,
                    backupMessage = "Error: ${e.message}"
                )
            }
        }
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(backupMessage = null)
    }

    fun loadBackupList() {
        viewModelScope.launch {
            val list = backupManager.getBackupList()
            _uiState.value = _uiState.value.copy(backupList = list)
        }
    }

    fun deleteBackup(filePath: String) {
        viewModelScope.launch {
            backupManager.deleteBackup(filePath)
            loadBackupList()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()) }

    LaunchedEffect(uiState.backupMessage) {
        uiState.backupMessage?.let {
            // Show snackbar
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Profile Section
            item {
                Text("Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.Person,
                        title = "User Name",
                        subtitle = uiState.preferences?.userName ?: "User",
                        onClick = { viewModel.showNameDialog() }
                    )
                    HorizontalDivider()
                    SettingsItem(
                        icon = Icons.Default.AttachMoney,
                        title = "Monthly Income",
                        subtitle = formatCurrency(uiState.preferences?.monthlyIncome ?: 0.0),
                        onClick = { viewModel.showIncomeDialog() }
                    )
                }
            }

            // Backup Section
            item {
                Text("Backup & Restore", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            item {
                SettingsCard {
                    SettingsSwitch(
                        icon = Icons.Default.Backup,
                        title = "Auto Backup",
                        subtitle = "Automatically backup data daily",
                        checked = uiState.preferences?.autoBackupEnabled ?: false,
                        onCheckedChange = { viewModel.toggleAutoBackup(it) }
                    )
                    if (uiState.preferences?.autoBackupEnabled == true) {
                        HorizontalDivider()
                        SettingsItem(
                            icon = Icons.Default.Schedule,
                            title = "Backup Frequency",
                            subtitle = uiState.preferences?.backupFrequency ?: "DAILY",
                            onClick = { /* Show frequency picker */ }
                        )
                    }
                    HorizontalDivider()
                    SettingsItem(
                        icon = Icons.Default.CloudUpload,
                        title = "Backup Now",
                        subtitle = uiState.preferences?.lastBackupDate?.let { "Last: ${dateFormat.format(Date(it))}" } ?: "Never backed up",
                        onClick = { viewModel.showBackupOptionsDialog() }
                    )
                    HorizontalDivider()
                    SettingsItem(
                        icon = Icons.Default.CloudDownload,
                        title = "Restore Backup",
                        subtitle = "Restore from backup file",
                        onClick = { viewModel.showRestoreOptionsDialog() }
                    )
                }
            }

            // App Settings
            item {
                Text("App Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            item {
                SettingsCard {
                    SettingsSwitch(
                        icon = Icons.Default.DarkMode,
                        title = "Dark Mode",
                        subtitle = "Use dark theme",
                        checked = uiState.preferences?.darkModeEnabled ?: false,
                        onCheckedChange = { viewModel.toggleDarkMode(it) }
                    )
                    HorizontalDivider()
                    SettingsSwitch(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "Enable push notifications",
                        checked = uiState.preferences?.notificationsEnabled ?: true,
                        onCheckedChange = { viewModel.toggleNotifications(it) }
                    )
                }
            }

            // About
            item {
                Text("About", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }

            item {
                SettingsCard {
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "About App",
                        subtitle = "Version 1.0.0",
                        onClick = { navController.navigate(Screen.About.route) }
                    )
                }
            }
        }
    }

    // Name Dialog
    if (uiState.showNameDialog) {
        var name by remember { mutableStateOf(uiState.preferences?.userName ?: "") }
        AlertDialog(
            onDismissRequest = { viewModel.hideNameDialog() },
            title = { Text("Edit Name") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Your Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.updateUserName(name) }, enabled = name.isNotBlank()) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideNameDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Income Dialog
    if (uiState.showIncomeDialog) {
        var income by remember { mutableStateOf(uiState.preferences?.monthlyIncome?.toString() ?: "") }
        AlertDialog(
            onDismissRequest = { viewModel.hideIncomeDialog() },
            title = { Text("Monthly Income") },
            text = {
                OutlinedTextField(
                    value = income,
                    onValueChange = { income = it },
                    label = { Text("Amount") },
                    leadingIcon = { Text("৳") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.updateMonthlyIncome(income.toDoubleOrNull() ?: 0.0) }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideIncomeDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Backup Options Dialog
    if (uiState.showBackupOptionsDialog) {
        var compressEnabled by remember { mutableStateOf(true) }
        var encryptEnabled by remember { mutableStateOf(false) }
        
        AlertDialog(
            onDismissRequest = { viewModel.hideBackupOptionsDialog() },
            title = { Text("Create Backup") },
            text = {
                Column {
                    Text("Choose backup options:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = compressEnabled, onCheckedChange = { compressEnabled = it })
                        Text("Compress backup file")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = encryptEnabled, onCheckedChange = { encryptEnabled = it })
                        Text("Encrypt backup (AES-256)")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.createBackup(compressEnabled, encryptEnabled) },
                    enabled = !uiState.isBackingUp
                ) {
                    if (uiState.isBackingUp) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Create Backup")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideBackupOptionsDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Restore Options Dialog
    if (uiState.showRestoreOptionsDialog) {
        val context = LocalContext.current
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        var isCompressed by remember { mutableStateOf(false) }
        var isEncrypted by remember { mutableStateOf(false) }
        
        val filePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument()
        ) { uri ->
            uri?.let {
                selectedUri = it
                viewModel.createBackupFromUri(it, isEncrypted, isCompressed)
            }
        }
        
        AlertDialog(
            onDismissRequest = { viewModel.hideRestoreOptionsDialog() },
            title = { Text("Restore Backup") },
            text = {
                Column {
                    Text("Select backup file format:", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isCompressed, onCheckedChange = { isCompressed = it })
                        Text("Compressed (.sb gzip)")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isEncrypted, onCheckedChange = { isEncrypted = it })
                        Text("Encrypted")
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Tap 'Restore' to select a backup file",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { 
                        filePickerLauncher.launch(arrayOf("*/*"))
                    },
                    enabled = !uiState.isRestoring
                ) {
                    if (uiState.isRestoring) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp))
                    } else {
                        Text("Restore")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.hideRestoreOptionsDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Snackbar for backup messages
    uiState.backupMessage?.let { message ->
        LaunchedEffect(message) {
            // The message is shown as part of the dialog or could use a Snackbar
        }
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SettingsSwitch(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
