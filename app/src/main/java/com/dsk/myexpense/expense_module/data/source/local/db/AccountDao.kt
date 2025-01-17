package com.dsk.myexpense.expense_module.data.source.local.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.dsk.myexpense.expense_module.data.model.AccountEntity

@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: AccountEntity)

    @Query("SELECT * FROM accounts")
    fun getAccounts(): LiveData<List<AccountEntity>>

    @Query("UPDATE accounts SET isConnected = :status WHERE accountType = :type")
    suspend fun updateAccountStatus(type: String, status: Boolean)
}