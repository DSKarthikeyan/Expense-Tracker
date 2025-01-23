package com.dsk.myexpense.expense_module.data.repository

import androidx.lifecycle.LiveData
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.data.source.local.db.CategoryDao

class CategoryRepository(private var categoryDao: CategoryDao) {

    suspend fun getCategoryOrInsert(categoryName: String, type: String): Category {
        return categoryDao.getCategoryByNameAndType(categoryName, type) ?: run {
            val existingCategory = categoryDao.getCategoriesByType(type).firstOrNull()
            if (existingCategory == null) {
                val defaultCategory = Category(
                    name = "Default $type", type = type, iconResId = R.drawable.ic_other_expenses
                )
                val newCategoryId = categoryDao.insertCategory(defaultCategory)
                defaultCategory.copy(id = newCategoryId.toInt())
            } else {
                val newCategory = Category(
                    name = categoryName, type = type, iconResId = existingCategory.iconResId
                )
                val newCategoryId = categoryDao.insertCategory(newCategory)
                newCategory.copy(id = newCategoryId.toInt())
            }
        }
    }

    fun getCategoriesByType(type: String): List<Category> =
        categoryDao.getCategoriesByType(type)

    suspend fun getCategoriesByTypeName(type: String, name: String): Category? =
        categoryDao.getCategoryByNameAndType(type = type, name = name)

    suspend fun insertAllCategories(categories: List<Category>) = categoryDao.insertAll(categories)

    suspend fun insertCategory(categories: Category) = categoryDao.insertCategory(categories)

    suspend fun addCategory(category: Category): Long {
        return categoryDao.insertCategory(category)
    }

    suspend fun addCategories(categories: List<Category>) {
        categoryDao.insertAll(categories)
    }

    // Fetch all categories from the database
    fun getAllCategoriesLiveData(): LiveData<List<Category>> {
        return categoryDao.getAllCategoriesLiveData()
    }

    fun getAllCategories(): List<Category> {
        return categoryDao.getAllCategories()
    }

    // Method to delete a category from the repository
    suspend fun deleteCategory(category: Category) {
        categoryDao.deleteCategory(category)  // Delegate the delete operation to the DAO
    }

    suspend fun getCategoryNameByID(categoryID: Int): Category? {
        return categoryDao.getCategoryNameByID(categoryID)
    }

    suspend fun getCategoryByNameAndType(categoryName: String, type: String): Category{
       return categoryDao.getCategoryByNameAndType(categoryName, type) ?: run {
            val existingCategory = categoryDao.getCategoriesByType(type).firstOrNull()

            if (existingCategory == null) {
                val defaultCategory = Category(
                    name = "Default $type", type = type, iconResId = R.drawable.ic_other_expenses
                )
                val newCategoryId = categoryDao.insertCategory(defaultCategory)
                defaultCategory.copy(id = newCategoryId.toInt())
            } else {
                val newCategory = Category(
                    name = categoryName, type = type, iconResId = existingCategory.iconResId
                )
                val newCategoryId = categoryDao.insertCategory(newCategory)
                newCategory.copy(id = newCategoryId.toInt())
            }
        }
    }
}