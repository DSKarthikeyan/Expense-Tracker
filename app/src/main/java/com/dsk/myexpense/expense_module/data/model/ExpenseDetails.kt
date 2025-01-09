package com.dsk.myexpense.expense_module.data.model

import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(
    tableName = "expense_details",
    foreignKeys = [ForeignKey(
        entity = Category::class,
        parentColumns = ["id"],
        childColumns = ["categoryId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["categoryId"])]
)
data class ExpenseDetails(
    val expenseSenderName: String,
    val expenseMessageSenderName: String,
    val expenseReceiverName: String,
    val expenseDescription: String,
    val amount: Double,
    val isIncome: Boolean,
    val categoryId: Int? = null, // Foreign key to Category table
    @PrimaryKey(autoGenerate = false) val expenseID: Int? = null,
    @ColumnInfo(name = "date") val expenseAddedDate: Long
) : Parcelable
