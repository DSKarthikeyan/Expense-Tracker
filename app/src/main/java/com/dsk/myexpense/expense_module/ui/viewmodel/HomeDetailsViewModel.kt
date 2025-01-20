package com.dsk.myexpense.expense_module.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.data.model.Currency
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.data.model.User
import com.dsk.myexpense.expense_module.data.repository.ExpenseRepository
import com.dsk.myexpense.expense_module.data.source.local.db.DailyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.db.MonthlyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.db.WeeklyExpenseSum
import com.dsk.myexpense.expense_module.data.source.local.sharedPref.SharedPreferencesManager
import com.dsk.myexpense.expense_module.ui.view.settings.SettingsRepository
import com.dsk.myexpense.expense_module.util.CurrencyCache
import com.dsk.myexpense.expense_module.util.CurrencyUtils
import com.dsk.myexpense.expense_module.util.Utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _userDetails = MutableStateFlow<User?>(null)
    val userDetails: LiveData<User?> = _userDetails.asStateFlow().asLiveData()

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

    private val getTotalIncomeExpenseAmount: LiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(expenseRepository.getTotalIncomeExpenseAmount) { totalIncomeExpenseInUSD ->
            val exchangeRate = CurrencyCache.getExchangeRate(context)
            value = CurrencyUtils.convertFromUSD(totalIncomeExpenseInUSD.toDouble(), exchangeRate)
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
            updateCombinedLiveData(currencySymbol.value ?: "", balance = balance)
        }
    }

    fun getExpensesByCategory(categoryId: Int) = liveData(Dispatchers.IO) {
        emit(expenseRepository.getExpensesByCategory(categoryId))
    }

    fun getExpensesBetweenDates(startDate: Long, endDate: Long) = liveData(Dispatchers.IO) {
        emit(expenseRepository.getExpensesBetweenDates(startDate, endDate))
    }

    fun getAllCategoriesLiveData(): LiveData<List<Category>> = expenseRepository.getAllCategoriesLiveData()

    fun getAllCurrencyLiveData() = liveData(Dispatchers.IO) {
        emit(expenseRepository.getAllCurrencyList())
    }

    fun getAllExpenses(): List<ExpenseDetails> = expenseRepository.getAllExpenses()

    fun getAllCategories(): List<Category> = expenseRepository.getAllCategories()

    fun getAllCurrency() = expenseRepository.getAllCurrencyList()

    private fun updateCombinedLiveData(
        currency: String = "",
        income: Double? = getTotalIncomeAmount.value,
        expense: Double? = getTotalExpenseAmount.value,
        balance: Double? = getTotalIncomeExpenseAmount.value?.toDouble()
    ) {
        combinedLiveData.value = Pair(
            currency, Triple(income, expense, balance)
        )
    }

    val allExpenseDetails: LiveData<List<ExpenseDetails>> =
        MediatorLiveData<List<ExpenseDetails>>().apply {
            addSource(expenseRepository.allExpenseDetails) { expenses ->
                // Make sure context is valid, and exchange rate is retrieved properly
                val exchangeRate = CurrencyCache.getExchangeRate(context)

                // Log to ensure exchangeRate and amount conversion is working correctly
                Log.d("DsK", "Exchange Rate: $exchangeRate")

                // Update the LiveData value after conversion
                value = expenses.map { expense ->
                    try {
                        val convertedAmount =
                            CurrencyUtils.convertFromUSD(expense.amount, exchangeRate)
                        Log.d("DsK", "Converted amount: $convertedAmount")
                        expense.copy(amount = convertedAmount)
                    } catch (e: Exception) {
                        Log.e("DsK", "Error converting amount: ${e.message}")
                        expense // return original expense in case of error
                    }
                }
            }
        }

    fun deleteExpenseDetails(expenseDetails: ExpenseDetails) {
        viewModelScope.launch {
            expenseRepository.deleteExpenseDetails(expenseDetails)
        }
    }

    fun insertExpense(
        context: Context, expenseDetails: ExpenseDetails, bitmap: Bitmap?, categoryName: String
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
        context: Context, expenseDetails: ExpenseDetails, bitmap: Bitmap?, categoryName: String
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

    fun insertExpenseDetails(expenseDetails: List<ExpenseDetails>) {
        viewModelScope.launch {
            expenseRepository.insertAllExpense(expenseDetails)
        }
    }

    fun insertAllCategory(category: List<Category>) {
        viewModelScope.launch {
            expenseRepository.insertAllCategories(category)
        }
    }

    fun insertAllCurrencies(category: List<Currency>) {
        viewModelScope.launch {
            expenseRepository.insertAllCurrencies(category)
        }
    }


    fun getDailyExpenses(): List<DailyExpenseWithTime> {
        return expenseRepository.getDailyExpenses()
    }

    fun getWeeklyExpenses(): List<WeeklyExpenseSum> {
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
                val symbol = CurrencyUtils.getCurrencySymbol(
                    context, settingsRepository = settingsRepository
                )
                _currencySymbol.postValue(symbol)
            } catch (e: Exception) {
                _currencySymbol.postValue("Currency Symbol Error: ${e.message}")
            }
        }
    }

    fun getCurrencySymbol(context: Context): String {
        return CurrencyCache.getCurrencySymbol(context) ?: "$"
    }

    fun saveUser(name: String, profilePicture: String) {
        Log.d("DsK","View Model in")
        viewModelScope.launch {
            val user = User(name = name, profilePicture = profilePicture)
            deleteUser()
            Log.d("DsK","View Model in 22")
            expenseRepository.insertUser(user)
            _userDetails.value = user
            _userDetails.emit(user)
            Log.d("DsK","View Model ${user.name}")
        }
    }

    fun fetchUser() {
        viewModelScope.launch {
            val user = expenseRepository.getUser()
            _userDetails.value = user
            _userDetails.emit(user) // Update StateFlow with SharedPreferences data
        }
    }

    private fun deleteUser() {
        expenseRepository.deleteAllUser()
        _userDetails.value = null // Clear StateFlow
    }
}