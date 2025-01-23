package com.dsk.myexpense.expense_module.data.repository

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.data.model.ExpenseInvoiceImage
import com.dsk.myexpense.expense_module.data.model.User
import com.dsk.myexpense.expense_module.data.source.local.db.DailyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.db.ExpenseDAO
import com.dsk.myexpense.expense_module.data.source.local.db.MonthlyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.db.WeeklyExpenseSum
import com.dsk.myexpense.expense_module.data.source.local.sharedPref.SharedPreferencesManager
import com.dsk.myexpense.expense_module.util.AppConstants
import com.dsk.myexpense.expense_module.util.Utility
import com.dsk.myexpense.expense_module.util.Utility.bitmapToByteArray

class ExpenseRepository(
    private val expenseDAO: ExpenseDAO,
    private val categoryRepository: CategoryRepository,
    private var sharedPreferencesManager: SharedPreferencesManager
) {

    val allExpenseDetails: LiveData<List<ExpenseDetails>> = expenseDAO.getAllExpenseDetails()
    val allExpenseDetailRecent: LiveData<List<ExpenseDetails>> =
        expenseDAO.getAllExpenseDetailsRecent()
    val getTotalIncomeAmount: LiveData<Double> = expenseDAO.getTotalIncome().asLiveData()
    val getTotalExpenseAmount: LiveData<Double> = expenseDAO.getTotalExpense().asLiveData()
    val getTotalIncomeExpenseAmount: LiveData<Double> =
        expenseDAO.getTotalIncomeExpense().asLiveData()

    fun insertUser(user: User) = sharedPreferencesManager.saveUser(user.name, user.profilePicture)
    fun deleteAllUser() = sharedPreferencesManager.deleteUser()
    fun getUser() = sharedPreferencesManager.getUser()

    suspend fun saveExpenseWithInvoice(
        context: Context, expenseDetails: ExpenseDetails, categoryName: String, bitmap: Bitmap?
    ) {
        val expenseWithCategory =
            getExpenseDetailsWithCategory(expenseDetails, context, categoryName)
        var invoiceImage: ExpenseInvoiceImage? = null
        if (bitmap != null) {
            val byteArray = bitmapToByteArray(bitmap)
            invoiceImage = ExpenseInvoiceImage(
                expenseID = 0,
                expenseInvoiceImage = byteArray,
                expenseImageFilePath = AppConstants.EMPTY_STRING
            )
        }

        if (invoiceImage != null) {
            expenseDAO.insertExpenseWithInvoice(expenseWithCategory, invoiceImage)
        } else {
            expenseDAO.insert(expenseWithCategory)
        }
    }

    private suspend fun getExpenseDetailsWithCategory(
        expenseDetails: ExpenseDetails, context: Context, categoryName: String
    ): ExpenseDetails {
        val type = if (expenseDetails.isIncome) context.resources.getString(R.string.text_income)
        else context.resources.getString(R.string.text_expense)
        val updatedExpenseDetails = Utility.convertExpenseAmountToUSD(context, expenseDetails)
        val category = categoryRepository.getCategoryOrInsert(categoryName, type)

        return updatedExpenseDetails.copy(categoryId = category.id)
    }

    suspend fun insert(expenseDetails: ExpenseDetails) {
        expenseDAO.insert(expenseDetails)
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

    suspend fun updateExpenseWithInvoice(
        context: Context,
        expenseDetails: ExpenseDetails,
        categoryName: String,
        isIncome: Boolean,
        bitmap: Bitmap?
    ) {
        val type =
            if (isIncome) context.resources.getString(R.string.text_income) else context.resources.getString(
                R.string.text_expense
            )
        val updatedExpenseDetails = Utility.convertExpenseAmountToUSD(context, expenseDetails)
        // Ensure category exists or create a new one
        val category = categoryRepository.getCategoryByNameAndType(categoryName, type)

        // Update the expense details with the category ID
        val updatedExpenseDetailsValue = updatedExpenseDetails.copy(categoryId = category.id)

        // Convert the bitmap to byteArray if it's provided
        var invoiceImage: ExpenseInvoiceImage? = null
        if (bitmap != null) {
            val byteArray = bitmapToByteArray(bitmap)
            invoiceImage = ExpenseInvoiceImage(
                expenseID = expenseDetails.expenseID ?: 0,
                expenseInvoiceImage = byteArray,
                expenseImageFilePath = AppConstants.EMPTY_STRING // File path can be updated if necessary
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

    fun getAllExpensesLiveData() = expenseDAO.getAllExpensesLiveData()
    fun getAllExpenses() = expenseDAO.getAllExpenses()

    suspend fun getExpensesBetweenDates(startDate: Long, endDate: Long) =
        expenseDAO.getExpensesBetweenDates(startDate, endDate)

    suspend fun getExpensesByCategory(categoryId: Int) =
        expenseDAO.getExpensesByCategory(categoryId)
}
