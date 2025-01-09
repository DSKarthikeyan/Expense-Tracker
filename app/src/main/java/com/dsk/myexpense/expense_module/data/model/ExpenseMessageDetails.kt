package com.dsk.myexpense.expense_module.data.model

data class ExpenseMessageDetails (
    var expenseMessageSender: String,
    val expenseType: String,
    val expenseAmount: Double,
    val receiverName: String,
    val senderName: String,
    var expenseDate: Long,
    val additionalDetails: String = "",
    var isIncome: Boolean?,
    var categoryName: String?
)