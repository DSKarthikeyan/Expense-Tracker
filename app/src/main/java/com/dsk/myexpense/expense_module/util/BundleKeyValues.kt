package com.dsk.myexpense.expense_module.util

import androidx.annotation.StringDef

@Retention(AnnotationRetention.SOURCE)
@StringDef(
    BundleKeyValues.ADD_EXPENSE_KEY,
    BundleKeyValues.ADD_EXPENSE_DETAIL,
    BundleKeyValues.DELETE_EXPENSE_DETAIL,
    BundleKeyValues.NOTIFICATION_ID,
    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_CATEGORY_NAME,
    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_MESSAGE_SENDER,
    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_MESSAGE_DESCRIPTION,
    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT,
    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_TYPE,
    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_DATE,
    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT_RECEIVER_NAME,
    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT_SENDER_NAME,
    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_IS_INCOME,
    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_MESSAGE_DETAILS,
    BundleKeyValues.EXPENSE_DETAILS_KEY_SENDER_NAME,
    BundleKeyValues.EXPENSE_DETAILS_KEY_CATEGORY_ID,
    BundleKeyValues.EXPENSE_DETAILS_KEY_EXPENSE_ID,
    BundleKeyValues.EXPENSE_DETAILS_KEY_EXPENSE_ADDED_DATE
)

annotation class BundleKeyValues {
    companion object {
        const val ADD_EXPENSE_KEY = "AddExpense"
        const val ADD_EXPENSE_DETAIL = "ExpenseDetail"
        const val DELETE_EXPENSE_DETAIL = "DeleteExpenseDetail"
        const val NOTIFICATION_ID = "NotificationId"
        const val NOTIFICATION_KEY_EXPENSE_CATEGORY_NAME = "categoryName"
        const val NOTIFICATION_KEY_EXPENSE_MESSAGE_SENDER = "messageSender"
        const val NOTIFICATION_KEY_EXPENSE_MESSAGE_DESCRIPTION = "expenseDescription"
        const val NOTIFICATION_KEY_EXPENSE_AMOUNT = "amount"
        const val NOTIFICATION_KEY_EXPENSE_TYPE = "type"
        const val NOTIFICATION_KEY_EXPENSE_DATE = "date"
        const val NOTIFICATION_KEY_EXPENSE_AMOUNT_RECEIVER_NAME = "expenseReceiverName"
        const val NOTIFICATION_KEY_EXPENSE_AMOUNT_SENDER_NAME = "expenseMessageSenderName"
        const val NOTIFICATION_KEY_EXPENSE_IS_INCOME = "isIncome"
        const val NOTIFICATION_KEY_EXPENSE_MESSAGE_DETAILS = "messageDetails"

        const val EXPENSE_DETAILS_KEY_SENDER_NAME = "expenseSenderName"
        const val EXPENSE_DETAILS_KEY_CATEGORY_ID ="categoryId"
        const val EXPENSE_DETAILS_KEY_EXPENSE_ID = "expenseID"
        const val EXPENSE_DETAILS_KEY_EXPENSE_ADDED_DATE = "expenseAddedDate"

        const val EXPENSE_DETAILS_KEY_CATEGORY_FILE_ID = "id"
        const val EXPENSE_DETAILS_KEY_CATEGORY_FILE_NAME = "name"
        const val EXPENSE_DETAILS_KEY_CATEGORY_FILE_TYPE = "type"
        const val EXPENSE_DETAILS_KEY_CATEGORY_FILE_ICON_ID = "iconResId"

        const val EXPENSE_DETAILS_KEY_CURRENCY_FILE_ID = "id"
        const val EXPENSE_DETAILS_KEY_CURRENCY_FILE_CODE = "code"
        const val EXPENSE_DETAILS_KEY_CURRENCY_FILE_NAME = "name"
        const val EXPENSE_DETAILS_KEY_CURRENCY_FILE_SYMBOL = "symbol"
    }
}