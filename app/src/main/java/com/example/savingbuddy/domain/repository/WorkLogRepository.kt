package com.example.savingbuddy.domain.repository

import com.example.savingbuddy.domain.model.WorkLog
import com.example.savingbuddy.domain.model.WorkLogSummary
import kotlinx.coroutines.flow.Flow

interface WorkLogRepository {
    fun getWorkLogsForDateRange(startDate: Long, endDate: Long): Flow<List<WorkLog>>
    fun getAllWorkLogs(): Flow<List<WorkLog>>
    fun getWorkLogsByType(dayType: String): Flow<List<WorkLog>>
    fun getCountByType(dayType: String): Flow<Int>
    fun getTotalWorkHours(startDate: Long, endDate: Long): Flow<Float>
    fun getTotalOvertimeHours(startDate: Long, endDate: Long): Flow<Float>
    suspend fun getWorkLogByDate(date: Long): WorkLog?
    suspend fun addWorkLog(workLog: WorkLog)
    suspend fun addWorkLogs(workLogs: List<WorkLog>)
    suspend fun updateWorkLog(workLog: WorkLog)
    suspend fun deleteWorkLog(workLog: WorkLog)
    suspend fun deleteWorkLogsForRange(startDate: Long, endDate: Long)
    suspend fun deleteWorkLogByDate(date: Long)
    suspend fun getWorkLogSummary(startDate: Long, endDate: Long): WorkLogSummary
}