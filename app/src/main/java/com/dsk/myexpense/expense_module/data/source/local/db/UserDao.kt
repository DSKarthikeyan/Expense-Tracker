package com.dsk.myexpense.expense_module.data.source.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.dsk.myexpense.expense_module.data.model.User

@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User)

    @Query("DELETE FROM user")
    suspend fun deleteAllUser()

    @Query("SELECT * FROM user LIMIT 1")
    suspend fun getUser(): User
}