package com.dsk.myexpense.expense_module.ui.view.settings

class SettingsRepository(private val settingsDataStore: SettingsDataStore) {

    suspend fun getDarkModeSetting(): Boolean {
        return settingsDataStore.getDarkModeSetting()
    }

    suspend fun setDarkModeSetting(enabled: Boolean) {
        settingsDataStore.setDarkModeSetting(enabled)
    }

    suspend fun getDefaultCurrency(): String {
        return settingsDataStore.getDefaultCurrency()
    }

    suspend fun getDefaultCurrencyValue(): Double {
        return settingsDataStore.getDefaultCurrencyValue()
    }

    suspend fun setDefaultCurrency(currency: String,currencyValue: Double) {
        settingsDataStore.setDefaultCurrency(currency, currencyValue)
    }

}