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
import com.dsk.myexpense.expense_module.data.repository.CategoryRepository
import com.dsk.myexpense.expense_module.data.repository.CurrencyRepository
import com.dsk.myexpense.expense_module.data.repository.ExpenseRepository
import com.dsk.myexpense.expense_module.data.source.local.db.DailyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.db.MonthlyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.db.WeeklyExpenseSum
import com.dsk.myexpense.expense_module.ui.view.settings.SettingsRepository
import com.dsk.myexpense.expense_module.util.AppConstants
import com.dsk.myexpense.expense_module.util.CurrencyUtils
import com.dsk.myexpense.expense_module.util.Utility
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeDetailsViewModel(
    context: Context,
    private var expenseRepository: ExpenseRepository,
    private var categoryRepository: CategoryRepository,
    private var currencyRepository: CurrencyRepository,
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
            val exchangeRate = CurrencyUtils.getExchangeRate(context)
            value = CurrencyUtils.convertFromUSD(totalIncomeInUSD, exchangeRate)
        }
    }

    private val getTotalExpenseAmount: LiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(expenseRepository.getTotalExpenseAmount) { totalExpenseInUSD ->
            val exchangeRate = CurrencyUtils.getExchangeRate(context)
            value = CurrencyUtils.convertFromUSD(totalExpenseInUSD, exchangeRate)
        }
    }

    private val getTotalIncomeExpenseAmount: LiveData<Double> = MediatorLiveData<Double>().apply {
        addSource(expenseRepository.getTotalIncomeExpenseAmount) { totalIncomeExpenseInUSD ->
            val exchangeRate = CurrencyUtils.getExchangeRate(context)
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

    fun getAllCategoriesLiveData(): LiveData<List<Category>> =
        categoryRepository.getAllCategoriesLiveData()

    fun getAllCurrencyLiveData() = liveData(Dispatchers.IO) {
        emit(currencyRepository.getAllCurrencyList())
    }

    fun getAllExpenses(): List<ExpenseDetails> = expenseRepository.getAllExpenses()

    fun getAllCategories(): List<Category> = categoryRepository.getAllCategories()

    fun getAllCurrency() = currencyRepository.getAllCurrencyList()

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
                // Convert expenses on a background thread
                viewModelScope.launch(Dispatchers.IO) {
                    val convertedExpenses = convertAllExpenseAmount(context, expenses)
                    withContext(Dispatchers.Main) {
                        value = convertedExpenses
                    }
                }
            }
        }

    val allExpenseDetailRecent: LiveData<List<ExpenseDetails>> =
        MediatorLiveData<List<ExpenseDetails>>().apply {
            addSource(expenseRepository.allExpenseDetailRecent) { expenses ->
                // Convert expenses on a background thread
                viewModelScope.launch(Dispatchers.IO) {
                    val convertedExpenses = convertAllExpenseAmount(context, expenses)
                    withContext(Dispatchers.Main) {
                        value = convertedExpenses
                    }
                }
            }
        }

    private fun convertAllExpenseAmount(
        context: Context, expenseDetails: List<ExpenseDetails>
    ): List<ExpenseDetails> {
        // Make sure context is valid, and exchange rate is retrieved properly
        val exchangeRate = CurrencyUtils.getExchangeRate(context)

        // Convert amounts in the expenseDetails list
        return expenseDetails.map { expense ->
            try {
                val convertedAmount = CurrencyUtils.convertFromUSD(expense.amount, exchangeRate)
                expense.copy(amount = convertedAmount)
            } catch (e: Exception) {
                Log.e(
                    "DsK",
                    "Error converting amount for expense ID ${expense.expenseID}: ${e.message}"
                )
                expense // Return original expense in case of an error
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
            categoryRepository.insertAllCategories(category)
        }
    }

    fun insertAllCurrencies(category: List<Currency>) {
        viewModelScope.launch {
            currencyRepository.insertAllCurrencies(category)
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
        return CurrencyUtils.getCurrencySymbol(context) ?: AppConstants.KEY_CURRENCY_VALUE_SYMBOL
    }

    fun saveUser(name: String, profilePicture: String) {
        viewModelScope.launch {
            val user = User(name = name, profilePicture = profilePicture)
            deleteUser()
            expenseRepository.insertUser(user)
            _userDetails.value = user
            _userDetails.emit(user)
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

    suspend fun getExpenseCategoryDetails(categoryName: String, categoryType: String): Category =
        categoryRepository.getCategoryOrInsert(categoryName, categoryType)
}