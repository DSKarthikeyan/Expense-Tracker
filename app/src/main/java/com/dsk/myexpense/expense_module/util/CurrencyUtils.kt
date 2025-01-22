package com.dsk.myexpense.expense_module.util

import android.content.Context
import android.util.Log
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.ui.view.settings.SettingsRepository
import org.json.JSONObject

object CurrencyUtils {

    fun getCurrencySymbol(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(AppConstants.KEY_BASE_CURRENCY_SYMBOL, null)
    }

    fun setCurrencySymbol(context: Context, symbol: String) {
        val sharedPreferences = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(AppConstants.KEY_BASE_CURRENCY_SYMBOL, symbol)
            apply()
        }
    }

    fun getBaseCurrency(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(AppConstants.KEY_BASE_CURRENCY, AppConstants.KEY_CURRENCY_VALUE_NAME) ?: AppConstants.KEY_CURRENCY_VALUE
    }

    fun getExchangeRate(context: Context): Double {
        val sharedPreferences = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getFloat(AppConstants.KEY_EXCHANGE_RATE, 0.0f).toDouble()
    }

    fun setBaseCurrency(context: Context, currency: String, exchangeRate: Double) {
        val sharedPreferences = context.getSharedPreferences(AppConstants.PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString(AppConstants.KEY_BASE_CURRENCY, currency)
            .putFloat(AppConstants.KEY_EXCHANGE_RATE, exchangeRate.toFloat())
            .apply()
    }

    suspend fun getCurrencySymbol(
        context: Context,
        getFromCache: Boolean = true,
        settingsRepository: SettingsRepository? = null,
        currencyCode: String? = null
    ): String {
        if(getFromCache) {
            // Attempt to fetch the symbol from cache
            getCurrencySymbol(context)?.let {
                return it
            }
        }
        // Load currency data from resources
        val currencyMap = loadCurrencyMapFromJSON(context)

        // Determine the currency code to use
        val codeToFetch = currencyCode ?: settingsRepository?.getDefaultCurrency()
        // Get the symbol for the currency code, falling back to the code itself if not found
        val symbol = currencyMap[codeToFetch] ?: codeToFetch

        // Cache the fetched symbol
        setCurrencySymbol(context, symbol!!)

        return symbol
    }

    fun loadCurrencyMapFromJSON(context: Context): Map<String, String> {
        val inputStream = context.resources.openRawResource(R.raw.currencies)
        val jsonString = inputStream.bufferedReader().use { it.readText() }
        val jsonObject = JSONObject(jsonString)

        return jsonObject.keys().asSequence().associateWith { jsonObject.getString(it) }
    }

    /**
     * Converts a given amount from USD to the base currency (e.g., INR).
     * @param amountInUSD The amount in USD.
     * @param exchangeRate The exchange rate (1 USD = exchangeRate in base currency).
     * @return The amount converted to the base currency.
     */
    fun convertFromUSD(amountInUSD: Double, exchangeRate: Double): Double {
        if (exchangeRate > 0) {
          return  amountInUSD * exchangeRate
        } else {
            Log.d("DsK","Exchange rate must be greater than 0")
            return 0.0
        }
    }

    /**
     * Converts a given amount from the base currency (e.g., INR) to USD.
     * @param amountInBaseCurrency The amount in the base currency.
     * @param exchangeRate The exchange rate (1 USD = exchangeRate in base currency).
     * @return The amount converted to USD.
     */
    fun convertToUSD(amountInBaseCurrency: Double, exchangeRate: Double): Double {
         if (exchangeRate > 0) {
             return amountInBaseCurrency / exchangeRate
        } else {
            Log.d("DsK","Exchange rate must be greater than 0")
             return 0.0
        }
    }
}