package com.dsk.myexpense.expense_module.data.source.local.db

data class DailyExpenseWithTime(
    val day: Long?,
    val amount: Int?,
    val time: Long?,
    val isIncome: Boolean
)

data class WeeklyExpenseSum(
    val day: Long?,
    val sum: Int?,
    val isIncome: Boolean
)

data class WeeklyExpenseWithTime(
    val week: Long?,
    val amount: Int?,
    val time: Long?,
    val isIncome: Boolean
)

data class MonthlyExpenseWithTime(
    val month: Long?,
    val amount: Int?,
    val time: Long?,
    val isIncome: Boolean
)