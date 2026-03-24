package com.example.savingbuddy.data.repository

import com.example.savingbuddy.data.local.dao.CategoryDao
import com.example.savingbuddy.data.local.entity.CategoryEntity
import com.example.savingbuddy.domain.model.Category
import com.example.savingbuddy.domain.model.TransactionType
import com.example.savingbuddy.domain.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CategoryRepositoryImpl @Inject constructor(
    private val categoryDao: CategoryDao
) : CategoryRepository {

    override fun getAllCategories(): Flow<List<Category>> =
        categoryDao.getAllCategories().map { list -> list.mapNotNull { entityToCategory(it) } }

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
        categoryDao.getCategoriesByType(type.name).map { list -> list.mapNotNull { entityToCategory(it) } }

    override suspend fun getCategoryById(id: String): Category? =
        entityToCategory(categoryDao.getCategoryById(id))

    override suspend fun addCategory(category: Category) {
        categoryDao.insertCategory(categoryToEntity(category))
    }

    override suspend fun addDefaultCategories() {
        // Check if categories already exist
        val existingCategories = categoryDao.getAllCategories().first()
        if (existingCategories.isNotEmpty()) {
            return
        }
        
        // Add default expense categories
        addPresetExpenseCategories()
    }

    override suspend fun addPresetExpenseCategories() {
        val presetCategories = listOf(
            // Expense categories
            Category(
                id = "cat_food_dining",
                name = "Food & Dining",
                icon = "🍔",
                color = 0xFF4CAF50,
                type = TransactionType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = "cat_transportation",
                name = "Transportation",
                icon = "🚗",
                color = 0xFF2196F3,
                type = TransactionType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = "cat_shopping",
                name = "Shopping",
                icon = "🛍️",
                color = 0xFF9C27B0,
                type = TransactionType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = "cat_entertainment",
                name = "Entertainment",
                icon = "🎬",
                color = 0xFFFF9800,
                type = TransactionType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = "cat_bills",
                name = "Bills & Utilities",
                icon = "💡",
                color = 0xFF607D8B,
                type = TransactionType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = "cat_healthcare",
                name = "Healthcare",
                icon = "🏥",
                color = 0xFFE91E63,
                type = TransactionType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = "cat_education",
                name = "Education",
                icon = "📚",
                color = 0xFF3F51B5,
                type = TransactionType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = "cat_rent",
                name = "Rent & Mortgage",
                icon = "🏠",
                color = 0xFF795548,
                type = TransactionType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = "cat_personal_care",
                name = "Personal Care",
                icon = "💇",
                color = 0xFF00BCD4,
                type = TransactionType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = "cat_gifts",
                name = "Gifts & Donations",
                icon = "🎁",
                color = 0xFFFF5722,
                type = TransactionType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = "cat_travel",
                name = "Travel",
                icon = "✈️",
                color = 0xFF009688,
                type = TransactionType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = "cat_coffee",
                name = "Coffee & Snacks",
                icon = "☕",
                color = 0xFF8D6E63,
                type = TransactionType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = "cat_groceries",
                name = "Groceries",
                icon = "🛒",
                color = 0xFF4CAF50,
                type = TransactionType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = "cat_fitness",
                name = "Fitness",
                icon = "💪",
                color = 0xFFFF4081,
                type = TransactionType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = "cat_subscriptions",
                name = "Subscriptions",
                icon = "📱",
                color = 0xFF673AB7,
                type = TransactionType.EXPENSE,
                isDefault = true
            ),
            Category(
                id = "cat_other_expense",
                name = "Other",
                icon = "📌",
                color = 0xFF9E9E9E,
                type = TransactionType.EXPENSE,
                isDefault = true
            ),
            // Income categories
            Category(
                id = "cat_salary",
                name = "Salary",
                icon = "💰",
                color = 0xFF4CAF50,
                type = TransactionType.INCOME,
                isDefault = true
            ),
            Category(
                id = "cat_business",
                name = "Business",
                icon = "💼",
                color = 0xFF2196F3,
                type = TransactionType.INCOME,
                isDefault = true
            ),
            Category(
                id = "cat_investment",
                name = "Investment",
                icon = "📈",
                color = 0xFFFF9800,
                type = TransactionType.INCOME,
                isDefault = true
            ),
            Category(
                id = "cat_gift_income",
                name = "Gift",
                icon = "🎁",
                color = 0xFFE91E63,
                type = TransactionType.INCOME,
                isDefault = true
            ),
            Category(
                id = "cat_other_income",
                name = "Other",
                icon = "📌",
                color = 0xFF9E9E9E,
                type = TransactionType.INCOME,
                isDefault = true
            )
        )

        for (category in presetCategories) {
            // Check if category already exists
            val existing = categoryDao.getCategoryById(category.id)
            if (existing == null) {
                categoryDao.insertCategory(categoryToEntity(category))
            }
        }
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(categoryToEntity(category))
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(categoryToEntity(category))
    }

    override suspend fun deleteAllCategories() {
        categoryDao.deleteAllCategories()
    }

    private fun entityToCategory(entity: CategoryEntity?): Category? {
        return entity?.let {
            Category(
                id = it.id,
                name = it.name,
                icon = it.icon,
                color = it.color,
                type = TransactionType.valueOf(it.type),
                isDefault = it.isDefault
            )
        }
    }

    private fun categoryToEntity(category: Category): CategoryEntity {
        return CategoryEntity(
            id = category.id,
            name = category.name,
            icon = category.icon,
            color = category.color,
            type = category.type.name,
            isDefault = category.isDefault
        )
    }
}
