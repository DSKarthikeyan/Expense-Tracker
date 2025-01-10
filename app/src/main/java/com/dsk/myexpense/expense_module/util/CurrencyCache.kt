package com.dsk.myexpense.expense_module.util

import android.content.Context
import android.util.Log

object CurrencyCache {
    private const val PREFS_NAME = "CurrencyPrefs"
    private const val KEY_CURRENCY_SYMBOL = "currency_symbol"

    fun getCurrencySymbol(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString(KEY_CURRENCY_SYMBOL, null)
    }

    fun setCurrencySymbol(context: Context, symbol: String) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        with(sharedPreferences.edit()) {
            putString(KEY_CURRENCY_SYMBOL, symbol)
            apply()
        }
    }

    fun getBaseCurrency(context: Context): String {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getString("base_currency", "USD") ?: "USD"
    }

    fun getExchangeRate(context: Context): Double {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPreferences.getFloat("exchange_rate", 0.0f).toDouble()
    }

    fun setBaseCurrency(context: Context, currency: String, exchangeRate: Double) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPreferences.edit()
            .putString("base_currency", currency)
            .putFloat("exchange_rate", exchangeRate.toFloat())
            .apply()
    }
}