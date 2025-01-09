package com.dsk.myexpense.expense_module.data.source.local

data class DailyExpenseWithTime(
    val day: Long?,
    val amount: Int?,
    val time: Long?
)

data class WeeklyExpenseSum(
    val day: Long?,
    val sum: Int?
)

data class WeeklyExpenseWithTime(
    val week: Long?,
    val amount: Int?,
    val time: Long?
)

data class MonthlyExpenseWithTime(
    val month: Long?,
    val amount: Int?,
    val time: Long?
)