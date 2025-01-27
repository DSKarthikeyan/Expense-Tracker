package com.dsk.myexpense.expense_module.util.detector

import android.app.Application
import com.dsk.myexpense.R
import java.util.regex.Pattern

/**
 * Helper class to identify and parse data based on Regexp.
 */
class RegexHelper {

    companion object {
        const val DEBIT_PATTERN = "debited|debit|deducted"
        const val MISC_PATTERN = "payment|spent|paying|sent|UPI"
    }

    /**
     * Check weather the message is of transaction type.
     *
     * @param message from SMS
     */
    fun isExpense(message: String): Boolean {
        val regex =
            "(?=.*[Aa]ccount.*|.*[Aa]/[Cc].*|.*[Aa][Cc][Cc][Tt].*|.*[Cc][Aa][Rr][Dd].*)(?=.*[Cc]redit.*|.*[Dd]ebit.*)(?=.*[Ii][Nn][Rr].*|.*[Rr][Ss].*)"
        return Pattern.compile(regex).matcher(message).find() || DEBIT_PATTERN.toRegex()
            .containsMatchIn(message.lowercase()) || MISC_PATTERN.toRegex()
            .containsMatchIn(message.lowercase())
    }

    /**
     * Get PaidTo/Merchant name from Transaction message.
     *
     * @param message from SMS
     */
    fun getPaidToName(message: String): String? {
//        val regex = "(?i)(?:\\sat\\s|in\\*)([A-Za-z0-9]*\\s?-?\\s?[A-Za-z0-9]*\\s?-?\\.?)"
//
//        return Regex(regex).find(message)?.groups?.get(0)?.value?.replace("at ", "")
//            ?.replace(" in", "")?.replace(" on", "")?.trim()
        val regex = Regex(
            """
        (?i)                                                # Case-insensitive matching
        (?:to\sname\sis\s|to\s|UPI/P2M/[^/]+/|at\s|in\*)    # Match prefixes indicating the name field
        ([A-Za-z][A-Za-z0-9\s.,'-]{2,})                    # Capture names (letters, numbers, spaces, punctuations)
        (?=\s(?:Not\s|SMS|Call|On|INR|A/c|Sent|$))         # Ensure the name ends before unintended text
        """.trimIndent(),
            RegexOption.COMMENTS
        )

        return regex.find(message)?.groups?.get(1)?.value?.trim()
    }

    /**
     * Get Amount spent on this transaction message.
     *
     * @param message from SMS
     */
    fun getAmountSpent(message: String): Double? {
        val regex = "(?i)(?:RS|INR|MRP)\\.?\\s?(\\d+(:?,\\d+)?(,\\d+)?(\\.\\d{1,2})?)"

        val matchGroup = Regex(regex).find(message)?.groups?.firstOrNull()
        return matchGroup?.value?.lowercase()
            ?.replace("inr.", "")?.replace("inr", "")
            ?.replace("mrp", "")
            ?.replace("rs.", "")?.replace("rs", "")
            ?.replace(",", "")?.trim()?.toDoubleOrNull()
    }

    /**
     *Get Card Name from transaction message
     */
    fun getCardName(message: String): String? {
        val regex = "[0-9]*[Xx*]*[0-9]*[Xx*]+[0-9]{3,}"

        return Regex(regex).find(message)?.groups?.get(0)?.value?.replace("inr", "")
            ?.replace("made on", "")?.replace("made a", "")?.trim()
    }

    fun getExpenseType(messageBody: String, application: Application): String {
        val depositRegex = Regex("deposited|credited|received", RegexOption.IGNORE_CASE)
        val withdrawalRegex = Regex("withdrawn|debited|spent|sent|overdue", RegexOption.IGNORE_CASE)

        return when {
            messageBody.contains(depositRegex) -> application.getString(R.string.text_income)
            messageBody.contains(withdrawalRegex) -> application.getString(R.string.text_expense)
            else -> application.getString(R.string.text_not_valid)
        }
    }
}
