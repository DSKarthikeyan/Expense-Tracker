package com.dsk.myexpense.expense_module.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey val accountType: String,
    val isConnected: Boolean
)