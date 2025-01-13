package com.dsk.myexpense.expense_module.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nameOnCard: String,
    val cardNumber: String,
    val expiryDate: String,
    val cvc: String,
    val zip: String
)