package com.dsk.myexpense.expense_module.data.source.network

import com.dsk.myexpense.expense_module.data.model.Currency
import com.dsk.myexpense.expense_module.util.ApiResponse
import com.dsk.myexpense.expense_module.util.AppConstants
import java.io.IOException

object CurrencyAPIService {

    private val apiService: CurrencyApi by lazy {
        RetrofitClient.createRetrofitService(AppConstants.BASE_URL_CURRENCY_LIST).create(CurrencyApi::class.java)
    }

    suspend fun getCurrencies(appId: String): ApiResponse<List<Currency>> {
        return try {
            val response = apiService.getCurrencies(appId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body["rates"] is Map<*, *>) {
                    ApiResponse.Success(
                        (body["rates"] as Map<String, Double>).map {
                            Currency(
                                name = it.key, code = it.value,
                                symbol =""
                            )
                        }
                    )
                } else {
                    ApiResponse.Error("Unexpected response format")
                }
            } else {
                ApiResponse.Error("Failed to fetch data: ${response.errorBody()?.string()}")
            }
        } catch (e: IOException) {
            ApiResponse.Error(e.localizedMessage ?: "Network error")
        } catch (e: Exception) {
            ApiResponse.Error(e.localizedMessage ?: "Unknown error")
        }
    }
}
