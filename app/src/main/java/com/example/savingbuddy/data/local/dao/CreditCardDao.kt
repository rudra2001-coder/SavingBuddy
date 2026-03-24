package com.example.savingbuddy.data.local.dao

import androidx.room.*
import com.example.savingbuddy.data.local.entity.CreditCardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CreditCardDao {
    @Query("SELECT * FROM credit_cards ORDER BY createdAt DESC")
    fun getAllCards(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards WHERE isActive = 1 ORDER BY createdAt DESC")
    fun getActiveCards(): Flow<List<CreditCardEntity>>

    @Query("SELECT * FROM credit_cards WHERE id = :id")
    suspend fun getCardById(id: String): CreditCardEntity?

    @Query("SELECT SUM(currentBalance) FROM credit_cards WHERE isActive = 1")
    fun getTotalBalance(): Flow<Double?>

    @Query("SELECT SUM(availableCredit) FROM credit_cards WHERE isActive = 1")
    fun getTotalAvailableCredit(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CreditCardEntity)

    @Update
    suspend fun updateCard(card: CreditCardEntity)

    @Delete
    suspend fun deleteCard(card: CreditCardEntity)

    @Query("DELETE FROM credit_cards")
    suspend fun deleteAllCards()
}
