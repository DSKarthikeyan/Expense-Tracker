package com.dsk.myexpense.expense_module.data.source.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.data.model.ExpenseInvoiceImage
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: ExpenseDetails): Long

    @Query("SELECT * FROM expense_details WHERE expenseID = :expenseID")
    suspend fun getExpenseById(expenseID: Int): ExpenseDetails

    @Delete
    suspend fun delete(todo: ExpenseDetails)

    @Query("SELECT * from expense_details order by expenseID ASC")
    fun getAllExpenseDetails(): LiveData<List<ExpenseDetails>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expense_details WHERE isIncome = 1")
    fun getTotalIncome(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expense_details WHERE isIncome != 1")
    fun getTotalExpense(): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expense_details")
    fun getTotalIncomeExpense(): Flow<Double>

    @Insert
    suspend fun insertInvoiceImage(invoiceImage: ExpenseInvoiceImage)

    @Query("SELECT * FROM invoice_images WHERE expenseID = :expenseID")
    suspend fun getInvoiceImagesForExpense(expenseID: Int): List<ExpenseInvoiceImage>

    @Update
    suspend fun updateExpense(expenseDetails: ExpenseDetails)

    @Transaction
    suspend fun updateExpenseWithInvoice(
        expenseDetails: ExpenseDetails,
        expenseInvoiceImage: ExpenseInvoiceImage
    ) {
        val existingExpense = expenseDetails.expenseID?.let { getExpenseById(it) }
        val expenseDescription = expenseDetails.expenseDescription
        val updatedExpense = existingExpense?.copy(
            expenseSenderName = expenseDetails.expenseSenderName,
            expenseReceiverName = expenseDetails.expenseReceiverName,
            amount = expenseDetails.amount,
            isIncome = expenseDetails.isIncome,
            expenseDescription = expenseDescription,
            expenseAddedDate = expenseDetails.expenseAddedDate
        )
        if (updatedExpense != null) {
            updateExpense(updatedExpense)
            insertOrUpdateInvoiceImage(expenseInvoiceImage)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateInvoiceImage(expenseInvoiceImage: ExpenseInvoiceImage)

    @Query("""
    SELECT 
        date / 1000 * 1000 AS day,  -- Return the day in milliseconds
        amount AS amount,  -- The expense amount
        date AS time  -- Exact timestamp of the expense
    FROM expense_details
    WHERE strftime('%Y-%m-%d', date / 1000, 'unixepoch') = strftime('%Y-%m-%d', 'now')  -- Filter for current day
    ORDER BY amount DESC  -- Order by highest amount
    LIMIT 10  -- Limit to top 10
""")
    fun getDailyExpenseSum(): List<DailyExpenseWithTime>

    @Query("""
    SELECT 
        date AS day, 
        amount AS sum
    FROM expense_details 
    ORDER BY amount DESC 
""")
    fun getWeeklyExpenseSum(): List<WeeklyExpenseSum>

    @Query("""
    SELECT 
        (strftime('%Y-%m', date / 1000, 'unixepoch') || '-01') AS firstDayOfMonth,  -- First day of the current month
        CAST((strftime('%d', date / 1000, 'unixepoch') - 1) / 7 + 1 AS INTEGER) AS weekOfMonth,  -- Calculate the week of the month
        MIN(date) AS day,  -- Earliest date within each week as representative day
        SUM(amount) AS sum
    FROM expense_details
    WHERE strftime('%Y-%m', date / 1000, 'unixepoch') = strftime('%Y-%m', 'now')  -- Only current month's data
    GROUP BY weekOfMonth
    ORDER BY weekOfMonth
""")
    fun getMonthlyExpenseSum(): List<WeeklyExpenseSum>

    @Query("""
    SELECT 
        strftime('%Y-%m', date / 1000, 'unixepoch') AS month,  -- Year-Month as identifier
        SUM(amount) AS amount,  -- Sum of amounts for the month
        MIN(date) AS time  -- First date in the month as representative time
    FROM expense_details
    WHERE strftime('%Y', date / 1000, 'unixepoch') = strftime('%Y', 'now')  -- Only current year data
    GROUP BY month
    ORDER BY month
""")
    fun getYearlyExpenseSum(): List<MonthlyExpenseWithTime>

}