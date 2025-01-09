package com.dsk.myexpense.expense_module.ui.viewmodel.smshandler

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.data.repository.ExpenseRepository
import com.dsk.myexpense.expense_module.data.source.local.ExpenseTrackerDB
import com.dsk.myexpense.expense_module.data.source.network.CurrencyAPIService
import kotlinx.coroutines.launch

class SmsReceiverViewModel(application: Application) : AndroidViewModel(application) {

    // Dependency injection through constructor
    private val repository: ExpenseRepository = ExpenseRepository(
        expenseDAO = ExpenseTrackerDB.getDatabase(application).getExpenseDAO(),
        transactionDao = ExpenseTrackerDB.getDatabase(application).getExpenseTransactionDAO(),
        categoryDao = ExpenseTrackerDB.getDatabase(application).getExpenseCategoryDAO(),
        currencyDao = ExpenseTrackerDB.getDatabase(application).getExpenseCurrencyDAO(),
        currencyAPIService = CurrencyAPIService
    )

    // LiveData to observe all transactions
    val allTransactions: LiveData<List<ExpenseDetails>> = repository.allExpenseDetails

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
            repository.saveExpenseWithInvoice(
                expenseDetails = expenseDetails,
                categoryName = categoryName,
                bitmap = invoiceImage
            )
        }
    }
}
