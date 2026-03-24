package com.example.savingbuddy.data.repository

import com.example.savingbuddy.data.local.dao.CategoryDao
import com.example.savingbuddy.data.local.entity.CategoryEntity
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
        categoryDao.getAllCategories().map { list -> list.mapNotNull { entityToCategory(it) } }

    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
        categoryDao.getCategoriesByType(type.name).map { list -> list.mapNotNull { entityToCategory(it) } }

    override suspend fun getCategoryById(id: String): Category? =
        entityToCategory(categoryDao.getCategoryById(id))

    override suspend fun addCategory(category: Category) {
        categoryDao.insertCategory(categoryToEntity(category))
    }

    override suspend fun addDefaultCategories() {
        // Default categories are added in database initialization
    }

    override suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(categoryToEntity(category))
    }

    override suspend fun updateCategory(category: Category) {
        categoryDao.updateCategory(categoryToEntity(category))
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
