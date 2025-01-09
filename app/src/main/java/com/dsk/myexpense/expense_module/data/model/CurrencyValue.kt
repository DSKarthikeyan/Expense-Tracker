//package com.dsk.myexpense.expense_module.data.model
//
//import toCurrencyString
//
//data class CurrencyValue(val value: Float, val currencyCode: String, val isExchanged: Boolean = false) {
//    fun toCurrencyString(): String {
//        val prefix = if (isExchanged) "~" else ""
//        return prefix + value.toCurrencyString(currencyCode)
//    }
//}
