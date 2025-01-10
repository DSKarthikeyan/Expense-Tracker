package com.dsk.myexpense.expense_module.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.data.repository.ExpenseRepository
import com.dsk.myexpense.expense_module.data.source.local.DailyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.MonthlyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.WeeklyExpenseSum
import com.dsk.myexpense.expense_module.ui.view.settings.SettingsRepository
import com.dsk.myexpense.expense_module.util.Utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeDetailsViewModel(
    private var expenseRepository: ExpenseRepository,
    private var settingsRepository: SettingsRepository
) : ViewModel() {

    val allExpenseDetails: LiveData<List<ExpenseDetails>> = expenseRepository.allExpenseDetails
    private val getTotalIncomeAmount: LiveData<Double> = expenseRepository.getTotalIncomeAmount
    private val getTotalExpenseAmount: LiveData<Double> = expenseRepository.getTotalExpenseAmount
    private val getTotalIncomeExpenseAmount: LiveData<Int> = expenseRepository.getTotalIncomeExpenseAmount

    private val _currencySymbol = MutableLiveData<String>()
    private val currencySymbol: LiveData<String> get() = _currencySymbol

    // MediatorLiveData to combine currency symbol with other amounts
    val combinedLiveData = MediatorLiveData<Pair<String, Triple<Double?, Double?, Double?>>>()

    fun deleteExpenseDetails(expenseDetails: ExpenseDetails) {
        viewModelScope.launch {
            expenseRepository.deleteExpenseDetails(expenseDetails)
        }
    }

    fun insertExpense(
        expenseDetails: ExpenseDetails, bitmap: Bitmap?, categoryName: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (bitmap != null) {
            expenseRepository.saveExpenseWithInvoice(
                expenseDetails,
                categoryName = categoryName,
                bitmap = bitmap,
            )
        } else {
            expenseRepository.insert(expenseDetails)
        }
    }

    fun updateExpense(
        expenseDetails: ExpenseDetails,
        bitmap: Bitmap?,
        categoryName: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        if (bitmap != null) {
            expenseRepository.updateExpenseWithInvoice(
                expenseDetails = expenseDetails,
                categoryName = categoryName,
                bitmap = bitmap,
                isIncome = expenseDetails.isIncome
            )
        } else {
            expenseRepository.update(expenseDetails)
        }
    }

    suspend fun getCategoryNameByID(categoryId: Int): Category? {
        return expenseRepository.getCategoryNameByID(categoryId)
    }


    fun getDailyExpenses():
            List<DailyExpenseWithTime> {
        return expenseRepository.getDailyExpenses()
    }

    fun getWeeklyExpenses():
            List<WeeklyExpenseSum> {
        return expenseRepository.getWeeklyExpenses()
    }

    fun getMonthlyExpenses(): List<WeeklyExpenseSum> {
        return expenseRepository.getMonthlyExpenses()
    }


    fun getYearlyExpenses(): List<MonthlyExpenseWithTime> {
        return expenseRepository.getYearlyExpenses()
    }

    init {
        // Initialize MediatorLiveData with currency symbol and amounts
        combinedLiveData.addSource(currencySymbol) { currency ->
            updateCombinedLiveData(currency = currency)
        }
        combinedLiveData.addSource(getTotalIncomeAmount) { income ->
            updateCombinedLiveData(currencySymbol.value ?: "",income)
        }
        combinedLiveData.addSource(getTotalExpenseAmount) { expense ->
            updateCombinedLiveData(currencySymbol.value ?: "",expense)
        }
        combinedLiveData.addSource(getTotalIncomeExpenseAmount) { balance ->
            Log.d("DsK","balance $balance")
            updateCombinedLiveData(currencySymbol.value ?: "", balance.toDouble())
        }
    }

    private fun updateCombinedLiveData(
        currency: String = "", income: Double? = getTotalIncomeAmount.value,
        expense: Double? = getTotalExpenseAmount.value,
        balance: Double? = getTotalIncomeExpenseAmount.value?.toDouble()) {
        combinedLiveData.value = Pair(
            currency,
            Triple(income, expense, balance)
        )
    }

    fun fetchCurrencySymbol(context: Context) {
        viewModelScope.launch {
            try {
                val symbol = Utility.getCurrencySymbol(context, settingsRepository)
                Log.d("DsK","fetchCurrencySymbol $symbol")
                _currencySymbol.postValue(symbol)
            } catch (e: Exception) {
                _currencySymbol.postValue("Currency Symbol Error: ${e.message}")
            }
        }
    }
}