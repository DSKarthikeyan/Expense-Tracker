package com.dsk.myexpense.expense_module.data.model

data class SMSMessage(
    val address: String,
    val body: String,
    val time: Long
)