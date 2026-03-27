package com.example.savingbuddy.data.backup

import android.content.Context
import android.net.Uri
import androidx.work.*
import com.example.savingbuddy.data.local.SavingBuddyDatabase
import com.example.savingbuddy.data.local.dao.*
import com.example.savingbuddy.data.local.dao.WorkLogDao
import com.example.savingbuddy.data.local.entity.AccountEntity
import com.example.savingbuddy.data.local.entity.BudgetEntity
import com.example.savingbuddy.data.local.entity.CategoryEntity
import com.example.savingbuddy.data.local.entity.CreditCardEntity
import com.example.savingbuddy.data.local.entity.FocusSessionEntity
import com.example.savingbuddy.data.local.entity.HabitEntity
import com.example.savingbuddy.data.local.entity.AchievementEntity
import com.example.savingbuddy.data.local.entity.HealthEntryEntity
import com.example.savingbuddy.data.local.entity.JournalEntryEntity
import com.example.savingbuddy.data.local.entity.LoanEntity
import com.example.savingbuddy.data.local.entity.MindfulSessionEntity
import com.example.savingbuddy.data.local.entity.SavingsGoalEntity
import com.example.savingbuddy.data.local.entity.TaskEntity
import com.example.savingbuddy.data.local.entity.TransactionEntity
import com.example.savingbuddy.data.local.entity.UserPreferencesEntity
import com.example.savingbuddy.data.local.entity.WorkLogEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.*
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.Deflater
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: SavingBuddyDatabase,
    private val accountDao: AccountDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val budgetDao: BudgetDao,
    private val loanDao: LoanDao,
    private val creditCardDao: CreditCardDao,
    private val healthDao: HealthDao,
    private val journalDao: JournalDao,
    private val habitDao: HabitDao,
    private val achievementDao: AchievementDao,
    private val focusSessionDao: FocusSessionDao,
    private val taskDao: TaskDao,
    private val mindfulSessionDao: MindfulSessionDao,
    private val userPreferencesDao: UserPreferencesDao,
    private val workLogDao: WorkLogDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    private val backupFileNameFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
    
    private val encryptionKey: ByteArray by lazy {
        val prefs = context.getSharedPreferences("backup_prefs", Context.MODE_PRIVATE)
        val storedKey = prefs.getString("encryption_key", null)
        if (storedKey != null) {
            android.util.Base64.decode(storedKey, android.util.Base64.DEFAULT)
        } else {
            val newKey = ByteArray(32) { (0..255).random().toByte() }
            prefs.edit().putString("encryption_key", android.util.Base64.encodeToString(newKey, android.util.Base64.DEFAULT)).apply()
            newKey
        }
    }

    suspend fun createBackup(includeCompression: Boolean = true, includeEncryption: Boolean = false): BackupResult = withContext(Dispatchers.IO) {
        try {
            val backup = createBackupJson()
            val jsonString = backup.toString()
            
            val fileName = "savingbuddy_backup_${backupFileNameFormat.format(Date())}.sb"
            val outputFile = File(context.getExternalFilesDir(null), fileName)
            
            val outputStream: OutputStream = when {
                includeCompression && includeEncryption -> {
                    val gzBytes = compressToGzip(jsonString.toByteArray(Charsets.UTF_8))
                    val encryptedBytes = encrypt(gzBytes)
                    FileOutputStream(outputFile)
                }
                includeCompression -> {
                    GZIPOutputStream(FileOutputStream(outputFile))
                }
                else -> FileOutputStream(outputFile)
            }
            
            outputStream.use {
                if (includeEncryption && !includeCompression) {
                    val encryptedBytes = encrypt(jsonString.toByteArray(Charsets.UTF_8))
                    it.write(encryptedBytes)
                } else {
                    it.write(jsonString.toByteArray(Charsets.UTF_8))
                }
            }
            
            BackupResult(
                success = true,
                filePath = outputFile.absolutePath,
                fileName = fileName,
                sizeBytes = outputFile.length(),
                isCompressed = includeCompression,
                isEncrypted = includeEncryption
            )
        } catch (e: Exception) {
            BackupResult(success = false, errorMessage = "Backup failed: ${e.message}")
        }
    }

    suspend fun restoreBackup(inputUri: Uri, isEncrypted: Boolean = false, isCompressed: Boolean = false): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(inputUri)
                ?: return@withContext RestoreResult(success = false, message = "Could not open file")
            
            val jsonString: String = inputStream.use { stream ->
                val bytes = stream.readBytes()
                when {
                    isEncrypted && isCompressed -> {
                        val decrypted = decrypt(bytes)
                        String(decompressGzip(decrypted), Charsets.UTF_8)
                    }
                    isEncrypted -> String(decrypt(bytes), Charsets.UTF_8)
                    isCompressed -> String(decompressGzip(bytes), Charsets.UTF_8)
                    else -> String(bytes, Charsets.UTF_8)
                }
            }
            
            restoreBackup(jsonString)
        } catch (e: Exception) {
            RestoreResult(success = false, message = "Restore failed: ${e.message}")
        }
    }

    suspend fun restoreBackupFromFile(file: File, isEncrypted: Boolean = false, isCompressed: Boolean = false): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val jsonString = file.inputStream().use { stream ->
                val bytes = stream.readBytes()
                when {
                    isEncrypted && isCompressed -> {
                        val decrypted = decrypt(bytes)
                        String(decompressGzip(decrypted), Charsets.UTF_8)
                    }
                    isEncrypted -> String(decrypt(bytes), Charsets.UTF_8)
                    isCompressed -> String(decompressGzip(bytes), Charsets.UTF_8)
                    else -> String(bytes, Charsets.UTF_8)
                }
            }
            restoreBackup(jsonString)
        } catch (e: Exception) {
            RestoreResult(success = false, message = "Restore failed: ${e.message}")
        }
    }

    suspend fun getBackupList(): List<BackupFile> = withContext(Dispatchers.IO) {
        val backupDir = context.getExternalFilesDir(null)
        backupDir?.listFiles { _, name -> name.startsWith("savingbuddy_backup_") && name.endsWith(".sb") }
            ?.map { file ->
                BackupFile(
                    name = file.name,
                    path = file.absolutePath,
                    sizeBytes = file.length(),
                    createdAt = file.lastModified()
                )
            }
            ?.sortedByDescending { it.createdAt }
            ?: emptyList()
    }

    suspend fun deleteBackup(filePath: String): Boolean = withContext(Dispatchers.IO) {
        try {
            File(filePath).delete()
        } catch (e: Exception) {
            false
        }
    }

    private fun compressToGzip(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }

    private fun decompressGzip(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        java.util.zip.GZIPInputStream(ByteArrayInputStream(data)).use { it.copyTo(bos) }
        return bos.toByteArray()
    }

    private fun encrypt(data: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val iv = ByteArray(12) { (0..255).random().toByte() }
        val gcmSpec = GCMParameterSpec(128, iv)
        val keySpec = SecretKeySpec(encryptionKey, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val encrypted = cipher.doFinal(data)
        
        val result = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, result, 0, iv.size)
        System.arraycopy(encrypted, 0, result, iv.size, encrypted.size)
        return result
    }

    private fun decrypt(data: ByteArray): ByteArray {
        val iv = data.copyOfRange(0, 12)
        val encrypted = data.copyOfRange(12, data.size)
        
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        val keySpec = SecretKeySpec(encryptionKey, "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        return cipher.doFinal(encrypted)
    }

    private suspend fun createBackupJson(): JSONObject {
        val backup = JSONObject()
        val timestamp = System.currentTimeMillis()

        backup.put("version", 2)
        backup.put("appName", "SavingBuddy")
        backup.put("createdAt", timestamp)
        backup.put("createdAtFormatted", dateFormat.format(Date(timestamp)))

        backup.put("accounts", accountDao.getAllAccounts().first().toJsonArray())
        backup.put("transactions", transactionDao.getAllTransactions().first().toJsonArray())
        backup.put("categories", categoryDao.getAllCategories().first().toJsonArray())
        backup.put("savingsGoals", savingsGoalDao.getAllSavingsGoals().first().toJsonArray())
        backup.put("budgets", budgetDao.getBudgetsForMonth(Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.YEAR)).first().toJsonArray())
        backup.put("loans", loanDao.getAllLoans().first().toJsonArray())
        backup.put("creditCards", creditCardDao.getAllCards().first().toJsonArray())
        backup.put("healthEntries", healthDao.getHealthEntries(0, Long.MAX_VALUE).first().toJsonArray())
        backup.put("journalEntries", journalDao.getAllEntries().first().toJsonArray())
        backup.put("habits", habitDao.getAllHabits().first().toJsonArray())
        backup.put("achievements", achievementDao.getAllAchievements().first().toJsonArray())
        backup.put("focusSessions", focusSessionDao.getSessionsForDateRange(0, Long.MAX_VALUE).first().toJsonArray())
        backup.put("tasks", taskDao.getTasksForDateRange(0, Long.MAX_VALUE).first().toJsonArray())
        backup.put("mindfulSessions", mindfulSessionDao.getSessionsForDateRange(0, Long.MAX_VALUE).first().toJsonArray())
        backup.put("workLogs", workLogDao.getAllWorkLogs().first().toJsonArray())
        
        val prefs = userPreferencesDao.getPreferencesSync()
        if (prefs != null) {
            backup.put("userPreferences", JSONObject().apply {
                put("userName", prefs.userName)
                put("monthlyIncome", prefs.monthlyIncome)
                put("currency", prefs.currency)
                put("currencySymbol", prefs.currencySymbol)
                put("darkModeEnabled", prefs.darkModeEnabled)
                put("notificationsEnabled", prefs.notificationsEnabled)
            })
        }

        return backup
    }

    suspend fun restoreBackup(jsonString: String): RestoreResult = withContext(Dispatchers.IO) {
        try {
            val backup = JSONObject(jsonString)
            val version = backup.optInt("version", 1)

            // Clear existing data
            clearAllData()

            // Restore data
            restoreAccounts(backup.getJSONArray("accounts"))
            restoreCategories(backup.getJSONArray("categories"))
            restoreTransactions(backup.getJSONArray("transactions"))
            restoreSavingsGoals(backup.getJSONArray("savingsGoals"))
            restoreBudgets(backup.getJSONArray("budgets"))
            restoreLoans(backup.getJSONArray("loans"))
            restoreCreditCards(backup.getJSONArray("creditCards"))
            restoreHealthEntries(backup.getJSONArray("healthEntries"))
            restoreJournalEntries(backup.getJSONArray("journalEntries"))
            restoreHabits(backup.getJSONArray("habits"))
            restoreAchievements(backup.getJSONArray("achievements"))
            restoreFocusSessions(backup.getJSONArray("focusSessions"))
            restoreTasks(backup.getJSONArray("tasks"))
            restoreMindfulSessions(backup.getJSONArray("mindfulSessions"))
            restoreWorkLogs(backup.optJSONArray("workLogs"))

            if (backup.has("userPreferences")) {
                restoreUserPreferences(backup.getJSONObject("userPreferences"))
            }

            RestoreResult(success = true, message = "Backup restored successfully")
        } catch (e: Exception) {
            RestoreResult(success = false, message = "Error restoring backup: ${e.message}")
        }
    }

    private suspend fun clearAllData() {
        transactionDao.deleteAllTransactions()
        accountDao.deleteAllAccounts()
        categoryDao.deleteAllCategories()
        savingsGoalDao.deleteAllSavingsGoals()
        budgetDao.deleteAllBudgets()
        loanDao.deleteAllLoans()
        creditCardDao.deleteAllCards()
        healthDao.deleteAllEntries()
        journalDao.deleteAllEntries()
        habitDao.deleteAllHabits()
        achievementDao.deleteAllAchievements()
        focusSessionDao.deleteAllSessions()
        taskDao.deleteAllTasks()
        mindfulSessionDao.deleteAllSessions()
        workLogDao.deleteAllWorkLogs()
    }

    private suspend fun restoreAccounts(array: JSONArray) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            accountDao.insertAccount(AccountEntity(
                id = obj.getString("id"),
                name = obj.getString("name"),
                balance = obj.getDouble("balance"),
                icon = obj.optString("icon", "💰"),
                color = obj.optLong("color", 0xFF4CAF50),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                isSynced = false
            ))
        }
    }

    private suspend fun restoreCategories(array: JSONArray) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            categoryDao.insertCategory(CategoryEntity(
                id = obj.getString("id"),
                name = obj.getString("name"),
                icon = obj.optString("icon", "📁"),
                color = obj.optLong("color", 0xFF4CAF50),
                type = obj.getString("type"),
                isDefault = obj.optBoolean("isDefault", false)
            ))
        }
    }

    private suspend fun restoreTransactions(array: JSONArray) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            transactionDao.insertTransaction(TransactionEntity(
                id = obj.getString("id"),
                amount = obj.getDouble("amount"),
                type = obj.getString("type"),
                categoryId = obj.getString("categoryId"),
                accountId = obj.getString("accountId"),
                note = obj.optString("note"),
                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis()),
                isSynced = false
            ))
        }
    }

    private suspend fun restoreSavingsGoals(array: JSONArray) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            savingsGoalDao.insertSavingsGoal(SavingsGoalEntity(
                id = obj.getString("id"),
                name = obj.getString("name"),
                targetAmount = obj.getDouble("targetAmount"),
                currentAmount = obj.getDouble("currentAmount"),
                icon = obj.optString("icon", "💰"),
                color = obj.optLong("color", 0xFF4CAF50),
                deadline = obj.optLong("deadline"),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            ))
        }
    }

    private suspend fun restoreBudgets(array: JSONArray) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            budgetDao.insertBudget(BudgetEntity(
                id = obj.getString("id"),
                categoryId = obj.getString("categoryId"),
                monthlyLimit = obj.getDouble("monthlyLimit"),
                month = obj.getInt("month"),
                year = obj.getInt("year"),
                alertThreshold = obj.optDouble("alertThreshold", 0.8),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            ))
        }
    }

    private suspend fun restoreLoans(array: JSONArray) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            loanDao.insertLoan(LoanEntity(
                id = obj.getString("id"),
                name = obj.getString("name"),
                lenderName = obj.getString("lenderName"),
                originalAmount = obj.getDouble("originalAmount"),
                remainingAmount = obj.getDouble("remainingAmount"),
                monthlyPayment = obj.getDouble("monthlyPayment"),
                interestRate = obj.getDouble("interestRate"),
                loanType = obj.getString("loanType"),
                startDate = obj.optLong("startDate", System.currentTimeMillis()),
                endDate = obj.optLong("endDate", 0).takeIf { it > 0 },
                isActive = obj.optBoolean("isActive", true),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            ))
        }
    }

    private suspend fun restoreCreditCards(array: JSONArray) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            creditCardDao.insertCard(CreditCardEntity(
                id = obj.getString("id"),
                name = obj.getString("name"),
                cardType = obj.getString("cardType"),
                lastFourDigits = obj.getString("lastFourDigits"),
                creditLimit = obj.getDouble("creditLimit"),
                currentBalance = obj.getDouble("currentBalance"),
                availableCredit = obj.getDouble("availableCredit"),
                minimumPayment = obj.getDouble("minimumPayment"),
                dueDate = obj.getInt("dueDate"),
                interestRate = obj.getDouble("interestRate"),
                isActive = obj.optBoolean("isActive", true),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            ))
        }
    }

    private suspend fun restoreHealthEntries(array: JSONArray) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            healthDao.insertEntry(HealthEntryEntity(
                id = obj.getString("id"),
                date = obj.getLong("date"),
                sleepHours = obj.optDouble("sleepHours", 0.0).toFloat(),
                sleepQuality = obj.optInt("sleepQuality", 3),
                waterIntake = obj.optDouble("waterIntake", 0.0).toFloat(),
                steps = obj.optInt("steps", 0).takeIf { it > 0 },
                workHours = obj.optDouble("workHours", 0.0).toFloat(),
                breakTime = obj.optDouble("breakTime", 0.0).toFloat(),
                mood = obj.optInt("mood", 3),
                stressLevel = obj.optInt("stressLevel", 3),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            ))
        }
    }

    private suspend fun restoreJournalEntries(array: JSONArray) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            journalDao.insertEntry(JournalEntryEntity(
                id = obj.getString("id"),
                date = obj.getLong("date"),
                mood = obj.optInt("mood", 3),
                title = obj.optString("title"),
                content = obj.getString("content"),
                tags = obj.optString("tags", ""),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            ))
        }
    }

    private suspend fun restoreHabits(array: JSONArray) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            habitDao.insertHabit(HabitEntity(
                id = obj.getString("id"),
                name = obj.getString("name"),
                type = obj.getString("type"),
                target = obj.optInt("target", 1),
                currentStreak = obj.optInt("currentStreak", 0),
                longestStreak = obj.optInt("longestStreak", 0),
                icon = obj.optString("icon", "✓"),
                color = obj.optLong("color", 0xFF4CAF50),
                isActive = obj.optBoolean("isActive", true),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            ))
        }
    }

    private suspend fun restoreAchievements(array: JSONArray) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            achievementDao.insertAchievement(AchievementEntity(
                id = obj.getString("id"),
                title = obj.getString("title"),
                description = obj.getString("description"),
                icon = obj.optString("icon", "🏆"),
                requirement = obj.optInt("requirement", 1),
                type = obj.getString("type"),
                unlocked = obj.optBoolean("unlocked", false),
                unlockedAt = obj.optLong("unlockedAt", 0).takeIf { it > 0 }
            ))
        }
    }

    private suspend fun restoreFocusSessions(array: JSONArray) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            focusSessionDao.insertSession(FocusSessionEntity(
                id = obj.getString("id"),
                duration = obj.getInt("duration"),
                completed = obj.optBoolean("completed", false),
                date = obj.optLong("date", System.currentTimeMillis()),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            ))
        }
    }

    private suspend fun restoreTasks(array: JSONArray) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            taskDao.insertTask(TaskEntity(
                id = obj.getString("id"),
                title = obj.getString("title"),
                date = obj.getLong("date"),
                priority = obj.optString("priority", "MEDIUM"),
                completed = obj.optBoolean("completed", false),
                category = obj.optString("category"),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            ))
        }
    }

    private suspend fun restoreMindfulSessions(array: JSONArray) {
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            mindfulSessionDao.insertSession(MindfulSessionEntity(
                id = obj.getString("id"),
                type = obj.getString("type"),
                durationSeconds = obj.getInt("durationSeconds"),
                completed = obj.optBoolean("completed", false),
                date = obj.optLong("date", System.currentTimeMillis()),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            ))
        }
    }

    private suspend fun restoreWorkLogs(array: JSONArray?) {
        array ?: return
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            workLogDao.insertWorkLog(WorkLogEntity(
                id = obj.getString("id"),
                date = obj.getLong("date"),
                dayType = obj.getString("dayType"),
                workHours = obj.optDouble("workHours", 8.0).toFloat(),
                overtimeHours = obj.optDouble("overtimeHours", 0.0).toFloat(),
                note = obj.optString("note"),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
            ))
        }
    }

    private suspend fun restoreUserPreferences(obj: JSONObject) {
        userPreferencesDao.insertPreferences(UserPreferencesEntity(
            userName = obj.optString("userName", "User"),
            monthlyIncome = obj.optDouble("monthlyIncome", 0.0),
            currency = obj.optString("currency", "BDT"),
            currencySymbol = obj.optString("currencySymbol", "৳"),
            darkModeEnabled = obj.optBoolean("darkModeEnabled", false),
            notificationsEnabled = obj.optBoolean("notificationsEnabled", true)
        ))
    }

    @JvmName("accountListToJsonArray")
    private fun List<AccountEntity>.toJsonArray(): JSONArray {
        return JSONArray().apply {
            forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("balance", item.balance)
                    put("icon", item.icon)
                    put("color", item.color)
                    put("createdAt", item.createdAt)
                    put("updatedAt", item.updatedAt)
                    put("isSynced", item.isSynced)
                })
            }
        }
    }

    @JvmName("transactionListToJsonArray")
    private fun List<TransactionEntity>.toJsonArray(): JSONArray {
        return JSONArray().apply {
            forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("amount", item.amount)
                    put("type", item.type)
                    put("categoryId", item.categoryId)
                    put("accountId", item.accountId)
                    put("note", item.note)
                    put("timestamp", item.timestamp)
                    put("createdAt", item.createdAt)
                    put("updatedAt", item.updatedAt)
                    put("isSynced", item.isSynced)
                })
            }
        }
    }

    @JvmName("categoryListToJsonArray")
    private fun List<CategoryEntity>.toJsonArray(): JSONArray {
        return JSONArray().apply {
            forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("icon", item.icon)
                    put("color", item.color)
                    put("type", item.type)
                    put("isDefault", item.isDefault)
                })
            }
        }
    }

    @JvmName("savingsGoalListToJsonArray")
    private fun List<SavingsGoalEntity>.toJsonArray(): JSONArray {
        return JSONArray().apply {
            forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("targetAmount", item.targetAmount)
                    put("currentAmount", item.currentAmount)
                    put("icon", item.icon)
                    put("color", item.color)
                    put("deadline", item.deadline)
                    put("createdAt", item.createdAt)
                    put("updatedAt", item.updatedAt)
                })
            }
        }
    }

    @JvmName("budgetListToJsonArray")
    private fun List<BudgetEntity>.toJsonArray(): JSONArray {
        return JSONArray().apply {
            forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("categoryId", item.categoryId)
                    put("monthlyLimit", item.monthlyLimit)
                    put("month", item.month)
                    put("year", item.year)
                    put("alertThreshold", item.alertThreshold)
                    put("createdAt", item.createdAt)
                    put("updatedAt", item.updatedAt)
                })
            }
        }
    }

    @JvmName("loanListToJsonArray")
    private fun List<LoanEntity>.toJsonArray(): JSONArray {
        return JSONArray().apply {
            forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("lenderName", item.lenderName)
                    put("originalAmount", item.originalAmount)
                    put("remainingAmount", item.remainingAmount)
                    put("monthlyPayment", item.monthlyPayment)
                    put("interestRate", item.interestRate)
                    put("loanType", item.loanType)
                    put("startDate", item.startDate)
                    put("endDate", item.endDate)
                    put("isActive", item.isActive)
                    put("createdAt", item.createdAt)
                    put("updatedAt", item.updatedAt)
                })
            }
        }
    }

    @JvmName("creditCardListToJsonArray")
    private fun List<CreditCardEntity>.toJsonArray(): JSONArray {
        return JSONArray().apply {
            forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("cardType", item.cardType)
                    put("lastFourDigits", item.lastFourDigits)
                    put("creditLimit", item.creditLimit)
                    put("currentBalance", item.currentBalance)
                    put("availableCredit", item.availableCredit)
                    put("minimumPayment", item.minimumPayment)
                    put("dueDate", item.dueDate)
                    put("interestRate", item.interestRate)
                    put("isActive", item.isActive)
                    put("createdAt", item.createdAt)
                    put("updatedAt", item.updatedAt)
                })
            }
        }
    }

    @JvmName("healthEntryListToJsonArray")
    private fun List<HealthEntryEntity>.toJsonArray(): JSONArray {
        return JSONArray().apply {
            forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("date", item.date)
                    put("sleepHours", item.sleepHours)
                    put("sleepQuality", item.sleepQuality)
                    put("waterIntake", item.waterIntake)
                    put("steps", item.steps)
                    put("workHours", item.workHours)
                    put("breakTime", item.breakTime)
                    put("mood", item.mood)
                    put("stressLevel", item.stressLevel)
                    put("createdAt", item.createdAt)
                    put("updatedAt", item.updatedAt)
                })
            }
        }
    }

    @JvmName("journalEntryListToJsonArray")
    private fun List<JournalEntryEntity>.toJsonArray(): JSONArray {
        return JSONArray().apply {
            forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("date", item.date)
                    put("mood", item.mood)
                    put("title", item.title)
                    put("content", item.content)
                    put("tags", item.tags)
                    put("createdAt", item.createdAt)
                    put("updatedAt", item.updatedAt)
                })
            }
        }
    }

    @JvmName("habitListToJsonArray")
    private fun List<HabitEntity>.toJsonArray(): JSONArray {
        return JSONArray().apply {
            forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("name", item.name)
                    put("type", item.type)
                    put("target", item.target)
                    put("currentStreak", item.currentStreak)
                    put("longestStreak", item.longestStreak)
                    put("icon", item.icon)
                    put("color", item.color)
                    put("isActive", item.isActive)
                    put("createdAt", item.createdAt)
                    put("updatedAt", item.updatedAt)
                })
            }
        }
    }

    @JvmName("achievementListToJsonArray")
    private fun List<AchievementEntity>.toJsonArray(): JSONArray {
        return JSONArray().apply {
            forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("description", item.description)
                    put("icon", item.icon)
                    put("requirement", item.requirement)
                    put("type", item.type)
                    put("unlocked", item.unlocked)
                    put("unlockedAt", item.unlockedAt)
                })
            }
        }
    }

    @JvmName("focusSessionListToJsonArray")
    private fun List<FocusSessionEntity>.toJsonArray(): JSONArray {
        return JSONArray().apply {
            forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("duration", item.duration)
                    put("completed", item.completed)
                    put("date", item.date)
                    put("createdAt", item.createdAt)
                })
            }
        }
    }

    @JvmName("taskListToJsonArray")
    private fun List<TaskEntity>.toJsonArray(): JSONArray {
        return JSONArray().apply {
            forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("title", item.title)
                    put("date", item.date)
                    put("priority", item.priority)
                    put("completed", item.completed)
                    put("category", item.category)
                    put("createdAt", item.createdAt)
                    put("updatedAt", item.updatedAt)
                })
            }
        }
    }

    @JvmName("mindfulSessionListToJsonArray")
    private fun List<MindfulSessionEntity>.toJsonArray(): JSONArray {
        return JSONArray().apply {
            forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("type", item.type)
                    put("durationSeconds", item.durationSeconds)
                    put("completed", item.completed)
                    put("date", item.date)
                    put("createdAt", item.createdAt)
                })
            }
        }
    }

    @JvmName("workLogListToJsonArray")
    private fun List<WorkLogEntity>.toJsonArray(): JSONArray {
        return JSONArray().apply {
            forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("date", item.date)
                    put("dayType", item.dayType)
                    put("workHours", item.workHours)
                    put("overtimeHours", item.overtimeHours)
                    put("note", item.note)
                    put("createdAt", item.createdAt)
                    put("updatedAt", item.updatedAt)
                })
            }
        }
    }
}

data class RestoreResult(val success: Boolean, val message: String)
data class BackupResult(
    val success: Boolean,
    val filePath: String? = null,
    val fileName: String? = null,
    val sizeBytes: Long? = null,
    val isCompressed: Boolean = false,
    val isEncrypted: Boolean = false,
    val errorMessage: String? = null
)
data class BackupFile(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val createdAt: Long
)
