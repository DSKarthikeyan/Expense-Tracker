package com.dsk.myexpense.expense_module.util

import android.content.Context
import android.util.Log
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.ui.view.settings.SettingsRepository
import java.util.Locale

object CurrencyUtils {

    // Utility function to fetch and cache currency symbol using SharedPreferences
    suspend fun getCurrencySymbol(
        context: Context,
        settingsRepository: SettingsRepository
    ): String {
        // Attempt to fetch from cache first (if any caching system is in place)
        CurrencyCache.getCurrencySymbol(context)?.let {
            return it
        }

        // Get the default currency code from the settings repository
        val defaultCurrencyCode = settingsRepository.getDefaultCurrency()

        // Load the currency codes and symbols from XML resources
        val currencyCodes = context.resources.getStringArray(R.array.currency_codes)
        val currencySymbols = context.resources.getStringArray(R.array.currency_symbols)

        // Create a map of currency codes to symbols
        val currencyMap = mutableMapOf<String, String>()
        for (i in currencyCodes.indices) {
            // Ensure mapping the code to the symbol
            currencyMap[currencyCodes[i]] = currencySymbols.getOrElse(i) { currencyCodes[i] }
        }

        // Retrieve the symbol for the default currency
        val symbol = currencyMap[defaultCurrencyCode] ?: defaultCurrencyCode

        // Cache and return the symbol
        CurrencyCache.setCurrencySymbol(context, symbol)
        return symbol
    }

    fun getCurrencySymbolFromXML(context: Context, currencyCode: String): String {
        // Load the currency codes and symbols from XML resources
        val currencyCodes = context.resources.getStringArray(R.array.currency_codes)
        val currencySymbols = context.resources.getStringArray(R.array.currency_symbols)

        // Create a map of currency codes to symbols
        val currencyMap = mutableMapOf<String, String>()
        for (i in currencyCodes.indices) {
            // Ensure mapping the code to the symbol
            currencyMap[currencyCodes[i]] = currencySymbols.getOrElse(i) { currencyCodes[i] }
        }

        // Retrieve the symbol for the given currency code
        return currencyMap[currencyCode] ?: currencyCode // fallback to code if no symbol found
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