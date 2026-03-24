package com.example.savingbuddy.data.repository

import com.example.savingbuddy.data.local.dao.BudgetDao
import com.example.savingbuddy.data.local.entity.BudgetEntity
import com.example.savingbuddy.domain.model.Budget
import com.example.savingbuddy.domain.repository.BudgetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class BudgetRepositoryImpl @Inject constructor(
    private val budgetDao: BudgetDao
) : BudgetRepository {

    override fun getBudgetsForMonth(month: Int, year: Int): Flow<List<Budget>> =
        budgetDao.getBudgetsForMonth(month, year).map { list -> list.mapNotNull { entityToBudget(it) } }

    override suspend fun getBudgetForCategory(categoryId: String, month: Int, year: Int): Budget? =
        entityToBudget(budgetDao.getBudgetForCategory(categoryId, month, year))

    override suspend fun getBudgetById(id: String): Budget? =
        entityToBudget(budgetDao.getBudgetById(id))

    override suspend fun addBudget(budget: Budget) {
        budgetDao.insertBudget(budgetToEntity(budget))
    }

    override suspend fun updateBudget(budget: Budget) {
        budgetDao.updateBudget(budgetToEntity(budget))
    }

    override suspend fun deleteBudget(budget: Budget) {
        budgetDao.deleteBudget(budgetToEntity(budget))
    }

    private fun entityToBudget(entity: BudgetEntity?): Budget? {
        return entity?.let {
            Budget(
                id = it.id,
                categoryId = it.categoryId,
                monthlyLimit = it.monthlyLimit,
                month = it.month,
                year = it.year,
                alertThreshold = it.alertThreshold,
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
            )
        }
    }

    private fun budgetToEntity(budget: Budget): BudgetEntity {
        return BudgetEntity(
            id = budget.id,
            categoryId = budget.categoryId,
            monthlyLimit = budget.monthlyLimit,
            month = budget.month,
            year = budget.year,
            alertThreshold = budget.alertThreshold,
            createdAt = budget.createdAt,
            updatedAt = budget.updatedAt
        )
    }
}
