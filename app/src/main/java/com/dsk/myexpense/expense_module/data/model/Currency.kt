package com.dsk.myexpense.expense_module.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currencies")
data class Currency(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "code") val code: Double,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "symbol") val symbol: String?
)