package com.example.savingbuddy.domain.repository

import com.example.savingbuddy.domain.model.Category
import com.example.savingbuddy.domain.model.TransactionType
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    fun getCategoriesByType(type: TransactionType): Flow<List<Category>>
    suspend fun getCategoryById(id: String): Category?
    suspend fun addCategory(category: Category)
    suspend fun addDefaultCategories()
    suspend fun addPresetExpenseCategories() // New method for preset categories
    suspend fun updateCategory(category: Category)
    suspend fun deleteCategory(category: Category)
    suspend fun deleteAllCategories()
}