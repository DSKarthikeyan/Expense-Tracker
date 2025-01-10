package com.dsk.myexpense.expense_module.util

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.data.model.ExpenseMessageDetails
import com.dsk.myexpense.expense_module.ui.viewmodel.smshandler.SmsReceiverViewModel
import java.util.regex.Pattern


class SmsReceiver : BroadcastReceiver() {

    companion object {
        private val processedMessages = HashSet<String>()
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (smsMessage in messages) {
                val uniqueKey = smsMessage.displayOriginatingAddress + smsMessage.timestampMillis
                if (processedMessages.contains(uniqueKey)) {
                    Log.d("SmsReceiver", "Duplicate SMS received, ignoring.")
                    return
                }
                processedMessages.add(uniqueKey)

                val sender = smsMessage.displayOriginatingAddress
                val messageBody = smsMessage.messageBody
                val dateString = smsMessage.timestampMillis

                val application = context.applicationContext as Application
                val messageDetails = extractDetails(messageBody, application)
                Log.d("DsK", "onReceive messageDetails $messageDetails")
                if (messageDetails.expenseType != "Not Valid") {
                    messageDetails.apply {
                        expenseMessageSender = sender
                        isIncome = expenseType == application.getString(R.string.text_income)
                        categoryName = determineCategory(messageBody, messageDetails.receiverName)
                        expenseDate = dateString
                    }
                    if (Utility.isAppInForeground(application)) {
                        val activityContext = Utility.getForegroundActivity(application)
                        if (activityContext != null) {
                            showTransactionDialog(
                                activityContext, messageDetails, messageBody, dateString
                            )
                        } else {
                            Log.e("SmsReceiver", "No active activity context found")
                        }
                    } else {
                        NotificationUtils.showNotification(
                            application, messageDetails, messageBody
                        )
                    }
                }
            }
        }
    }

    private fun showTransactionDialog(
        context: Context,
        messageDetails: ExpenseMessageDetails,
        description: String,
        date: Long,
    ) {
        if (context !is Activity || context.isFinishing) {
            Log.e(
                "SmsReceiver",
                "Cannot show dialog as context is not an activity or activity is finishing"
            )
            return
        }

        Log.d(
            "DsK",
            "showTransactionDialog Sender: ${messageDetails.senderName}, Receiver: ${messageDetails.receiverName}, Amount: ${messageDetails.expenseAmount}, Date: ${messageDetails.expenseDate}, isIncome: ${messageDetails.isIncome}"
        )
        val builder = AlertDialog.Builder(context)
        builder.setTitle("New Transaction")
            .setMessage("Sender: ${messageDetails.senderName}\nReceiver: ${messageDetails.receiverName}\nDescription: $description\nAmount: ${messageDetails.expenseAmount}\nDate: $date\nType: ${if (messageDetails.isIncome!!) "Income" else "Expense"}")
            .setPositiveButton("Add") { _, _ ->

                val application = context.applicationContext as Application
                val isIncome = messageDetails.isIncome ?: false
                val categoryNameValue = messageDetails.categoryName ?: ""
                val viewModel = SmsReceiverViewModel(application)
                viewModel.saveTransaction(
                    context,
                    messageDetails.senderName,
                    messageDetails.expenseMessageSender,
                    messageDetails.receiverName,
                    description,
                    messageDetails.expenseAmount,
                    date,
                    categoryName = categoryNameValue,
                    isIncome = isIncome,
                    invoiceImage = null,
                )
            }.setNegativeButton("Deny") { dialog, _ -> dialog.dismiss() }.show()
    }

    private fun extractDetails(body: String, application: Application): ExpenseMessageDetails {
        return when {
            body.contains("EMI", ignoreCase = true) -> extractEmiDetails(body, application)
            body.contains("A/C", ignoreCase = true) || body.contains(
                "UPI", ignoreCase = true
            ) -> extractTransactionDetails(body, application)

            body.contains("spent", ignoreCase = true) -> extractCardSpendingDetails(
                body, application
            )

            else -> ExpenseMessageDetails(
                "",
                "Not Valid",
                0.0,
                "Unknown",
                "Unknown",
                additionalDetails = "No specific details found.",
                expenseDate = 0,
                isIncome = false,
                categoryName = ""
            )
        }
    }

    private fun extractEmiDetails(body: String, application: Application): ExpenseMessageDetails {
        val amountRegex = Regex("Rs\\.?\\s?(\\d+[.,]?\\d*)")
        val bankRegex = Regex("(\\w+\\sBank)")
        val accountRegex = Regex("Loan\\s(?:Account|A/c|Acc)\\s(?:no|XX)?(\\d+)")
        val dueDateRegex = Regex("(\\d{2}-\\w{3}-\\d{2,4})")

        // Extract amount
        val amountMatch = amountRegex.find(body)
        val transactionAmount =
            amountMatch?.groups?.get(1)?.value?.replace(",", "")?.toDoubleOrNull() ?: 0.0

        // Extract bank name
        val bankMatch = bankRegex.find(body)
        val bankName = bankMatch?.groups?.get(1)?.value ?: "Unknown Bank"

        // Extract loan account
        val accountMatch = accountRegex.find(body)
        val loanAccount = accountMatch?.groups?.get(1)?.value ?: "Not Found"

        // Extract due date
        val dueDateMatch = dueDateRegex.find(body)
        val dueDate = dueDateMatch?.groups?.get(1)?.value ?: "Not Found"

        Log.d(
            "extractEmiDetails",
            "Amount: $transactionAmount, Bank: $bankName, Loan Account: $loanAccount, Due Date: $dueDate"
        )

        return ExpenseMessageDetails(
            "",
            expenseType = application.getString(R.string.text_expense),
            expenseAmount = transactionAmount,
            receiverName = loanAccount,
            senderName = bankName,
            additionalDetails = "Loan Account: $loanAccount, Due Date: $dueDate",
            expenseDate = 0, // This can be parsed into timestamp if needed
            isIncome = false,
            categoryName = "Loan"
        )
    }

    private fun extractTransactionDetails(
        body: String, application: Application
    ): ExpenseMessageDetails {
        var transactionAmount = 0.0
        var accountNumber = "Not Found"
        var senderName = "Unknown"
        var recipientName = "Not Found"
        var transactionDate = "Unknown"

        // Extract transaction amount
        val amountRegex = Regex("Rs\\.?\\s?(\\d+[.,]?\\d*)|INR\\s?(\\d+[.,]?\\d*)")
        val amountMatch = amountRegex.find(body)
        if (amountMatch != null) {
            transactionAmount = amountMatch.groups[1]?.value?.replace(",", "")?.toDoubleOrNull()
                ?: amountMatch.groups[2]?.value?.replace(",", "")?.toDoubleOrNull() ?: 0.0
        }

        // Extract account number
        val accountRegex =
            Regex("A/C\\s*(x?\\d+)|A/c\\s*no\\.\\s*(XX?\\d+)", RegexOption.IGNORE_CASE)
        val accountMatch = accountRegex.find(body)
        if (accountMatch != null) {
            accountNumber =
                accountMatch.groups[1]?.value ?: accountMatch.groups[2]?.value ?: "Not Found"
        }

        // Extract sender name
        val senderRegex =
            Regex("From\\s(.*?)(?=\\sA/C)|on\\s(\\w+\\sBank)", RegexOption.IGNORE_CASE)
        val senderMatch = senderRegex.find(body)
        if (senderMatch != null) {
            senderName = senderMatch.groups[1]?.value ?: senderMatch.groups[2]?.value ?: "Unknown"
        }

        // Extract recipient name
        val recipientRegex =
            Regex("To\\s(.*?)(?=\\sOn)|UPI/P2M/\\d+/([A-Za-z]+)", RegexOption.IGNORE_CASE)
        val recipientMatch = recipientRegex.find(body)
        if (recipientMatch != null) {
            recipientName =
                recipientMatch.groups[1]?.value ?: recipientMatch.groups[2]?.value ?: "Not Found"
        }

        // Extract transaction date
        val dateRegex =
            Regex("(\\d{2}-\\d{2}-\\d{2}),?\\s?(\\d{2}:\\d{2}:\\d{2})?|On\\s(\\d{2}/\\d{2}/\\d{2})")
        val dateMatch = dateRegex.find(body)
        if (dateMatch != null) {
            transactionDate = dateMatch.groups[1]?.value ?: dateMatch.groups[2]?.value
                    ?: dateMatch.groups[3]?.value ?: "Unknown"
        }

        return ExpenseMessageDetails(
            "",
            expenseType = "Expense", // Assuming all these are expenses
            expenseAmount = transactionAmount,
            receiverName = recipientName,
            senderName = senderName,
            additionalDetails = "Account Number: $accountNumber",
            expenseDate = 0, // Placeholder for actual date processing
            isIncome = false,
            categoryName = "General"
        )
    }


    private fun extractCardSpendingDetails(
        body: String, application: Application
    ): ExpenseMessageDetails {
        val amountRegex = Regex("Rs\\.?\\s*\\d+[.,]?\\d*")
        val locationRegex = Regex("at\\s*[^\\n]+")
        val senderRegex = Regex("on\\s*[^\\n]+")

        val amount =
            amountRegex.find(body)?.value?.replace("Rs.", "")?.trim()?.toDoubleOrNull() ?: 0.0
        val location = locationRegex.find(body)?.value?.replace("at", "")?.trim() ?: "Unknown"
        val sender = senderRegex.find(body)?.value?.replace("on", "")?.trim() ?: "Unknown"

        return ExpenseMessageDetails(
            "",
            expenseType = application.getString(R.string.text_expense),
            expenseAmount = amount,
            receiverName = location,
            senderName = sender,
            additionalDetails = "Location: $location",
            expenseDate = 0,
            isIncome = false,
            categoryName = sender
        )
    }

    private fun getExpenseType(messageBody: String, application: Application): String {
        val depositRegex = Regex("deposited|credited|received", RegexOption.IGNORE_CASE)
        val withdrawalRegex = Regex("withdrawn|debited|spent|sent|overdue", RegexOption.IGNORE_CASE)

        return when {
            messageBody.contains(depositRegex) -> application.getString(R.string.text_income)
            messageBody.contains(withdrawalRegex) -> application.getString(R.string.text_expense)
            else -> application.getString(R.string.text_not_valid)
        }
    }

    private fun determineCategory(body: String, recipientName: String): String {
        val fuelKeywords = listOf(
            "fuel", "petrol", "diesel", "gas", "hpcl", "bpcl", "indian oil", "shell", "crude"
        )
        val utilitiesKeywords = listOf(
            "electricity", "water", "bill", "utility", "power", "gas", "energy", "meter", "upi/p2m"
        )
        val groceriesKeywords =
            listOf("grocery", "supermarket", "mart", "store", "market", "bazaar", "provision")
        val entertainmentKeywords = listOf(
            "movie",
            "cinema",
            "entertainment",
            "theatre",
            "netflix",
            "prime",
            "disney",
            "concert",
            "show"
        )
        val travelKeywords = listOf(
            "uber",
            "ola",
            "taxi",
            "cab",
            "flight",
            "train",
            "bus",
            "ticket",
            "airline",
            "travel",
            "tour",
            "trip",
            "booking"
        )
        val foodKeywords = listOf(
            "restaurant",
            "food",
            "cafe",
            "dining",
            "eating",
            "bar",
            "hotel",
            "bakery",
            "fast food",
            "snack",
            "meals"
        )
        val shoppingKeywords = listOf(
            "shopping",
            "clothing",
            "apparel",
            "fashion",
            "shoes",
            "electronics",
            "jewelry",
            "mall",
            "boutique",
            "accessories"
        )
        val medicalKeywords = listOf(
            "hospital",
            "pharmacy",
            "medical",
            "clinic",
            "doctor",
            "medicines",
            "health",
            "lab",
            "test"
        )
        val insuranceKeywords =
            listOf("insurance", "premium", "policy", "life cover", "health cover", "accident cover")
        val emiKeywords = listOf("emi", "loan", "repayment", "installment", "due", "overdue")
        val educationKeywords = listOf(
            "school", "college", "university", "course", "tuition", "exam", "education", "coaching"
        )
        val othersKeywords = listOf("miscellaneous", "general", "other")

        // Convert to lower case for comparison
        val lowerBody = body.lowercase()
        val lowerRecipient = recipientName.lowercase()

        return when {
            medicalKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> "Medical"
            fuelKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> "Fuel"
            othersKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> "Others"
            utilitiesKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> "Utilities"
            groceriesKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> "Groceries"
            entertainmentKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> "Entertainment"
            travelKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> "Travel"
            foodKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> "Food & Dining"
            shoppingKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> "Shopping"
            insuranceKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> "Insurance"
            emiKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> "EMI & Loans"
            educationKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> "Education"
            else -> "Others"
        }
    }


}


//class SmsReceiver : BroadcastReceiver() {
//
//    companion object {
//        private val processedMessages = HashSet<String>()
//    }
//
//    override fun onReceive(context: Context, intent: Intent) {
//        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
//            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
//            for (smsMessage in messages) {
//                val uniqueKey = smsMessage.displayOriginatingAddress + smsMessage.timestampMillis
//                if (processedMessages.contains(uniqueKey)) {
//                    Log.d("DsK", "Duplicate SMS received, ignoring.")
//                    return
//                }
//                processedMessages.add(uniqueKey)
//
//                val sender = smsMessage.displayOriginatingAddress
//                val messageBody = smsMessage.messageBody
//                val dateString = smsMessage.timestampMillis
//
//                val (type, amount, toName, senderName) = getMessageDetails(messageBody)
//                Log.d("DsK", "Message getMessageDetails: ${getMessageDetails(messageBody)}")
//                Log.d("DsK", "Message extractDetails: ${extractDetails(messageBody)}")
//                Log.d("DsK", "Message type: $type")
//                if (type != "Not Valid") {
//
//                    val application = context.applicationContext as Application
//
//                    if (Utility.isAppInForeground(application)) {
//                        val activityContext = Utility.getForegroundActivity(application)
//                        if (activityContext != null) {
//                            showTransactionDialog(
//                                activityContext,
//                                sender,
//                                messageBody,
//                                type == context.getString(R.string.text_income),
//                                amount,
//                                dateString,
//                                toName,
//                                senderName,
//                                categoryName = "Other Expenses"
//                            )
//                        } else {
//                            Log.e("SmsReceiver", "No active activity context found")
//                        }
//                    } else {
//                        NotificationUtils.showNotification(
//                            application, sender, messageBody, type, amount, dateString, toName, senderName,
//                            type == context.getString(R.string.text_income)
//                        )
//                    }
//                }
//            }
//        }
//    }
//
//    private fun showTransactionDialog(
//        context: Context,
//        sender: String?,
//        description: String,
//        isIncome: Boolean,
//        amount: Double,
//        date: Long,
//        toName: String,
//        senderName: String,
//        categoryName: String
//    ) {
//        if (context !is Activity || context.isFinishing) {
//            Log.e(
//                "SmsReceiver",
//                "Cannot show dialog as context is not an activity or activity is finishing"
//            )
//            return
//        }
//
//        val builder = AlertDialog.Builder(context)
//        builder.setTitle("New Transaction")
//            .setMessage("Sender: $sender\nFrom: $senderName\nTo: $toName\nDescription: $description\nAmount: $amount\nType: \nDate: $date")
//            .setPositiveButton("Add") { _, _ ->
//                val application = context.applicationContext as Application
//
//                val viewModel = SmsReceiverViewModel(application)
//                viewModel.saveTransaction(
//                    sender,
//                    description,
//                    amount,
//                    date,
//                    categoryName = "Other Expenses",
//                    null,
//                    isIncome
//                )
//            }.setNegativeButton("Deny") { dialog, _ -> dialog.dismiss() }.show()
//    }
//
//    private fun extractDate(timestampMillis: String): String {
//        val timestamp = timestampMillis.toLong()
//        val date = Date(timestamp)
//        val dateFormat = SimpleDateFormat(AppConstants.DATE_FORMAT_STRING, Locale.getDefault())
//        return dateFormat.format(date)
//    }
//
//    /**
//     * Determines which message type the body belongs to and extracts relevant details.
//     */
//    private fun getMessageDetails(body: String): ExpenseMessageDetails {
//        val nameMatcher = AppConstants.regexForName.matcher(body)
//        val extractedName =
//            if (nameMatcher.find()) nameMatcher.group(1)?.trim() ?: "Unknown" else "Unknown"
//
//        val accountMatcher = AppConstants.regexForAccount.matcher(body)
//        if (!accountMatcher.find()) {
//            return ExpenseMessageDetails(
//                "Not Valid", 0.0, "Unknown", "Unknown", additionalDetails = "Name: $extractedName"
//            )
//        }
//
//        val amountMatcher = AppConstants.regexForAmount.matcher(body)
//        if (!amountMatcher.find()) {
//            return ExpenseMessageDetails(
//                "Not Valid", 0.0, "Unknown", "Unknown", additionalDetails = "Name: $extractedName"
//            )
//        }
//
//        val amount = amountMatcher.group(0)
//        val toNameMatcher = AppConstants.regexForToName.matcher(body)
//        val senderNameMatcher = AppConstants.regexForSenderName.matcher(body)
//
//        val toName = if (toNameMatcher.find()) toNameMatcher.group(0)?.trim() else "Unknown"
//        val senderName =
//            if (senderNameMatcher.find()) senderNameMatcher.group(0)?.trim() else "Unknown"
//
//        // Determine transaction type
//        val depositRegex = Regex("deposited|credited", RegexOption.IGNORE_CASE)
//        val withdrawalRegex = Regex("withdrawn|debited|spent|sent|overdue", RegexOption.IGNORE_CASE)
//
//        val type = when {
//            body.contains(depositRegex) -> "Income"
//            body.contains(withdrawalRegex) -> "Expense"
//            else -> "Not Valid"
//        }
//
//        // Parse amount value
//        val priceMatcher = AppConstants.price.matcher(amount?.replace(",", "") ?: "")
//        val amountValue = if (priceMatcher.find()) {
//            priceMatcher.group(0)?.toDoubleOrNull() ?: 0.0
//        } else {
//            0.0
//        }
//
//        Log.d("DsK", " $type, $amountValue, $toName, $senderName, $extractedName")
//        return ExpenseMessageDetails(
//            type,
//            amountValue,
//            toName!!,
//            senderName!!,
//            additionalDetails = "Name: $extractedName"
//        )
//    }
//
//    /**
//     * Extracts EMI details from the SMS body.
//     */
//    private fun extractEmiDetails(body: String): String {
//        val emiDetails = mutableMapOf<String, String>()
//
//        val emiMatcher = AppConstants.regexForEMI.matcher(body)
//        if (emiMatcher.find()) {
//            emiDetails["BankName"] = emiMatcher.group(1)?.trim() ?: "Not Found"
//            emiDetails["LoanAccount"] = emiMatcher.group(2)?.trim() ?: "Not Found"
//            emiDetails["DueDate"] = emiMatcher.group(3)?.trim() ?: "Not Found"
//        }
//
//        Log.d(
//            "DsK",
//            "Type: EMI Notification\n" + "Bank Name: ${emiDetails["BankName"]}\n" + "Loan Account: ${emiDetails["LoanAccount"]}\n" + "Due Date: ${emiDetails["DueDate"]}"
//        )
//        return if (emiDetails.isNotEmpty()) {
//            """
//            Type: EMI Notification
//            Bank Name: ${emiDetails["BankName"]}
//            Loan Account: ${emiDetails["LoanAccount"]}
//            Due Date: ${emiDetails["DueDate"]}
//        """.trimIndent()
//        } else {
//            "No EMI details found."
//        }
//    }
//
//    /**
//     * Extracts transaction details from the SMS body.
//     */
//    private fun extractTransactionDetails(body: String): String {
//        var accountNumber = "Not Found"
//        var recipientName = "Not Found"
//        var toName = "Not Found"
//        var senderName = "Not Found"
//        var transactionAmount = "Not Found"
//
//        // Match account number
//        val accountMatcher = AppConstants.regexForAccount.matcher(body)
//        if (accountMatcher.find()) {
//            accountNumber = accountMatcher.group(1)?.trim() ?: accountMatcher.group(2)?.trim()
//                    ?: accountMatcher.group(3)?.trim() ?: "Not Found"
//        }
//
//        // Match recipient name
//        val recipientNameMatcher = AppConstants.regexForRecipientName.matcher(body)
//        if (recipientNameMatcher.find()) {
//            recipientName = recipientNameMatcher.group(1)?.trim() ?: "Not Found"
//        }
//
//        // Match "To" name
//        val toNameMatcher = AppConstants.regexForToName.matcher(body)
//        if (toNameMatcher.find()) {
//            toName = toNameMatcher.group(0)?.trim() ?: "Not Found"
//        }
//
//        // Match "From" name
//        val senderNameMatcher = AppConstants.regexForSenderName.matcher(body)
//        if (senderNameMatcher.find()) {
//            senderName = senderNameMatcher.group(0)?.trim() ?: "Not Found"
//        }
//
//        // Match transaction amount
//        val transactionAmountMatcher = AppConstants.regexForTransactionAmount.matcher(body)
//        if (transactionAmountMatcher.find()) {
//            transactionAmount =
//                transactionAmountMatcher.group(0)?.replace("[rR][sS]\\.?\\s*".toRegex(), "")?.trim()
//                    ?: "Not Found"
//        }
//
//        Log.d(
//            "DsK",
//            "Type: Transaction Notification\nAccount Number: $accountNumber\nRecipient Name: $recipientName\nTo Name: $toName\nSender Name: $senderName\nTransaction Amount: $transactionAmount"
//        )
//
//        return """
//        Type: Transaction Notification
//        Account Number: $accountNumber
//        Recipient Name: $recipientName
//        To Name: $toName
//        Sender Name: $senderName
//        Transaction Amount: $transactionAmount
//    """.trimIndent()
//    }
//
//    /**
//     * Main function to determine which extraction function to call.
//     */
//    private fun extractDetails(body: String): String {
//        return when {
//            body.contains("EMI", ignoreCase = true) -> {
//                val emiDetails = extractEmiDetails(body)
//                val accountDetails = extractTransactionDetails(body)
//                "$emiDetails\n$accountDetails"
//            }
//
//            body.contains("A/C", ignoreCase = true) || body.contains(
//                "UPI", ignoreCase = true
//            ) -> extractTransactionDetails(body)
//
//            else -> "No specific details found."
//        }
//    }
//}
