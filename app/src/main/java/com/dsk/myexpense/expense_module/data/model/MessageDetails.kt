package com.dsk.myexpense.expense_module.data.model
import java.io.Serializable

data class MessageDetails(
    val categoryName: String,
    val expenseMessageSender: String,
    val expenseAmount: String,
    val expenseType: String,
    val expenseDate: String,
    val receiverName: String,
    val senderName: String,
    val isIncome: Boolean
) : Serializable