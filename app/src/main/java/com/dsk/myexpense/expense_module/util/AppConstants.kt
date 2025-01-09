package com.dsk.myexpense.expense_module.util

import java.util.regex.Pattern

class AppConstants {

    companion object{
        const val DATE_FORMAT_STRING = "EEE, d MMM yyyy HH:mm a"

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

        const val ANDROID_APP_URL = "https://play.google.com/store/apps/details?id=com.dsk.myexpense"

        const val ACTION_ADD = "com.dsk.ACTION_ADD"
        const val ACTION_DENY = "com.dsk.ACTION_DENY"
        const val CURRENCY_LIST_APP_ID = "3821eb4bb00649c2b8c84dc75cc4bd2c" // https://openexchangerates.org/account/
    }
}