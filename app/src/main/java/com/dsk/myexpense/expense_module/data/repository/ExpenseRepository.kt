package com.dsk.myexpense.expense_module.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.data.model.Currency
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.data.model.ExpenseInvoiceImage
import com.dsk.myexpense.expense_module.data.model.User
import com.dsk.myexpense.expense_module.data.source.local.db.CategoryDao
import com.dsk.myexpense.expense_module.data.source.local.db.CurrencyDAO
import com.dsk.myexpense.expense_module.data.source.local.db.DailyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.db.ExpenseDAO
import com.dsk.myexpense.expense_module.data.source.local.db.ExpenseTransactionDao
import com.dsk.myexpense.expense_module.data.source.local.db.MonthlyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.db.UserDao
import com.dsk.myexpense.expense_module.data.source.local.db.WeeklyExpenseSum
import com.dsk.myexpense.expense_module.data.source.local.sharedPref.SharedPreferencesManager
import com.dsk.myexpense.expense_module.data.source.network.CurrencyAPIService
import com.dsk.myexpense.expense_module.util.ApiResponse
import com.dsk.myexpense.expense_module.util.Utility
import com.dsk.myexpense.expense_module.util.Utility.bitmapToByteArray

class ExpenseRepository(
    private val expenseDAO: ExpenseDAO,
    private val transactionDao: ExpenseTransactionDao,
    private val categoryDao: CategoryDao,
    private val currencyDao: CurrencyDAO,
    private val currencyAPIService: CurrencyAPIService,
    private var sharedPreferencesManager: SharedPreferencesManager
) {

    val allExpenseDetails: LiveData<List<ExpenseDetails>> = expenseDAO.getAllExpenseDetails()
    val allExpenseDetailRecent: LiveData<List<ExpenseDetails>> = expenseDAO.getAllExpenseDetailsRecent()
    val getTotalIncomeAmount: LiveData<Double> = expenseDAO.getTotalIncome().asLiveData()
    val getTotalExpenseAmount: LiveData<Double> = expenseDAO.getTotalExpense().asLiveData()
    val getTotalIncomeExpenseAmount: LiveData<Double> = expenseDAO.getTotalIncomeExpense().asLiveData()

    fun insertUser(user: User) = sharedPreferencesManager.saveUser(user.name, user.profilePicture)
    fun deleteAllUser() = sharedPreferencesManager.deleteUser()
    fun getUser() = sharedPreferencesManager.getUser()

    suspend fun saveExpenseWithInvoice(
        context: Context,
        expenseDetails: ExpenseDetails, categoryName: String, bitmap: Bitmap?
    ) {
        val expenseWithCategory = getExpenseDetailsWithCategory(expenseDetails, context, categoryName)
        var invoiceImage: ExpenseInvoiceImage? = null
        if (bitmap != null) {
            val byteArray = bitmapToByteArray(bitmap)
            invoiceImage = ExpenseInvoiceImage(
                expenseID = 0, expenseInvoiceImage = byteArray, expenseImageFilePath = ""
            )
        }

        if (invoiceImage != null) {
            transactionDao.insertExpenseWithInvoice(expenseWithCategory, invoiceImage)
        } else {
            transactionDao.insert(expenseWithCategory)
        }
    }

    private suspend fun getExpenseDetailsWithCategory(expenseDetails: ExpenseDetails, context: Context, categoryName: String): ExpenseDetails{
        val type = if (expenseDetails.isIncome) context.resources.getString(R.string.text_income)
        else context.resources.getString(R.string.text_expense)
        val updatedExpenseDetails = Utility.convertExpenseAmountToUSD(context, expenseDetails)
        val category = getCategoryOrInsert(categoryName, type)

        return updatedExpenseDetails.copy(categoryId = category.id)
    }

    suspend fun getCategoryOrInsert(categoryName: String, type: String): Category{
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
    suspend fun insert(expenseDetails: ExpenseDetails) {
        transactionDao.insert(expenseDetails)
    }

    suspend fun insertAllExpense(expenseDetails: List<ExpenseDetails>) {
        expenseDAO.insertAllExpense(expenseDetails)
    }

    suspend fun deleteExpenseDetails(expenseDetails: ExpenseDetails) {
        expenseDAO.delete(expenseDetails)
    }

    suspend fun fetchInvoiceImages(expenseID: Int): List<Bitmap> {
        val images = expenseDAO.getInvoiceImagesForExpense(expenseID)
        return images.mapNotNull { it.expenseInvoiceImage?.let { Utility.byteArrayToBitmap(it) } }
    }

    suspend fun getCategoriesByType(type: String): List<Category> =
        categoryDao.getCategoriesByType(type)

    suspend fun getCategoriesByTypeName(type: String, name: String): Category? =
        categoryDao.getCategoryByNameAndType(type = type, name = name)

    suspend fun insertAllCategories(categories: List<Category>) = categoryDao.insertAll(categories)

    suspend fun insertCategory(categories: Category) = categoryDao.insertCategory(categories)

    suspend fun fetchCurrenciesFromAPI(apiKey: String, currencySymbolsFromJSON: Map<String, String>): ApiResponse<List<Currency>> {
        return try {
            // Fetch the currencies from the API
            val currenciesFromAPI = currencyAPIService.getCurrencies(apiKey)
            Log.d("DsK", "Currencies currenciesFromAPI ${currenciesFromAPI.data?.size}")

            // Create a mutable map to store unique currencies based on name/code
            val uniqueCurrenciesMap = mutableSetOf<Currency>()

            if (currenciesFromAPI is ApiResponse.Success) {
                currenciesFromAPI.data?.let { currencies ->
                    currencies.forEach { currency ->
                        val currencyCode: String = currency.name // Treating the code as String
                        val currencyPrice: Double = currency.code // Treating the code as String

                        // Look up the symbol in the JSON map, or default to code if not found
                        val finalSymbol = currencySymbolsFromJSON[currencyCode]

                        // Fallback to code if the symbol is null or empty
                        val finalSymbolToUse = finalSymbol?.takeIf { it.isNotEmpty() } ?: currencyCode

                        Log.d("DsK", "Currencies name ${currency.name} code $currencyCode symbol $finalSymbolToUse")

                        // Create a Currency object
                        val currencyEntity = Currency(
                            code = currencyPrice,
                            name = currency.name,
                            symbol = finalSymbolToUse
                        )
                        // Only add if the currency is not already in the map
                        uniqueCurrenciesMap.add(currencyEntity)
                    }

                    // Insert all unique currencies into the database
                    currencyDao.insertAll(uniqueCurrenciesMap.toList())

                    // Return the successful API response with data
                    ApiResponse.Success(currencies)
                } ?: ApiResponse.Error("No data available")
            } else {
                ApiResponse.Error(currenciesFromAPI.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.localizedMessage ?: "Unknown error")
        }
    }


    // Save currencies to local database
    suspend fun insertAllCurrencies(currencies: List<Currency>) {
        currencyDao.insertAll(currencies)
    }

    suspend fun insertCurrencies(currencies: Currency) {
        currencyDao.insertCurrency(currencies)
    }

    // Get currencies from local database
    fun getAllCurrencyList(): List<Currency> {
        return currencyDao.getAllCurrencyDetails()
    }

    fun getCurrenciesFromLocalLiveDB(): LiveData<List<Currency>> {
        return currencyDao.getAllCurrencies()
    }

    suspend fun getCategoryNameByID(categoryID: Int): Category? {
        return categoryDao.getCategoryNameByID(categoryID)
    }

    suspend fun updateExpenseWithInvoice(
        context: Context,
        expenseDetails: ExpenseDetails, categoryName: String, isIncome: Boolean, bitmap: Bitmap?
    ) {
        val type = if (isIncome) "Income" else "Expense"
        val updatedExpenseDetails = Utility.convertExpenseAmountToUSD(context, expenseDetails)
        // Ensure category exists or create a new one
        val category = categoryDao.getCategoryByNameAndType(categoryName, type) ?: run {
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

        // Update the expense details with the category ID
        val updatedExpenseDetailsValue = updatedExpenseDetails.copy(categoryId = category.id)

        // Convert the bitmap to byteArray if it's provided
        var invoiceImage: ExpenseInvoiceImage? = null
        if (bitmap != null) {
            val byteArray = bitmapToByteArray(bitmap)
            invoiceImage = ExpenseInvoiceImage(
                expenseID = expenseDetails.expenseID ?: 0,
                expenseInvoiceImage = byteArray,
                expenseImageFilePath = "" // File path can be updated if necessary
            )
        }

        // Perform the database update
        if (invoiceImage != null) {
            expenseDAO.updateExpenseWithInvoice(updatedExpenseDetailsValue, invoiceImage)
        } else {
            expenseDAO.updateExpense(updatedExpenseDetailsValue)
        }
    }

    suspend fun update(expenseDetails: ExpenseDetails) {
        expenseDAO.updateExpense(expenseDetails)
    }

    fun getDailyExpenses(): List<DailyExpenseWithTime> = expenseDAO.getDailyExpenseSum()
    fun getWeeklyExpenses(): List<WeeklyExpenseSum> = expenseDAO.getWeeklyExpenseSum()

    fun getMonthlyExpenses(): List<WeeklyExpenseSum> = expenseDAO.getMonthlyExpenseSum()
    fun getYearlyExpenses(): List<MonthlyExpenseWithTime> = expenseDAO.getYearlyExpenseSum()

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

    fun getAllExpensesLiveData() = expenseDAO.getAllExpensesLiveData()
    fun getAllExpenses() = expenseDAO.getAllExpenses()

    suspend fun getExpensesBetweenDates(startDate: Long, endDate: Long) =
        expenseDAO.getExpensesBetweenDates(startDate, endDate)

    suspend fun getExpensesByCategory(categoryId: Int) = expenseDAO.getExpensesByCategory(categoryId)
}
