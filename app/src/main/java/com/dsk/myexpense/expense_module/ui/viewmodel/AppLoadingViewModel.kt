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
import com.dsk.myexpense.expense_module.util.CurrencyUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppLoadingViewModel(private val repository: ExpenseRepository) : ViewModel() {

    /**
     * Initializes the predefined categories in the database if they don't already exist.
     */
    fun initializeCategories(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val expenseType = context.getString(R.string.text_expense)
            if (repository.getCategoriesByType(expenseType).isEmpty()) {
                repository.insertAllCategories(getPredefinedCategories(context))
            } else {
                Log.d("AppLoadingViewModel", "Categories already exist, no insertion needed.")
            }
        }
    }

    /**
     * Retrieves categories by type from the database.
     */
    fun getCategoriesByType(type: String): List<Category> =
        repository.getCategoriesByType(type)


    /**
     * Generates the predefined categories for both income and expenses.
     */
    private fun getPredefinedCategories(context: Context): List<Category> {
        val expenseType = context.getString(R.string.text_expense)
        val incomeType = context.getString(R.string.text_income)

        // Expense categories with names and icons
        val predefinedExpenseCategories = listOf(
            R.string.text_expenses_category_grocery to R.drawable.ic_grocery,
            R.string.text_expenses_category_netflix to R.drawable.ic_netflix,
            R.string.text_expenses_category_rent to R.drawable.ic_rent,
            R.string.text_expenses_category_paypal to R.drawable.ic_paypal,
            R.string.text_expenses_category_starbucks to R.drawable.ic_starbucks,
            R.string.text_expenses_category_shopping to R.drawable.ic_shopping,
            R.string.text_expenses_category_transport to R.drawable.ic_transport,
            R.string.text_expenses_category_utilities to R.drawable.ic_utilities,
            R.string.text_expenses_category_dining_out to R.drawable.ic_dining_out,
            R.string.text_expenses_category_entertainment to R.drawable.ic_entertainment,
            R.string.text_expenses_category_healthcare to R.drawable.ic_healthcare,
            R.string.text_expenses_category_insurance to R.drawable.ic_insurance,
            R.string.text_expenses_category_subscriptions to R.drawable.ic_subscriptions,
            R.string.text_expenses_category_education to R.drawable.ic_education,
            R.string.text_expenses_category_debt to R.drawable.ic_debt,
            R.string.text_expenses_category_gifts to R.drawable.ic_gifts,
            R.string.text_expenses_category_travel to R.drawable.ic_travel,
            R.string.text_expenses_category_other to R.drawable.ic_other_expenses
        )

        // Income categories with names and icons
        val predefinedIncomeCategories = listOf(
            R.string.text_income_category_paypal to R.drawable.ic_paypal,
            R.string.text_income_category_salary to R.drawable.ic_salary,
            R.string.text_income_category_freelance to R.drawable.ic_freelance,
            R.string.text_income_category_investments to R.drawable.ic_investments,
            R.string.text_income_category_bonus to R.drawable.ic_bonus,
            R.string.text_income_category_rental_income to R.drawable.ic_rental_income,
            R.string.text_income_category_other_income to R.drawable.ic_other_income
        )

        // Map expense categories
        val expenseCategories = predefinedExpenseCategories.map { (nameRes, iconRes) ->
            Category(
                name = context.getString(nameRes),
                type = expenseType,
                iconResId = iconRes
            )
        }

        // Map income categories
        val incomeCategories = predefinedIncomeCategories.map { (nameRes, iconRes) ->
            Category(
                name = context.getString(nameRes),
                type = incomeType,
                iconResId = iconRes
            )
        }

        // Combine and return all categories
        return expenseCategories + incomeCategories
    }

    fun fetchAndStoreCurrencies(currencySymbolsFromJSON: Map<String, String>) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val currenciesResponse = repository.fetchCurrenciesFromAPI(AppConstants.CURRENCY_LIST_APP_ID, currencySymbolsFromJSON )) {
                is ApiResponse.Success -> {
                    Log.d("AppLoadingViewModel"," Currency Loading Success ")
                    currenciesResponse.data?.let { repository.insertAllCurrencies(it) }
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
       return repository.getAllCurrencyList()
    }

    suspend fun getCategoryNameByID(categoryId: Int): Category? {
        return repository.getCategoryNameByID(categoryId)
    }

    fun getCurrencySymbol(context: Context): String{
        return CurrencyUtils.getCurrencySymbol(context) ?: AppConstants.KEY_CURRENCY_VALUE_SYMBOL
    }
}
