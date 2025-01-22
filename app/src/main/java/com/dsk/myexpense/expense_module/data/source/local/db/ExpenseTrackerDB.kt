package com.dsk.myexpense.expense_module.data.source.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dsk.myexpense.expense_module.data.model.AccountEntity
import com.dsk.myexpense.expense_module.data.model.CardEntity
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.data.model.Currency
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.data.model.ExpenseInvoiceImage
import com.dsk.myexpense.expense_module.data.model.User
import com.dsk.myexpense.expense_module.util.AppConstants

@Database(entities = [ExpenseDetails::class, ExpenseInvoiceImage::class,
    Category::class, Currency::class, CardEntity::class, AccountEntity::class,User::class], exportSchema = false, version = 1)
abstract class ExpenseTrackerDB : RoomDatabase() {
    abstract fun getExpenseDAO(): ExpenseDAO
    abstract fun getExpenseTransactionDAO(): ExpenseTransactionDao
    abstract fun getExpenseCategoryDAO(): CategoryDao
    abstract fun getExpenseCurrencyDAO(): CurrencyDAO
    abstract fun cardDao(): CardDao
    abstract fun accountDao(): AccountDao
    abstract fun getUserDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: ExpenseTrackerDB? = null

        fun getDatabase(
            context: Context
        ): ExpenseTrackerDB {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {

                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ExpenseTrackerDB::class.java,
                    AppConstants.APP_LOCAL_DATABASE_NAME
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                // return instance
                instance
            }
        }

    }
}