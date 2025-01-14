package com.dsk.myexpense.expense_module.ui.view.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.dataStore by preferencesDataStore("settings")

class SettingsDataStore private constructor(private val context: Context) {

    companion object {
        // Keys for storing preferences
        private val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
        private val DEFAULT_CURRENCY_KEY = stringPreferencesKey("default_currency")
        private val DEFAULT_CURRENCY_VALUE = doublePreferencesKey("default_currency_value")

        @Volatile
        private var INSTANCE: SettingsDataStore? = null

        fun getInstance(context: Context): SettingsDataStore {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: SettingsDataStore(context).also { INSTANCE = it }
            }
        }
    }

    // Function to get dark mode setting
    suspend fun getDarkModeSetting(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[DARK_MODE_KEY] ?: false
    }

    // Function to set dark mode setting
    suspend fun setDarkModeSetting(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = enabled
        }
    }

    // Function to get default currency
    suspend fun getDefaultCurrency(): String {
        val preferences = context.dataStore.data.first()
        return preferences[DEFAULT_CURRENCY_KEY] ?: "USD"
    }

    suspend fun getDefaultCurrencyValue(): Double {
        val preferences = context.dataStore.data.first()
        return preferences[DEFAULT_CURRENCY_VALUE] ?: 1.0
    }

    // Function to set default currency
    suspend fun setDefaultCurrency(currency: String, currencyValue: Double) {
        context.dataStore.edit { preferences ->
            preferences[DEFAULT_CURRENCY_KEY] = currency
            preferences[DEFAULT_CURRENCY_VALUE] = currencyValue
        }
    }
}
