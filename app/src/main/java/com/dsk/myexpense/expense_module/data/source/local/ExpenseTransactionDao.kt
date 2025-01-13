package com.dsk.myexpense.expense_module.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Transaction
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.data.model.ExpenseInvoiceImage

@Dao
interface ExpenseTransactionDao {
    @Transaction
    suspend fun insertExpenseWithInvoice(
        expenseDetails: ExpenseDetails,
        invoiceImage: ExpenseInvoiceImage
    ) {
        // Insert ExpenseDetails
        val expenseID = insert(expenseDetails).toInt() // Fetch the inserted ID

        // Prepare Invoice Image with the returned ID
        val updatedInvoiceImage = invoiceImage.copy(expenseID = expenseID)

        // Insert Invoice Image
        insertInvoiceImage(updatedInvoiceImage)
    }

    @Insert
    suspend fun insert(expenseDetails: ExpenseDetails): Long

    @Insert
    suspend fun insertInvoiceImage(invoiceImage: ExpenseInvoiceImage)

}