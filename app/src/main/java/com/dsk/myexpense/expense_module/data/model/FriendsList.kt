package com.dsk.myexpense.expense_module.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "friends_list")
data class FriendsList(
    @PrimaryKey(autoGenerate = false) val friendID: Int? = null,
    val friendName: String,
    val friendEmail: String,
    val friendPhone: Int
)
