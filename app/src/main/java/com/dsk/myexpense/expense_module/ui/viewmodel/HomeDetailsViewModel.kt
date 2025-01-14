package com.dsk.myexpense.expense_module.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.data.repository.ExpenseRepository
import com.dsk.myexpense.expense_module.data.source.local.DailyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.MonthlyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.WeeklyExpenseSum
import com.dsk.myexpense.expense_module.ui.view.settings.SettingsRepository
import com.dsk.myexpense.expense_module.util.CurrencyCache
import com.dsk.myexpense.expense_module.util.CurrencyUtils
import com.dsk.myexpense.expense_module.util.Utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class HomeDetailsViewModel(
    context: Context,
    private var expenseRepository: ExpenseRepository,
    private var settingsRepository: SettingsRepository
) : ViewModel() {

    private val _currencySymbol = MutableLiveData<String>()
    val currencySymbol: LiveData<String> get() = _currencySymbol

    // MediatorLiveData to combine currency symbol with other amounts
    val combinedLiveData = MediatorLiveData<Pair<String, Triple<Double?, Double?, Double?>>>()

    private val getTotalIncomeAmount: LiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(expenseRepository.getTotalIncomeAmount) { totalIncomeInUSD ->
            val exchangeRate = CurrencyCache.getExchangeRate(context)
            value = CurrencyUtils.convertFromUSD(totalIncomeInUSD, exchangeRate)
        }
    }

    private val getTotalExpenseAmount: LiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(expenseRepository.getTotalExpenseAmount) { totalExpenseInUSD ->
            val exchangeRate = CurrencyCache.getExchangeRate(context)
            value = CurrencyUtils.convertFromUSD(totalExpenseInUSD, exchangeRate)
        }
    }

    private val getTotalIncomeExpenseAmount: LiveData<Int> = MediatorLiveData<Int>().apply {
        addSource(expenseRepository.getTotalIncomeExpenseAmount) { totalIncomeExpenseInUSD ->
            val exchangeRate = CurrencyCache.getExchangeRate(context)
            value = CurrencyUtils.convertFromUSD(totalIncomeExpenseInUSD.toDouble(), exchangeRate).toInt()
        }
    }

    init {
        // Initialize MediatorLiveData with currency symbol and amounts
        combinedLiveData.addSource(currencySymbol) { currency ->
            updateCombinedLiveData(currency = currency)
        }
        combinedLiveData.addSource(getTotalIncomeAmount) { income ->
            updateCombinedLiveData(currencySymbol.value ?: "", income = income)
        }
        combinedLiveData.addSource(getTotalExpenseAmount) { expense ->
            updateCombinedLiveData(currencySymbol.value ?: "", expense = expense)
        }
        combinedLiveData.addSource(getTotalIncomeExpenseAmount) { balance ->
            updateCombinedLiveData(currencySymbol.value ?: "", balance = balance.toDouble())
        }
    }

    fun getAllExpenses() = liveData(Dispatchers.IO) {
        emit(expenseRepository.getAllExpenses())
    }

    fun getExpensesByCategory(categoryId: Int) = liveData(Dispatchers.IO) {
        emit(expenseRepository.getExpensesByCategory(categoryId))
    }

    fun getExpensesBetweenDates(startDate: Long, endDate: Long) = liveData(Dispatchers.IO) {
        emit(expenseRepository.getExpensesBetweenDates(startDate, endDate))
    }

    fun getAllCategories() = liveData(Dispatchers.IO) {
        emit(expenseRepository.getAllCategories())
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

    val allExpenseDetails: LiveData<List<ExpenseDetails>> = MediatorLiveData<List<ExpenseDetails>>().apply {
        addSource(expenseRepository.allExpenseDetails) { expenses ->
            // Make sure context is valid, and exchange rate is retrieved properly
            val exchangeRate = CurrencyCache.getExchangeRate(context)

            // Log to ensure exchangeRate and amount conversion is working correctly
            Log.d("DsK", "Exchange Rate: $exchangeRate")

            // Update the LiveData value after conversion
            value = expenses.map { expense ->
                // Log the conversion
                val convertedAmount = CurrencyUtils.convertFromUSD(expense.amount, exchangeRate)
                expense.copy(amount = convertedAmount)
            }
        }
    }

    fun deleteExpenseDetails(expenseDetails: ExpenseDetails) {
        viewModelScope.launch {
            expenseRepository.deleteExpenseDetails(expenseDetails)
        }
    }

    fun insertExpense(
        context: Context,
        expenseDetails: ExpenseDetails,
        bitmap: Bitmap?,
        categoryName: String
    ) = viewModelScope.launch(Dispatchers.IO) {

        val updatedExpenseDetails = Utility.convertExpenseAmountToUSD(context, expenseDetails)

        if (bitmap != null) {
            expenseRepository.saveExpenseWithInvoice(
                context = context,
                expenseDetails = updatedExpenseDetails,
                categoryName = categoryName,
                bitmap = bitmap
            )
        } else {
            expenseRepository.insert(updatedExpenseDetails)
        }
    }

    fun updateExpense(
        context: Context,
        expenseDetails: ExpenseDetails,
        bitmap: Bitmap?,
        categoryName: String
    ) = viewModelScope.launch(Dispatchers.IO) {
        val updatedExpenseDetails = Utility.convertExpenseAmountToUSD(context, expenseDetails)

        if (bitmap != null) {
            expenseRepository.updateExpenseWithInvoice(
                context,
                expenseDetails = updatedExpenseDetails,
                categoryName = categoryName,
                bitmap = bitmap,
                isIncome = expenseDetails.isIncome
            )
        } else {
            expenseRepository.update(updatedExpenseDetails)
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

    fun fetchCurrencySymbol(context: Context) {
        viewModelScope.launch {
            try {
                val symbol = CurrencyUtils.getCurrencySymbol(context, settingsRepository = settingsRepository)
                _currencySymbol.postValue(symbol)
            } catch (e: Exception) {
                _currencySymbol.postValue("Currency Symbol Error: ${e.message}")
            }
        }
    }

}