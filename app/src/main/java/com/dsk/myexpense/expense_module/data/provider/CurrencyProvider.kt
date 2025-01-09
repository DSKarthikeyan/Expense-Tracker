package com.dsk.myexpense.expense_module.data.provider//package com.dsk.myexpense.expense_module.data
//
//import android.content.Context
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.withContext
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.json.Json
//
//@kotlinx.serialization.Serializable
//data class Currency(
//    val symbol: String,
//    val name: String,
//    val symbol_native: String,
//    val decimal_digits: Int,
//    val rounding: Int,
//    val code: String,
//    val name_plural: String,
//    val type: String,
//)
//
//@Serializable
//private data class CurrencyWrapper(val data: Map<String, Currency>)
//
//class CurrencyProvider(private val context: Context)  {
//
//    suspend fun retrieveCurrencies(): List<Currency> =
//        withContext(Dispatchers.IO) {
//            val currenciesFile = context.assets.open("currencies.json").bufferedReader().use { it.readText() }
//            return@withContext Json
//                .decodeFromString<CurrencyWrapper>(currenciesFile)
//                .data.values
//                .toList()
//            }
//}
