package com.dsk.myexpense.expense_module.ui.view.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.dsk.myexpense.expense_module.data.model.Currency
import kotlinx.coroutines.launch


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
    fun setDefaultCurrency(currency: String) {
        viewModelScope.launch {
            settingsRepository.setDefaultCurrency(currency)
            _defaultCurrency.value = currency
        }
    }

}
