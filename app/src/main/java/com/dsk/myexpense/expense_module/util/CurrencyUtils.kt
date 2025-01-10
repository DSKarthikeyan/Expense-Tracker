package com.dsk.myexpense.expense_module.util

import android.content.Context
import android.util.Log
import com.dsk.myexpense.expense_module.ui.view.settings.SettingsRepository
import java.util.Locale

object CurrencyUtils {

    // Utility function to fetch and cache currency symbol using SharedPreferences
    suspend fun getCurrencySymbol(
        context: Context,
        settingsRepository: SettingsRepository
    ): String {
        CurrencyCache.getCurrencySymbol(context)?.let {
            return it
        }

        val defaultCurrency = settingsRepository.getDefaultCurrency()
        val currency = java.util.Currency.getInstance(defaultCurrency)
        val symbol = currency.getSymbol(Locale.getDefault(Locale.Category.DISPLAY))
        CurrencyCache.setCurrencySymbol(context, symbol)

        return symbol
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