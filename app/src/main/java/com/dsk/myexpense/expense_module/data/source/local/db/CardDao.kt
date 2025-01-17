package com.dsk.myexpense.expense_module.data.source.local.db

import androidx.lifecycle.LiveData
import androidx.room.*
import com.dsk.myexpense.expense_module.data.model.CardEntity

@Dao
interface CardDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity)

    @Query("SELECT * FROM cards")
    fun getCards(): LiveData<List<CardEntity>>

    @Query("SELECT * FROM cards")
    fun getAllCards(): List<CardEntity>
}