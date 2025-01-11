package com.dsk.myexpense.expense_module.ui.view.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsk.myexpense.expense_module.util.CurrencyCache
import com.dsk.myexpense.expense_module.util.CurrencyUtils
import com.dsk.myexpense.expense_module.util.Utility
import kotlinx.coroutines.launch
import java.util.Currency
import java.util.Locale


class SettingsViewModel(private val settingsRepository: SettingsRepository) : ViewModel() {

    private val _darkModeEnabled = MutableLiveData<Boolean>()
    val darkModeEnabled: LiveData<Boolean> get() = _darkModeEnabled

    private val _defaultCurrency = MutableLiveData<String>()
    val defaultCurrency: LiveData<String> get() = _defaultCurrency

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
            val symbol = CurrencyUtils.getCurrencySymbol(context, getFromCache = false, currencyCode = currency)
//            Log.d("SettingsViewModel","Currency setDefaultCurrency: symbol $symbol -- currency $currency -- currencyValue $currencyValue")
            // Cache the symbol and base currency
            CurrencyCache.setCurrencySymbol(context, symbol)
            CurrencyCache.setBaseCurrency(context, currency, currencyValue)

            // Persist the default currency in the repository
            settingsRepository.setDefaultCurrency(currency)

            // Update the LiveData with the new default currency
            _defaultCurrency.value = currency
        }
    }

}
