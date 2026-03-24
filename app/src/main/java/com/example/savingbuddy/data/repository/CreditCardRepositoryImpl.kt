package com.example.savingbuddy.data.repository

import com.example.savingbuddy.data.local.dao.CreditCardDao
import com.example.savingbuddy.data.local.entity.CreditCardEntity
import com.example.savingbuddy.domain.model.CardType
import com.example.savingbuddy.domain.model.CreditCard
import com.example.savingbuddy.domain.repository.CreditCardRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CreditCardRepositoryImpl @Inject constructor(
    private val creditCardDao: CreditCardDao
) : CreditCardRepository {

    override fun getAllCards(): Flow<List<CreditCard>> =
        creditCardDao.getAllCards().map { list -> list.mapNotNull { entityToCard(it) } }

    override fun getActiveCards(): Flow<List<CreditCard>> =
        creditCardDao.getActiveCards().map { list -> list.mapNotNull { entityToCard(it) } }

    override suspend fun getCardById(id: String): CreditCard? =
        entityToCard(creditCardDao.getCardById(id))

    override fun getTotalBalance(): Flow<Double> =
        creditCardDao.getTotalBalance().map { it ?: 0.0 }

    override fun getTotalAvailableCredit(): Flow<Double> =
        creditCardDao.getTotalAvailableCredit().map { it ?: 0.0 }

    override suspend fun addCard(card: CreditCard) {
        creditCardDao.insertCard(cardToEntity(card))
    }

    override suspend fun updateCard(card: CreditCard) {
        creditCardDao.updateCard(cardToEntity(card))
    }

    override suspend fun deleteCard(card: CreditCard) {
        creditCardDao.deleteCard(cardToEntity(card))
    }

    private fun entityToCard(entity: CreditCardEntity?): CreditCard? {
        return entity?.let {
            CreditCard(
                id = it.id,
                name = it.name,
                cardType = CardType.valueOf(it.cardType),
                lastFourDigits = it.lastFourDigits,
                creditLimit = it.creditLimit,
                currentBalance = it.currentBalance,
                availableCredit = it.availableCredit,
                minimumPayment = it.minimumPayment,
                dueDate = it.dueDate,
                interestRate = it.interestRate,
                isActive = it.isActive,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
    }

    private fun cardToEntity(card: CreditCard): CreditCardEntity {
        return CreditCardEntity(
            id = card.id,
            name = card.name,
            cardType = card.cardType.name,
            lastFourDigits = card.lastFourDigits,
            creditLimit = card.creditLimit,
            currentBalance = card.currentBalance,
            availableCredit = card.availableCredit,
            minimumPayment = card.minimumPayment,
            dueDate = card.dueDate,
            interestRate = card.interestRate,
            isActive = card.isActive,
            createdAt = card.createdAt,
            updatedAt = card.updatedAt
        )
    }
}
