package com.dsk.myexpense.expense_module.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.data.repository.CategoryRepository
import kotlinx.coroutines.launch

class CategoryViewModel(private val categoryRepository: CategoryRepository) : ViewModel() {

    // LiveData holding the list of categories
    var categories: LiveData<List<Category>> = categoryRepository.getAllCategoriesLiveData()

    private val _newCategory = MutableLiveData<Category?>()
    val newCategory: LiveData<Category?> get() = _newCategory

    // Add a new category and return the result via LiveData
    fun addCategory(category: Category) {
        viewModelScope.launch {
            val insertedCategoryId = categoryRepository.addCategory(category)
            if (insertedCategoryId > 0) {
                category.id = insertedCategoryId.toInt() // Update the ID of the category
                _newCategory.postValue(category) // Set the newly added category
            } else {
                _newCategory.postValue(null) // In case of failure, post null
            }
        }
    }

    fun deleteCategory(category: Category) {
        viewModelScope.launch {
            categoryRepository.deleteCategory(category)  // Use your repository to delete the category from the DB
        }
    }
}
