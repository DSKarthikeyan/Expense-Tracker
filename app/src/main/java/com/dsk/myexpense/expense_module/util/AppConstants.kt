package com.dsk.myexpense.expense_module.util

import java.util.regex.Pattern

class AppConstants {

    companion object {
        const val DATE_TIME_FORMAT_STRING = "EEE, d MMM yyyy hh:mm a"
        const val DATE_FORMAT_STRING = "EEE, d MMM yyyy"
        const val RAILWAY_TIME_FORMAT_STRING = "HH:mm a"
        const val APP_LINK_SHARE_FORMAT = "text/plain"
        const val APP_IMAGE_SELECTION_FORMAT = "image/*"
        const val APP_PDF_FORMAT = "application/pdf"
        const val EMPTY_STRING = ""
        const val APP_IMAGE_SELECTION_RESOURCE_PATH = "android.resource://"
        const val TEXT_RESOURCE_TYPE_STRING = "string"
        const val TEXT_RESOURCE_TYPE_DRAWABLE = "drawable"

        // Regex patterns for matching different components
        val regexForRecipientName: Pattern = Pattern.compile("UPI/P2M/\\d+/([A-Za-z\\s]+)")
        val regexForTransactionAmount: Pattern =
            Pattern.compile("[rR][sS]\\.?\\s*[,\\d]+\\.?\\d{0,2}|[iI][nN][rR]\\.?\\s*[,\\d]+\\.?\\d{0,2}")
        val regexForEMI: Pattern =
            Pattern.compile("Rs\\s*[\\d,.]+\\s+EMI\\s+for\\s+([A-Za-z\\s]+)\\s+Loan\\s+(XX\\d{4})\\s+due\\s+on\\s+(\\d{2}-[A-Za-z]{3}-\\d{2})")
        val regexForAccount: Pattern =
            Pattern.compile("A/C\\s*(x\\d{4,})|A/c\\s*no\\.?\\s*(XX\\d{4,})|Loan\\s+(XX\\d{4,})|Loan Account number\\s*(xx\\d+)")
        val regexForAmount: Pattern =
            Pattern.compile("[rR][sS]\\.?\\s*[,\\d]+\\.?\\d{0,2}|[iI][nN][rR]\\.?\\s*[,\\d]+\\.?\\d{0,2}")
        val regexForName: Pattern = Pattern.compile("Dear\\s+Mr/Ms\\s+([A-Za-z\\s]+)")
        val regexForToName: Pattern = Pattern.compile("(?<=To\\s)[A-Za-z\\s]+")
        val regexForSenderName: Pattern = Pattern.compile("(?<=From\\s)[A-Za-z\\s]+")
        val price: Pattern = Pattern.compile("\\d+(\\.\\d+)?")

        const val PREFS_NAME = "CurrencyPrefs"
        const val KEY_BASE_CURRENCY_SYMBOL = "currency_symbol"
        const val KEY_CURRENCY_VALUE_NAME = "USD"
        const val KEY_CURRENCY_VALUE_SYMBOL = "$"
        const val KEY_CURRENCY_VALUE = 1.0
        const val KEY_EXPENSE_FILE_FORMAT_TYPE_CSV = "CSV"
        const val KEY_EXPENSE_FILE_FORMAT_TYPE_JSON = "JSON"
        const val KEY_BASE_CURRENCY = "base_currency"
        const val KEY_EXCHANGE_RATE = "exchange_rate"

        const val ANDROID_APP_URL =
            "https://play.google.com/store/apps/details?id=com.dsk.myexpense"

        const val NOTIFICATION_ACTION_ADD = "com.dsk.ACTION_ADD"
        const val NOTIFICATION_ACTION_DENY = "com.dsk.ACTION_DENY"
        const val NOTIFICATION_ACTION_MAIN_ACTIVITY = "com.dsk.myexpense.expense_module.core.MainActivity"
        const val CURRENCY_LIST_APP_ID =
            "3821eb4bb00649c2b8c84dc75cc4bd2c" // https://openexchangerates.org/account/
        const val BASE_URL_CURRENCY_LIST = "https://openexchangerates.org/api/"

        const val APP_CSV_FILE_NAME_EXTENSION = ".csv"
        const val APP_JSON_FILE_NAME_EXTENSION = ".json"
        const val APP_PDF_FILE_NAME_EXTENSION = ".pdf"
        const val APP_CSV_EXPENSE_DETAILS_FILE_NAME = "expense_details"
        const val APP_CSV_CATEGORIES_DETAILS_FILE_NAME = "categories"
        const val APP_CSV_CURRENCIES_DETAILS_FILE_NAME = "currencies"
        const val APP_JSON_DATA_FILE_NAME = "data${APP_JSON_FILE_NAME_EXTENSION}"
        const val APP_LOCAL_DATABASE_NAME = "expense_tracker"

        const val APP_FILE_EXPENSES_KEY_NAME = "expenses"
        const val APP_FILE_CATEGORIES_KEY_NAME = "categories"
        const val APP_FILE_CURRENCIES_KEY_NAME = "currencies"

        const val USER_SHARED_PREF_NAME = "user_prefs"
        const val USER_SHARED_PREF_USER_DETAILS_NAME = "user_name"
        const val USER_SHARED_PREF_USER_PROFILE_PICTURE_NAME = "user_profile_picture"
    }
}