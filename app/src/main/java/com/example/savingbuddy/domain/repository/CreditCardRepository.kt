package com.example.savingbuddy.domain.repository

import com.example.savingbuddy.domain.model.CreditCard
import kotlinx.coroutines.flow.Flow

interface CreditCardRepository {
    fun getAllCards(): Flow<List<CreditCard>>
    fun getActiveCards(): Flow<List<CreditCard>>
    suspend fun getCardById(id: String): CreditCard?
    fun getTotalBalance(): Flow<Double>
    fun getTotalAvailableCredit(): Flow<Double>
    suspend fun addCard(card: CreditCard)
    suspend fun updateCard(card: CreditCard)
    suspend fun deleteCard(card: CreditCard)
}
