package com.dsk.myexpense.expense_module.data.model

import android.os.Parcel
import android.os.Parcelable

data class ExpenseMessageDetails(
    val senderName: String? = null, // Nullable string
    val expenseMessageSender: String? = null,
    val receiverName: String? = null,
    val expenseAmount: Double = 0.0,
    val expenseDate: Long = 0L,
    val expenseType: String? = null,
    val categoryName: String? = null,
    val isIncome: Boolean? = null,
    val additionalDetails: String? = null // New field for any additional message details
) : Parcelable {

    constructor(parcel: Parcel) : this(
        senderName = parcel.readString(),
        expenseMessageSender = parcel.readString(),
        receiverName = parcel.readString(),
        expenseAmount = parcel.readDouble(),
        expenseDate = parcel.readLong(),
        expenseType = parcel.readString(),
        categoryName = parcel.readString(),
        isIncome = parcel.readValue(Boolean::class.java.classLoader) as? Boolean,
        additionalDetails = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(senderName)
        parcel.writeString(expenseMessageSender)
        parcel.writeString(receiverName)
        parcel.writeDouble(expenseAmount)
        parcel.writeLong(expenseDate)
        parcel.writeString(expenseType)
        parcel.writeString(categoryName)
        parcel.writeValue(isIncome)
        parcel.writeString(additionalDetails)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<ExpenseMessageDetails> {
        override fun createFromParcel(parcel: Parcel): ExpenseMessageDetails {
            return ExpenseMessageDetails(parcel)
        }

        override fun newArray(size: Int): Array<ExpenseMessageDetails?> {
            return arrayOfNulls(size)
        }
    }
}
