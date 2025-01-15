package com.dsk.myexpense.expense_module.data.source.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dsk.myexpense.expense_module.data.model.Category

@Dao
interface CategoryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<Category>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: Category): Long

    @Delete
    suspend fun deleteCategory(category: Category)  // Add this method to delete a category

    @Query("SELECT * FROM category_table")
    fun getAllCategories(): List<Category>

    @Query("SELECT * FROM category_table")
    fun getAllCategoriesLiveData(): LiveData<List<Category>>

    @Query("SELECT * FROM category_table")
    suspend fun getAllCategoriesValue(): List<Category>

    @Query("SELECT * FROM category_table WHERE type = :type")
    suspend fun getCategoriesByType(type: String): List<Category>

    @Query("SELECT * FROM category_table WHERE name = :name AND type = :type LIMIT 1")
    suspend fun getCategoryByNameAndType(name: String, type: String): Category?

    @Query("SELECT * FROM category_table WHERE id = :categoryId LIMIT 1")
    suspend fun getCategoryNameByID(categoryId: Int): Category?
}
