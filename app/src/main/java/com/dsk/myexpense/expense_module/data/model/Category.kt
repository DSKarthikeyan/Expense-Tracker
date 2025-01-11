package com.dsk.myexpense.expense_module.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "category_table")
data class Category(
    @PrimaryKey(autoGenerate = true) var id: Int = 0,
    val name: String,
    val type: String, // "income" or "expense"
    val iconResId: Int // Resource ID for the category icon
)
