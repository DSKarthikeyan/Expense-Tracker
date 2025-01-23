package com.dsk.myexpense.expense_module.ui.view.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.data.repository.CategoryRepository
import com.dsk.myexpense.expense_module.util.CurrencyUtils
import kotlinx.coroutines.launch


class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val categoryRepository: CategoryRepository,
) : ViewModel() {

    private val _darkModeEnabled = MutableLiveData<Boolean>()
    val darkModeEnabled: LiveData<Boolean> get() = _darkModeEnabled

    private val _defaultCurrency = MutableLiveData<String>()
    val defaultCurrency: LiveData<String> get() = _defaultCurrency

    private val _defaultCurrencyValue = MutableLiveData<Double>()
    val defaultCurrencyValue: LiveData<Double> get() = _defaultCurrencyValue

    private val _selectedCategory = MutableLiveData<Category?>()
    val selectedCategory: LiveData<Category?> = _selectedCategory

    init {
        loadSettings()
    }

    /**
     * Loads the initial settings values from the repository.
     */
    private fun loadSettings() {
        viewModelScope.launch {
            _darkModeEnabled.value = settingsRepository.getDarkModeSetting()
            _defaultCurrency.value = settingsRepository.getDefaultCurrency()
            _defaultCurrencyValue.value = settingsRepository.getDefaultCurrencyValue()
        }
    }

    /**
     * Updates the Dark Mode setting and persists it in the repository.
     */
    fun setDarkModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDarkModeSetting(enabled)
            _darkModeEnabled.value = enabled
        }
    }

    /**
     * Updates the default currency and persists it in the repository.
     */
    fun setDefaultCurrency(context: Context, currency: String, currencyValue: Double) {
        viewModelScope.launch {
            // Use the utility function to get the symbol from the XML
            val symbol = CurrencyUtils.getCurrencySymbol(
                context,
                getFromCache = false,
                currencyCode = currency
            )
//            Log.d("SettingsViewModel","Currency setDefaultCurrency: symbol $symbol -- currency $currency -- currencyValue $currencyValue")
            // Cache the symbol and base currency
            CurrencyUtils.setCurrencySymbol(context, symbol)
            CurrencyUtils.setBaseCurrency(context, currency, currencyValue)

            // Persist the default currency in the repository
            settingsRepository.setDefaultCurrency(currency, currencyValue)

            // Update the LiveData with the new default currency
            _defaultCurrency.value = currency
        }
    }

    fun setSelectedCategory(category: Category) {
        _selectedCategory.value = category
        saveCategoryToDatabase(category) // Save to DB as well
    }

    private fun saveCategoryToDatabase(category: Category) {
        // Save the selected category to the database (assuming you have a repository that handles DB operations)
        viewModelScope.launch {
            categoryRepository.addCategory(category)
        }
    }
}
