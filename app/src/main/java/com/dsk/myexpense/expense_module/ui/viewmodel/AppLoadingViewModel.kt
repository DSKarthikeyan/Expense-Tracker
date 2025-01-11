package com.dsk.myexpense.expense_module.ui.viewmodel

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.data.model.Currency
import com.dsk.myexpense.expense_module.data.repository.ExpenseRepository
import com.dsk.myexpense.expense_module.util.ApiResponse
import com.dsk.myexpense.expense_module.util.AppConstants
import com.dsk.myexpense.expense_module.util.Utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date

class AppLoadingViewModel(private val repository: ExpenseRepository) : ViewModel() {

    /**
     * Initializes the predefined categories in the database if they don't already exist.
     */
    fun initializeCategories(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val expenseType = context.getString(R.string.text_expense)
            if (repository.getCategoriesByType(expenseType).isEmpty()) {
                repository.insertAll(getPredefinedCategories(context))
            } else {
                Log.d("AppLoadingViewModel", "Categories already exist, no insertion needed.")
            }
        }
    }

    /**
     * Retrieves categories by type from the database.
     */
    suspend fun getCategoriesByType(type: String): List<Category> =
        withContext(Dispatchers.IO) {
            repository.getCategoriesByType(type)
        }

    /**
     * Generates the predefined categories for both income and expenses.
     */
    private fun getPredefinedCategories(context: Context): List<Category> {
        val expenseType = context.getString(R.string.text_expense)
        val incomeType = context.getString(R.string.text_income)

        val predefinedExpenseCategories = listOf(
            "grocery" to R.drawable.ic_grocery,
            "netflix" to R.drawable.ic_netflix,
            "rent" to R.drawable.ic_rent,
            "paypal" to R.drawable.ic_paypal,
            "starbucks" to R.drawable.ic_starbucks,
            "shopping" to R.drawable.ic_shopping,
            "transport" to R.drawable.ic_transport,
            "utilities" to R.drawable.ic_utilities,
            "dining_out" to R.drawable.ic_dining_out,
            "entertainment" to R.drawable.ic_entertainment,
            "healthcare" to R.drawable.ic_healthcare,
            "insurance" to R.drawable.ic_insurance,
            "subscriptions" to R.drawable.ic_subscriptions,
            "education" to R.drawable.ic_education,
            "debt" to R.drawable.ic_debt,
            "gifts" to R.drawable.ic_gifts,
            "travel" to R.drawable.ic_travel,
            "other" to R.drawable.ic_other_expenses
        ).map { (key, icon) ->
            Category(
                name = Utility.getResourcesName("text_expenses_category_", key, context),
                type = expenseType,
                iconResId = icon
            )
        }

        val predefinedIncomeCategories = listOf(
            "paypal" to R.drawable.ic_paypal,
            "salary" to R.drawable.ic_salary,
            "freelance" to R.drawable.ic_freelance,
            "investments" to R.drawable.ic_investments,
            "bonus" to R.drawable.ic_bonus,
            "rental_income" to R.drawable.ic_rental_income,
            "other_income" to R.drawable.ic_other_income
        ).map { (key, icon) ->
            Category(
                name = Utility.getResourcesName("text_income_category_", key, context),
                type = incomeType,
                iconResId = icon
            )
        }

        return predefinedExpenseCategories + predefinedIncomeCategories
    }

    fun fetchAndStoreCurrencies(currencySymbolsFromJSON: Map<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val currenciesResponse = repository.fetchCurrenciesFromAPI(AppConstants.CURRENCY_LIST_APP_ID, currencySymbolsFromJSON )) {
                is ApiResponse.Success -> {
                    Log.d("AppLoadingViewModel"," Currency Loading Success ")
                    currenciesResponse.data?.let { repository.saveCurrenciesToLocalDB(it) }
                }
                is ApiResponse.Error -> {
                    Log.e("AppLoadingViewModel", "Error fetching currencies: ${currenciesResponse.message}")
                }
                is ApiResponse.Loading -> {
                    // Handle loading state if needed
                }
            }
        }
    }

    // Fetch currencies from API
    val allCurrencies: LiveData<List<Currency>> = repository.getCurrenciesFromLocalLiveDB()

    fun getCurrenciesFromLocalDB(): List<Currency>{
       return repository.getCurrenciesFromLocalDB()
    }

    suspend fun getCategoryNameByID(categoryId: Int): Category? {
        return repository.getCategoryNameByID(categoryId)
    }
}
