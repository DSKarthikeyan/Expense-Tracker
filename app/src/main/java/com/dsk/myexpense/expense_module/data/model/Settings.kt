package com.dsk.myexpense.expense_module.data.model

data class Setting(
    val id: Int,
    val name: String,
    val type: SettingType,
    val value: String
)

enum class SettingType {
    DARK_MODE,
    DEFAULT_CURRENCY,
    DEVELOPER_INFO
}
