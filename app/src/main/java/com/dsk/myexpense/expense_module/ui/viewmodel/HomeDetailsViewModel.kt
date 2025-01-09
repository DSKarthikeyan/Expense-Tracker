package com.dsk.myexpense.expense_module.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.data.repository.ExpenseRepository
import com.dsk.myexpense.expense_module.data.source.local.DailyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.MonthlyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.WeeklyExpenseSum
import com.dsk.myexpense.expense_module.data.source.local.WeeklyExpenseWithTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HomeDetailsViewModel(private var repository: ExpenseRepository) : ViewModel() {

    val allExpenseDetails: LiveData<List<ExpenseDetails>> = repository.allExpenseDetails
    val getTotalIncomeAmount: LiveData<Int> = repository.getTotalIncomeAmount
    val getTotalExpenseAmount: LiveData<Int> = repository.getTotalExpenseAmount
    val getTotalIncomeExpenseAmount: LiveData<Int> = repository.getTotalIncomeExpenseAmount

    fun deleteExpenseDetails(expenseDetails: ExpenseDetails) {
        viewModelScope.launch {
            repository.deleteExpenseDetails(expenseDetails)
        }
    }

    fun insertExpense(
        expenseDetails: ExpenseDetails, bitmap: Bitmap?, categoryName: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (bitmap != null) {
            repository.saveExpenseWithInvoice(
                expenseDetails,
                categoryName = categoryName,
                bitmap = bitmap,
            )
        } else {
            repository.insert(expenseDetails)
        }
    }

    fun updateExpense(
        expenseDetails: ExpenseDetails,
        bitmap: Bitmap?,
        categoryName: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (bitmap != null) {
            repository.updateExpenseWithInvoice(
                expenseDetails = expenseDetails,
                categoryName = categoryName,
                bitmap = bitmap,
                isIncome = expenseDetails.isIncome
            )
        } else {
            repository.update(expenseDetails)
        }
    }

    suspend fun getCategoryNameByID(categoryId: Int): Category? {
        return repository.getCategoryNameByID(categoryId)
    }


    fun getDailyExpenses():
            List<DailyExpenseWithTime> {
        return repository.getDailyExpenses()
    }

    fun getWeeklyExpenses():
            List<WeeklyExpenseSum> {
        return repository.getWeeklyExpenses()
    }

    fun getMonthlyExpenses(): List<WeeklyExpenseSum> {
        return repository.getMonthlyExpenses()
    }


    fun getYearlyExpenses(): List<MonthlyExpenseWithTime> {
        return repository.getYearlyExpenses()
    }

}