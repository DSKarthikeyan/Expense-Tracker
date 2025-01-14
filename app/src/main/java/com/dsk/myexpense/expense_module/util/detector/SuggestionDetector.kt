package com.dsk.myexpense.expense_module.util.detector

import com.dsk.myexpense.expense_module.data.model.SMSMessage
import com.dsk.myexpense.expense_module.data.model.Suggestion

/**
 * Detects Suggestion by analysing the SMS Message
 */
abstract class SuggestionDetector {

    /**
     * Detect Suggestion by analyzing the SMS Message.
     */
    abstract fun detectSuggestions(smsMessage: SMSMessage): Suggestion?
}
