package com.dsk.myexpense.expense_module.data.source.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dsk.myexpense.expense_module.data.model.User

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM user LIMIT 1")
    suspend fun getUser(): User?
}