package com.dsk.myexpense.expense_module.ui.view.settings

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsk.myexpense.expense_module.util.CurrencyCache
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
    fun setDefaultCurrency(context: Context, currency: String) {
        viewModelScope.launch {
            Log.d("DsK","currency -- $currency")
            val currencySymbolValue = Currency.getInstance(currency)
            val symbol = currencySymbolValue.getSymbol(Locale.getDefault(Locale.Category.DISPLAY))
            Log.d("DsK","currency symbol -- $symbol currencySymbolValue $currencySymbolValue")
            CurrencyCache.setCurrencySymbol(context, symbol)
            settingsRepository.setDefaultCurrency(currency)
            _defaultCurrency.value = currency
        }
    }

}
