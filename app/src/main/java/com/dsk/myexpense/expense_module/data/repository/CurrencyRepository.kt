package com.dsk.myexpense.expense_module.data.repository

import android.util.Log
import androidx.lifecycle.LiveData
import com.dsk.myexpense.expense_module.data.model.Currency
import com.dsk.myexpense.expense_module.data.source.local.db.CurrencyDAO
import com.dsk.myexpense.expense_module.data.source.network.CurrencyAPIService
import com.dsk.myexpense.expense_module.util.ApiResponse

class CurrencyRepository(
    private val currencyDAO: CurrencyDAO, private val currencyAPIService: CurrencyAPIService
) {
    suspend fun fetchCurrenciesFromAPI(
        apiKey: String, currencySymbolsFromJSON: Map<String, String>
    ): ApiResponse<List<Currency>> {
        return try {
            // Fetch the currencies from the API
            val currenciesFromAPI = currencyAPIService.getCurrencies(apiKey)
            Log.d("DsK", "Currencies currenciesFromAPI ${currenciesFromAPI.data?.size}")

            // Create a mutable map to store unique currencies based on name/code
            val uniqueCurrenciesMap = mutableSetOf<Currency>()

            if (currenciesFromAPI is ApiResponse.Success) {
                currenciesFromAPI.data?.let { currencies ->
                    currencies.forEach { currency ->
                        val currencyCode: String = currency.name // Treating the code as String
                        val currencyPrice: Double = currency.code // Treating the code as String

                        // Look up the symbol in the JSON map, or default to code if not found
                        val finalSymbol = currencySymbolsFromJSON[currencyCode]

                        // Fallback to code if the symbol is null or empty
                        val finalSymbolToUse =
                            finalSymbol?.takeIf { it.isNotEmpty() } ?: currencyCode

//                        Log.d("DsK", "Currencies name ${currency.name} code $currencyCode symbol $finalSymbolToUse")

                        // Create a Currency object
                        val currencyEntity = Currency(
                            code = currencyPrice, name = currency.name, symbol = finalSymbolToUse
                        )
                        // Only add if the currency is not already in the map
                        uniqueCurrenciesMap.add(currencyEntity)
                    }

                    // Insert all unique currencies into the database
                    currencyDAO.insertAll(uniqueCurrenciesMap.toList())

                    // Return the successful API response with data
                    ApiResponse.Success(currencies)
                } ?: ApiResponse.Error("No data available")
            } else {
                ApiResponse.Error(currenciesFromAPI.message ?: "Unknown error")
            }
        } catch (e: Exception) {
            ApiResponse.Error(e.localizedMessage ?: "Unknown error")
        }
    }


    // Save currencies to local database
    suspend fun insertAllCurrencies(currencies: List<Currency>) {
        currencyDAO.insertAll(currencies)
    }

    suspend fun insertCurrencies(currencies: Currency) {
        currencyDAO.insertCurrency(currencies)
    }

    // Get currencies from local database
    fun getAllCurrencyList(): List<Currency> {
        return currencyDAO.getAllCurrencyDetails()
    }

    fun getCurrenciesFromLocalLiveDB(): LiveData<List<Currency>> {
        return currencyDAO.getAllCurrencies()
    }
}