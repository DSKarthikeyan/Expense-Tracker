package com.dsk.myexpense.expense_module.util.detector

import android.app.Application
import android.util.Log
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.data.model.SMSMessage
import com.dsk.myexpense.expense_module.data.model.Suggestion
/**
 * Suggestion Detector with the help of Regexp Parsing.
 */
class SuggestionDetectorImpl(private val regexHelper: RegexHelper, private val application: Application) :
    SuggestionDetector() {

    /**
     * Check for smsMessage is of Transactional SMS and parse the Expense suggestion.
     */
    override fun detectSuggestions(smsMessage: SMSMessage): Suggestion? {
        val expenseType = regexHelper.getExpenseType(smsMessage.body, application)
        val isExpense = expenseType == application.resources.getString(
            R.string.text_expense)
        val spent = regexHelper.getAmountSpent(smsMessage.body)
        val paidToName = regexHelper.getPaidToName(smsMessage.body)

        if (spent != null) {
            return Suggestion(
                id = generateUniqueId(smsMessage),
                amount = spent,
                paidTo = paidToName,
                time = smsMessage.time,
                referenceMessage = smsMessage.body,
                referenceMessageSender = smsMessage.address,
                isExpense =  isExpense,
                expenseType = expenseType
            )
        }

        return null
    }

    private fun generateUniqueId(smsMessage: SMSMessage) =
        smsMessage.body.hashCode().toLong().plus(smsMessage.time)
}
