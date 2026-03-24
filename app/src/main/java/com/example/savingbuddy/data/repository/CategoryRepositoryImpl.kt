package com.example.savingbuddy.data.repository

import com.example.savingbuddy.data.local.dao.CategoryDao
import com.example.savingbuddy.data.local.mapper.toDomain
import com.example.savingbuddy.data.local.mapper.toEntity
import com.example.savingbuddy.domain.model.Category
import com.example.savingbuddy.domain.model.TransactionType
import com.example.savingbuddy.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories().map { list -> list.map { it.toDomain() } }

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
        categoryDao.getCategoriesByType(type.name).map { list -> list.map { it.toDomain() } }

    override suspend fun getCategoryById(id: String): Category? =
        categoryDao.getCategoryById(id)?.toDomain()

    override suspend fun addCategory(category: Category) {
        categoryDao.insertCategory(category.toEntity())
    }

    override suspend fun addDefaultCategories() {
        val defaultCategories = listOf(
            Category("", "Food", "restaurant", 0xFFFF5722, TransactionType.EXPENSE, true),
            Category("", "Transport", "directions_car", 0xFF2196F3, TransactionType.EXPENSE, true),
            Category("", "Shopping", "shopping_bag", 0xFFE91E63, TransactionType.EXPENSE, true),
            Category("", "Entertainment", "movie", 0xFF9C27B0, TransactionType.EXPENSE, true),
            Category("", "Bills", "receipt", 0xFF607D8B, TransactionType.EXPENSE, true),
            Category("", "Health", "local_hospital", 0xFFF44336, TransactionType.EXPENSE, true),
            Category("", "Salary", "work", 0xFF4CAF50, TransactionType.INCOME, true),
            Category("", "Freelance", "laptop", 0xFF00BCD4, TransactionType.INCOME, true),
            Category("", "Investment", "trending_up", 0xFF8BC34A, TransactionType.INCOME, true),
            Category("", "Gift", "card_giftcard", 0xFFFF9800, TransactionType.INCOME, true)
        ).map { it.toEntity() }
        categoryDao.insertCategories(defaultCategories)
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(category.toEntity())
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category.toEntity())
    }
}