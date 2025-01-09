package com.dsk.myexpense.expense_module.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "invoice_images",
    foreignKeys = [
        ForeignKey(
            entity = ExpenseDetails::class,
            parentColumns = ["expenseID"],
            childColumns = ["expenseID"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ExpenseInvoiceImage(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(index = true) // Index for faster querying
    val expenseID: Int, // Foreign key referencing ExpenseDetails.expenseID

    val expenseInvoiceImage: ByteArray?, // Store the image as a byte array

    val expenseImageFilePath: String // Alternative: file path for the image
)
