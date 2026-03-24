package com.example.savingbuddy.data.backup

import android.content.Context
import androidx.work.*
import com.example.savingbuddy.data.local.dao.UserPreferencesDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

class AutoBackupWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // This worker should be triggered by the app when auto-backup is enabled
            // The actual backup logic is handled by BackupManager
            // For now, just succeed - the app triggers backups manually
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "auto_backup_work"

        fun schedule(context: Context, frequency: String) {
            val workManager = WorkManager.getInstance(context)
            
            workManager.cancelUniqueWork(WORK_NAME)
            
            val repeatInterval = when (frequency) {
                "DAILY" -> 1L to TimeUnit.DAYS
                "WEEKLY" -> 7L to TimeUnit.DAYS
                "MONTHLY" -> 30L to TimeUnit.DAYS
                else -> 1L to TimeUnit.DAYS
            }

            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val backupWork = PeriodicWorkRequestBuilder<AutoBackupWorker>(repeatInterval.first, repeatInterval.second)
                .setConstraints(constraints)
                .build()

            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE,
                backupWork
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}

@Singleton
class BackupScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val userPreferencesDao: UserPreferencesDao
) {
    suspend fun updateSchedule() {
        val prefs = userPreferencesDao.getPreferencesSync()
        if (prefs?.autoBackupEnabled == true) {
            AutoBackupWorker.schedule(context, prefs.backupFrequency)
        } else {
            AutoBackupWorker.cancel(context)
        }
    }
}