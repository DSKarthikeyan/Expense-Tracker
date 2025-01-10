package com.dsk.myexpense.expense_module.data.repository

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.data.model.Currency
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.data.model.ExpenseInvoiceImage
import com.dsk.myexpense.expense_module.data.source.local.CategoryDao
import com.dsk.myexpense.expense_module.data.source.local.CurrencyDAO
import com.dsk.myexpense.expense_module.data.source.local.DailyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.ExpenseDAO
import com.dsk.myexpense.expense_module.data.source.local.ExpenseTransactionDao
import com.dsk.myexpense.expense_module.data.source.local.MonthlyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.WeeklyExpenseSum
import com.dsk.myexpense.expense_module.data.source.local.WeeklyExpenseWithTime
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
) {

    val allExpenseDetails: LiveData<List<ExpenseDetails>> = expenseDAO.getAllExpenseDetails()
    val getTotalIncomeAmount: LiveData<Double> = expenseDAO.getTotalIncome().asLiveData()
    val getTotalExpenseAmount: LiveData<Double> = expenseDAO.getTotalExpense().asLiveData()
    val getTotalIncomeExpenseAmount: LiveData<Double> = expenseDAO.getTotalIncomeExpense().asLiveData()

    suspend fun saveExpenseWithInvoice(
        expenseDetails: ExpenseDetails, categoryName: String, bitmap: Bitmap?
    ) {
        val type = if (expenseDetails.isIncome) "Income" else "Expense"

        val category = categoryDao.getCategoryByNameAndType(categoryName, type) ?: run {
            val existingCategory = categoryDao.getCategoriesByType(type).firstOrNull()

            if (existingCategory == null) {
                val defaultCategory = Category(
                    name = "Default $type",
                    type = type,
                    iconResId = R.drawable.ic_other_expenses
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

        val expenseWithCategory = expenseDetails.copy(categoryId = category.id)

        var invoiceImage: ExpenseInvoiceImage? = null
        if (bitmap != null) {
            val byteArray = bitmapToByteArray(bitmap)
            invoiceImage = ExpenseInvoiceImage(
                expenseID = 0,
                expenseInvoiceImage = byteArray,
                expenseImageFilePath = ""
            )
        }

        if (invoiceImage != null) {
            transactionDao.insertExpenseWithInvoice(expenseWithCategory, invoiceImage)
        } else {
            transactionDao.insert(expenseWithCategory)
        }
    }

    suspend fun insert(expenseDetails: ExpenseDetails) {
        transactionDao.insert(expenseDetails)
    }

    suspend fun deleteExpenseDetails(expenseDetails: ExpenseDetails) {
        expenseDAO.delete(expenseDetails)
    }

    suspend fun fetchInvoiceImages(expenseID: Int): List<Bitmap> {
        val images = expenseDAO.getInvoiceImagesForExpense(expenseID)
        return images.mapNotNull { it?.expenseInvoiceImage?.let { Utility.byteArrayToBitmap(it) } }
    }

    suspend fun getCategoriesByType(type: String): List<Category> =
        categoryDao.getCategoriesByType(type)

    suspend fun getCategoriesByTypeName(type: String, name: String): Category? =
        categoryDao.getCategoryByNameAndType(type = type, name = name)

    suspend fun insertAll(categories: List<Category>) = categoryDao.insertAll(categories)

    suspend fun fetchCurrenciesFromAPI(apiKey: String): ApiResponse<List<Currency>> {
        return try {
            val currenciesFromAPI = currencyAPIService.getCurrencies(apiKey)
            if (currenciesFromAPI is ApiResponse.Success) {
                Log.d("DsK","insertion Success ${currenciesFromAPI.data}")
                currenciesFromAPI.data?.let { currencyDao.insertAll(it) }
                ApiResponse.Success(currenciesFromAPI.data)
            } else {
                ApiResponse.Error("Error fetching currencies: ${currenciesFromAPI.message}")
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.localizedMessage ?: "Unknown error")
        }
    }

    // Save currencies to local database
    suspend fun saveCurrenciesToLocalDB(currencies: List<Currency>) {
        currencyDao.insertAll(currencies)
    }

    // Get currencies from local database
    fun getCurrenciesFromLocalDB(): List<Currency> {
        return currencyDao.getAllCurrencyDetails()
    }

    fun getCurrenciesFromLocalLiveDB(): LiveData<List<Currency>> {
        return currencyDao.getAllCurrencies()
    }

    suspend fun getCategoryNameByID(categoryID: Int): Category? {
        return categoryDao.getCategoryNameByID(categoryID)
    }

    suspend fun updateExpenseWithInvoice(
        expenseDetails: ExpenseDetails,
        categoryName: String,
        isIncome: Boolean,
        bitmap: Bitmap?
    ) {
        val type = if (isIncome) "Income" else "Expense"

        // Ensure category exists or create a new one
        val category = categoryDao.getCategoryByNameAndType(categoryName, type) ?: run {
            val existingCategory = categoryDao.getCategoriesByType(type).firstOrNull()

            if (existingCategory == null) {
                val defaultCategory = Category(
                    name = "Default $type",
                    type = type,
                    iconResId = R.drawable.ic_other_expenses
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
        val updatedExpenseDetails = expenseDetails.copy(categoryId = category.id)

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
            expenseDAO.updateExpenseWithInvoice(updatedExpenseDetails, invoiceImage)
        } else {
            expenseDAO.updateExpense(updatedExpenseDetails)
        }
    }

    suspend fun update(expenseDetails: ExpenseDetails) {
        expenseDAO.updateExpense(expenseDetails)
    }

    fun getDailyExpenses(): List<DailyExpenseWithTime> = expenseDAO.getDailyExpenseSum()
    fun getWeeklyExpenses():
            List<WeeklyExpenseSum> = expenseDAO.getWeeklyExpenseSum()
    fun getMonthlyExpenses(): List<WeeklyExpenseSum> = expenseDAO.getMonthlyExpenseSum()
    fun getYearlyExpenses(): List<MonthlyExpenseWithTime> = expenseDAO.getYearlyExpenseSum()
}
