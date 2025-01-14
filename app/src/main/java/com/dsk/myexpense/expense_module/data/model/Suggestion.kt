package com.dsk.myexpense.expense_module.data.model

data class Suggestion(
    val id: Long,
    val amount: Double,
    val paidTo: String?,
    val time: Long,
    val referenceMessage: String,
    val referenceMessageSender: String,
    val isExpense: Boolean,
    val expenseType: String
)
