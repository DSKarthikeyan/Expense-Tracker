package com.dsk.myexpense.expense_module.data.source.local

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dsk.myexpense.expense_module.data.model.Currency

@Dao
interface CurrencyDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(currencies: List<Currency>)

    @Query("SELECT * FROM currencies")
    fun getAllCurrencies(): LiveData<List<Currency>>

    @Query("SELECT * FROM currencies")
    fun getAllCurrencyDetails(): List<Currency>

}