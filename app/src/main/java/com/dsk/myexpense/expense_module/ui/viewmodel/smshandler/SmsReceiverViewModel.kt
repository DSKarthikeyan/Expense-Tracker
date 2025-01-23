package com.dsk.myexpense.expense_module.ui.viewmodel.smshandler

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.data.repository.ExpenseRepository
import kotlinx.coroutines.launch

class SmsReceiverViewModel(private val expenseRepository: ExpenseRepository) : ViewModel() {

    /**
     * Save a transaction with optional invoice image.
     * @param senderName Sender of the expense
     * @param messageSenderName message Sender of the expense
     * @param description Description of the expense
     * @param amount Amount of the expense
     * @param date Date of the expense
     * @param categoryName Category name associated with the expense
     * @param invoiceImage Optional bitmap for the invoice
     * @param isIncome Flag indicating if the transaction is an income
     */
    fun saveTransaction(
        context: Context,
        senderName: String?,
        messageSenderName: String,
        receiverName: String,
        description: String,
        amount: Double,
        date: Long,
        categoryName: String,
        invoiceImage: Bitmap?,
        isIncome: Boolean
    ) {
        viewModelScope.launch {
            // Create ExpenseDetails object
            val expenseDetails = ExpenseDetails(
                expenseMessageSenderName = messageSenderName,
                expenseSenderName = senderName ?: "",
                expenseDescription = description,
                amount = amount,
                isIncome = isIncome,
                expenseReceiverName = receiverName,
                expenseAddedDate = date
            )
            // Save transaction with or without invoice
            expenseRepository.saveExpenseWithInvoice(
                    context = context,
                    expenseDetails = expenseDetails,
                    categoryName = categoryName,
                    bitmap = invoiceImage
            )
        }
    }
}
