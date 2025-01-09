package com.dsk.myexpense.expense_module.data.source.network

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyApi {

    @GET("latest.json")
    suspend fun getCurrencies(@Query("app_id") appId: String): Response<Map<String, Any>>

}