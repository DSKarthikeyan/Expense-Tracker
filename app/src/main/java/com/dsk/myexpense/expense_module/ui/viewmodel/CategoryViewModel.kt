package com.dsk.myexpense.expense_module.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.data.repository.ExpenseRepository
import kotlinx.coroutines.launch

class CategoryViewModel(private val categoryRepository: ExpenseRepository) : ViewModel() {

    // LiveData holding the list of categories
    private val _categories = MutableLiveData<List<Category>>()
    val categories: LiveData<List<Category>> get() = _categories

    private val _newCategory = MutableLiveData<Category?>()
    val newCategory: LiveData<Category?> get() = _newCategory

    // Rename the function to avoid signature clash
    fun fetchCategories(): LiveData<List<Category>> {
        // Assuming categories are fetched from the repository or database
        viewModelScope.launch {
            val categoriesList = categoryRepository.getAllCategories()  // Your DB call or repository fetch
            _categories.postValue(categoriesList)  // Update LiveData with the fetched categories
        }
        return categories
    }

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

    fun addCategories(categories: List<Category>) {
        viewModelScope.launch {
            categoryRepository.addCategories(categories)
            loadCategoriesByType(categories.firstOrNull()?.type ?: "expense") // Refresh categories after adding
        }
    }

    private fun loadCategoriesByType(type: String) {
        viewModelScope.launch {
            _categories.value = categoryRepository.getCategoriesByType(type)
        }
    }

}
