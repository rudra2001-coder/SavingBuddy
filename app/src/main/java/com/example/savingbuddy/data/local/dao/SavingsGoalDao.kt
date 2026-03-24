package com.example.savingbuddy.data.local.dao

import androidx.room.*
import com.example.savingbuddy.data.local.entity.SavingsGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SavingsGoalDao {
    @Query("SELECT * FROM savings_goals ORDER BY createdAt DESC")
    fun getAllSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals WHERE id = :id")
    suspend fun getSavingsGoalById(id: String): SavingsGoalEntity?

    @Query("SELECT SUM(currentAmount) FROM savings_goals")
    fun getTotalSavings(): Flow<Double?>

    @Query("SELECT SUM(targetAmount) FROM savings_goals")
    fun getTotalTargetAmount(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoalEntity)

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity)

    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity)

    @Query("UPDATE savings_goals SET currentAmount = currentAmount + :amount WHERE id = :goalId")
    suspend fun addToGoal(goalId: String, amount: Double)
}