package com.dsk.myexpense.expense_module.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    val name: String,
    val profilePicture: String // Store URI or file path
)