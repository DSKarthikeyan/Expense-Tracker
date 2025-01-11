package com.dsk.myexpense.expense_module.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currencies")
data class Currency(
    @PrimaryKey(autoGenerate = true) val id: Int? = null,
    @ColumnInfo(name = "code") val code: Double,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "symbol") val symbol: String? = null
){
    // Overriding equals and hashCode to ensure correct comparison of Currency objects
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Currency) return false
        return code == other.code && name == other.name // Or use 'code' alone to prevent duplicate codes
    }

    override fun hashCode(): Int {
        return code.hashCode() // Or use a combination of 'code' and 'name' if you want to include both
    }
}