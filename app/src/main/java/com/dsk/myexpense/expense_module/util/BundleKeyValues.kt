package com.dsk.myexpense.expense_module.util

import androidx.annotation.StringDef

@Retention(AnnotationRetention.SOURCE)
@StringDef(
    BundleKeyValues.ADD_EXPENSE_KEY,
    BundleKeyValues.ADD_EXPENSE_DETAIL,
    BundleKeyValues.DELETE_EXPENSE_DETAIL,
    BundleKeyValues.NOTIFICATION_ID
)

annotation class BundleKeyValues {
    companion object{
        const val ADD_EXPENSE_KEY = "AddExpense"
        const val ADD_EXPENSE_DETAIL = "ExpenseDetail"
        const val DELETE_EXPENSE_DETAIL = "DeleteExpenseDetail"
        const val NOTIFICATION_ID = "NotificationId"
    }
}