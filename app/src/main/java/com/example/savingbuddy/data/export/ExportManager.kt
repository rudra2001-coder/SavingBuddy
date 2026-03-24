package com.example.savingbuddy.data.export

import android.content.Context
import android.os.Environment
import com.example.savingbuddy.data.local.dao.BudgetDao
import com.example.savingbuddy.data.local.dao.CategoryDao
import com.example.savingbuddy.data.local.dao.CreditCardDao
import com.example.savingbuddy.data.local.dao.LoanDao
import com.example.savingbuddy.data.local.dao.RecurringTransactionDao
import com.example.savingbuddy.data.local.dao.SavingsGoalDao
import com.example.savingbuddy.data.local.dao.TransactionDao
import com.example.savingbuddy.data.local.dao.AccountDao
import com.example.savingbuddy.data.local.entity.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExportManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionDao: TransactionDao,
    private val accountDao: AccountDao,
    private val categoryDao: CategoryDao,
    private val savingsGoalDao: SavingsGoalDao,
    private val budgetDao: BudgetDao,
    private val creditCardDao: CreditCardDao,
    private val loanDao: LoanDao,
    private val recurringTransactionDao: RecurringTransactionDao
) {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    suspend fun exportTransactionsCsv(startDate: Long? = null, endDate: Long? = null): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val transactions = if (startDate != null && endDate != null) {
                    transactionDao.getTransactionsByDateRange(startDate, endDate).first()
                } else {
                    transactionDao.getAllTransactions().first()
                }

                val categories = categoryDao.getAllCategories().first().associateBy { it.id }
                val accounts = accountDao.getAllAccounts().first().associateBy { it.id }

                val csvContent = buildString {
                    appendLine("Date,Type,Amount,Category,Account,Note")
                    transactions.forEach { tx ->
                        val categoryName = categories[tx.categoryId]?.name ?: "Unknown"
                        val accountName = accounts[tx.accountId]?.name ?: "Unknown"
                        val date = dateFormat.format(Date(tx.timestamp))
                        val note = tx.note?.replace(",", ";")?.replace("\n", " ") ?: ""
                        appendLine("$date,${tx.type},${tx.amount},$categoryName,$accountName,$note")
                    }
                }

                val fileName = "transactions_${System.currentTimeMillis()}.csv"
                val file = saveToDownloads(fileName, csvContent)

                ExportResult(
                    success = true,
                    filePath = file.absolutePath,
                    fileName = fileName,
                    recordCount = transactions.size
                )
            } catch (e: Exception) {
                ExportResult(success = false, errorMessage = e.message)
            }
        }
    }

    suspend fun exportAccountsCsv(): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val accounts = accountDao.getAllAccounts().first()

                val csvContent = buildString {
                    appendLine("Name,Balance,Icon,Created")
                    accounts.forEach { acc ->
                        val date = dateFormat.format(Date(acc.createdAt))
                        appendLine("${acc.name.replace(",", ";")},${acc.balance},${acc.icon},$date")
                    }
                }

                val fileName = "accounts_${System.currentTimeMillis()}.csv"
                val file = saveToDownloads(fileName, csvContent)

                ExportResult(
                    success = true,
                    filePath = file.absolutePath,
                    fileName = fileName,
                    recordCount = accounts.size
                )
            } catch (e: Exception) {
                ExportResult(success = false, errorMessage = e.message)
            }
        }
    }

    suspend fun exportSavingsGoalsCsv(): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val goals = savingsGoalDao.getAllSavingsGoals().first()

                val csvContent = buildString {
                    appendLine("Name,Target Amount,Current Amount,Icon,Deadline")
                    goals.forEach { goal ->
                        val deadline = if (goal.deadline != null && goal.deadline > 0) dateFormat.format(Date(goal.deadline)) else "N/A"
                        appendLine("${goal.name.replace(",", ";")},${goal.targetAmount},${goal.currentAmount},${goal.icon},$deadline")
                    }
                }

                val fileName = "savings_goals_${System.currentTimeMillis()}.csv"
                val file = saveToDownloads(fileName, csvContent)

                ExportResult(
                    success = true,
                    filePath = file.absolutePath,
                    fileName = fileName,
                    recordCount = goals.size
                )
            } catch (e: Exception) {
                ExportResult(success = false, errorMessage = e.message)
            }
        }
    }

    suspend fun exportBudgetsCsv(month: Int, year: Int): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val budgets = budgetDao.getBudgetsForMonth(month, year).first()
                val categories = categoryDao.getAllCategories().first().associateBy { it.id }

                val csvContent = buildString {
                    appendLine("Category,Monthly Limit,Month,Year,Alert Threshold")
                    budgets.forEach { budget ->
                        val categoryName = categories[budget.categoryId]?.name ?: "Unknown"
                        appendLine("$categoryName,${budget.monthlyLimit},${budget.month},${budget.year},${budget.alertThreshold}")
                    }
                }

                val fileName = "budgets_${System.currentTimeMillis()}.csv"
                val file = saveToDownloads(fileName, csvContent)

                ExportResult(
                    success = true,
                    filePath = file.absolutePath,
                    fileName = fileName,
                    recordCount = budgets.size
                )
            } catch (e: Exception) {
                ExportResult(success = false, errorMessage = e.message)
            }
        }
    }

    suspend fun exportFullBackupCsv(): ExportResult {
        return withContext(Dispatchers.IO) {
            try {
                val transactions = transactionDao.getAllTransactions().first()
                val accounts = accountDao.getAllAccounts().first()
                val categories = categoryDao.getAllCategories().first()
                val savingsGoals = savingsGoalDao.getAllSavingsGoals().first()
                val budgets = budgetDao.getBudgetsForMonth(Calendar.getInstance().get(Calendar.MONTH), Calendar.getInstance().get(Calendar.YEAR)).first()
                val creditCards = creditCardDao.getAllCards().first()
                val loans = loanDao.getAllLoans().first()
                val recurringTransactions = recurringTransactionDao.getAllRecurringTransactions().first()

                val csvContent = buildString {
                    appendLine("# SavingBuddy Full Backup")
                    appendLine("# Exported: ${dateTimeFormat.format(Date())}")
                    appendLine()
                    
                    appendLine("## Accounts")
                    appendLine("Name,Balance,Icon,Created")
                    accounts.forEach { acc ->
                        appendLine("${acc.name.replace(",", ";")},${acc.balance},${acc.icon},${dateFormat.format(Date(acc.createdAt))}")
                    }
                    appendLine()
                    
                    appendLine("## Categories")
                    appendLine("Name,Icon,Color,Type,IsDefault")
                    categories.forEach { cat ->
                        appendLine("${cat.name.replace(",", ";")},${cat.icon},${cat.color},${cat.type},${cat.isDefault}")
                    }
                    appendLine()
                    
                    appendLine("## Transactions")
                    appendLine("Date,Type,Amount,Category ID,Account ID,Note")
                    transactions.forEach { tx ->
                        appendLine("${dateFormat.format(Date(tx.timestamp))},${tx.type},${tx.amount},${tx.categoryId},${tx.accountId},${tx.note?.replace(",", ";") ?: ""}")
                    }
                    appendLine()
                    
                    appendLine("## Savings Goals")
                    appendLine("Name,Target Amount,Current Amount,Icon,Deadline")
                    savingsGoals.forEach { goal ->
                        appendLine("${goal.name.replace(",", ";")},${goal.targetAmount},${goal.currentAmount},${goal.icon},${goal.deadline}")
                    }
                    appendLine()
                    
                    appendLine("## Budgets")
                    appendLine("Category ID,Monthly Limit,Month,Year,Alert Threshold")
                    budgets.forEach { budget ->
                        appendLine("${budget.categoryId},${budget.monthlyLimit},${budget.month},${budget.year},${budget.alertThreshold}")
                    }
                    appendLine()
                    
                    appendLine("## Credit Cards")
                    appendLine("Name,Card Type,Last Four,Credit Limit,Current Balance,Available Credit")
                    creditCards.forEach { card ->
                        appendLine("${card.name.replace(",", ";")},${card.cardType},${card.lastFourDigits},${card.creditLimit},${card.currentBalance},${card.availableCredit}")
                    }
                    appendLine()
                    
                    appendLine("## Loans")
                    appendLine("Name,Lender,Original Amount,Remaining Amount,Monthly Payment,Interest Rate")
                    loans.forEach { loan ->
                        appendLine("${loan.name.replace(",", ";")},${loan.lenderName},${loan.originalAmount},${loan.remainingAmount},${loan.monthlyPayment},${loan.interestRate}")
                    }
                    appendLine()
                    
                    appendLine("## Recurring Transactions")
                    appendLine("Title,Amount,Type,Category ID,Account ID,Recurring Type,Is Active")
                    recurringTransactions.forEach { rt ->
                        appendLine("${rt.title.replace(",", ";")},${rt.amount},${rt.type},${rt.categoryId},${rt.accountId},${rt.recurringType},${rt.isActive}")
                    }
                }

                val fileName = "savingbuddy_backup_${System.currentTimeMillis()}.csv"
                val file = saveToDownloads(fileName, csvContent)

                ExportResult(
                    success = true,
                    filePath = file.absolutePath,
                    fileName = fileName,
                    recordCount = transactions.size + accounts.size + categories.size + savingsGoals.size + budgets.size + creditCards.size + loans.size + recurringTransactions.size
                )
            } catch (e: Exception) {
                ExportResult(success = false, errorMessage = e.message)
            }
        }
    }

    private fun saveToDownloads(fileName: String, content: String): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, fileName)
        FileWriter(file).use { it.write(content) }
        return file
    }

    fun getExportHistory(): List<ExportRecord> {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        return downloadsDir.listFiles { _, name ->
            name.startsWith("savingbuddy") || name.startsWith("transactions") || 
            name.startsWith("accounts") || name.startsWith("savings") || name.startsWith("budgets")
        }?.map { file ->
            ExportRecord(
                name = file.name,
                path = file.absolutePath,
                sizeBytes = file.length(),
                createdAt = file.lastModified()
            )
        }?.sortedByDescending { it.createdAt } ?: emptyList()
    }
}

data class ExportResult(
    val success: Boolean,
    val filePath: String? = null,
    val fileName: String? = null,
    val recordCount: Int = 0,
    val errorMessage: String? = null
)

data class ExportRecord(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val createdAt: Long
)