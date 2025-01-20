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
import com.dsk.myexpense.expense_module.data.model.SMSMessage
import com.dsk.myexpense.expense_module.ui.viewmodel.smshandler.SmsReceiverViewModel
import com.dsk.myexpense.expense_module.util.detector.RegexHelper
import com.dsk.myexpense.expense_module.util.detector.SuggestionDetector
import com.dsk.myexpense.expense_module.util.detector.SuggestionDetectorImpl

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private val processedMessages = HashSet<String>()
        private lateinit var suggestionDetector: SuggestionDetector
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (smsMessage in messages) {
                // Generate a unique key based on sender and timestamp to avoid processing duplicates
                val uniqueKey = smsMessage.displayOriginatingAddress + smsMessage.timestampMillis
                if (processedMessages.contains(uniqueKey)) {
                    Log.d("SmsReceiver", "Duplicate SMS received, ignoring.")
                    return
                }
                processedMessages.add(uniqueKey)

                val sender = smsMessage.displayOriginatingAddress
                val messageBody = smsMessage.messageBody
                val dateString = smsMessage.timestampMillis

                // Get the application context
                val application = context.applicationContext as Application

                // Extract message details using the extractDetails function
                var messageDetails = extractDetails(
                    regexHelper = RegexHelper(),
                    smsMessage = SMSMessage(
                        address = sender,
                        body = messageBody,
                        time = dateString
                    ),
                    application = application
                )

                Log.d("DsK", "onReceive messageDetails: $messageDetails")

                // Check if the message contains valid expense details
                if (messageDetails.expenseType != "Not Valid") {
                    // Update the message details with additional information
                    messageDetails = messageDetails.copy( // Using copy() to create a new instance with updated properties
                        expenseMessageSender = sender,
                        isIncome = messageDetails.expenseType == application.getString(R.string.text_income),
                        categoryName = determineCategory(context, messageBody, messageDetails.receiverName ?: "receiverName"),
                        expenseDate = dateString
                    )

                    Log.d("DsK SmsReceiver", "categoryName: ${messageDetails.categoryName}")

                    // Check if the app is in the foreground
                    if (Utility.isAppInForeground(application)) {
                        val activityContext = Utility.getForegroundActivity(application)
                        if (activityContext != null) {
                            // Show the transaction dialog if the app is in the foreground
                            NotificationUtils.showTransactionDialog(
                                activityContext, messageDetails, messageBody, dateString
                            )
                        } else {
                            Log.e("SmsReceiver", "No active activity context found")
                        }
                    } else {
                        // Show the notification if the app is in the background
                        NotificationUtils.showNotification(
                            application, messageDetails, messageBody
                        )
                    }
                }
            }
        }
    }

    private fun extractDetails(
        regexHelper: RegexHelper, smsMessage: SMSMessage, application: Application
    ): ExpenseMessageDetails {

        // Initialize the SuggestionDetector with the regexHelper and application context
        val suggestionDetector = SuggestionDetectorImpl(regexHelper, application)

        // Detect message suggestion from the SMS
        val messageSuggestion = suggestionDetector.detectSuggestions(smsMessage)

        if (messageSuggestion != null) {
            // If suggestions are found, return the ExpenseMessageDetails populated with the relevant data
            return ExpenseMessageDetails(
                senderName = messageSuggestion.referenceMessageSender,
                expenseType = messageSuggestion.expenseType,
                expenseAmount = messageSuggestion.amount,
                receiverName = messageSuggestion.paidTo ?: "",
                expenseDate = messageSuggestion.time,
                isIncome = messageSuggestion.isExpense,
                categoryName = messageSuggestion.referenceMessageSender,
                additionalDetails = messageSuggestion.referenceMessage ?: ""
            )
        }

        // Return a default ExpenseMessageDetails object if no suggestions are found
        return ExpenseMessageDetails(
            senderName = "Unknown",
            expenseMessageSender = "Not Valid",
            expenseAmount = 0.0,
            receiverName = "Unknown",
            expenseDate = 0L,
            expenseType = "Not Valid",
            categoryName = "Unknown",
            isIncome = false,
            additionalDetails = "No specific details found."
        )
    }

    private fun determineCategory(context: Context, body: String, recipientName: String): String {
        // Keywords for each category
        val groceryKeywords =
            listOf("grocery", "supermarket", "mart", "store", "market", "bazaar", "provision")
        val netflixKeywords =
            listOf("netflix", "prime", "disney", "hulu", "subscription", "streaming")
        val rentKeywords = listOf("rent", "lease", "tenant", "landlord", "housing", "apartment")
        val paypalKeywords =
            listOf("paypal", "online payment", "transaction", "money transfer", "vpa")
        val starbucksKeywords =
            listOf("starbucks", "coffee", "cafe", "espresso", "latte", "beverage")
        val shoppingKeywords = listOf(
            "shopping", "clothing", "apparel", "fashion", "shoes", "electronics", "jewelry", "mall"
        )
        val transportKeywords =
            listOf("uber", "ola", "taxi", "cab", "bus", "train", "ticket", "transport", "commute")
        val utilitiesKeywords =
            listOf("electricity", "water", "bill", "utility", "power", "energy", "gas", "meter")
        val diningOutKeywords =
            listOf("restaurant", "dining", "food", "cafe", "bar", "bakery", "snack", "meals")
        val entertainmentKeywords =
            listOf("movie", "cinema", "theatre", "concert", "show", "performance")
        val healthcareKeywords = listOf(
            "hospital", "pharmacy", "medical", "doctor", "health", "medicines", "clinic", "test"
        )
        val insuranceKeywords = listOf("insurance", "premium", "policy", "cover", "claim")
        val subscriptionsKeywords = listOf("subscription", "membership", "plan")
        val educationKeywords =
            listOf("school", "college", "tuition", "exam", "course", "education", "university")
        val debtKeywords = listOf("debt", "loan", "emi", "repayment", "installment", "due")
        val giftsKeywords = listOf("gift", "donation", "charity", "present", "fundraiser", "help")
        val travelKeywords =
            listOf("travel", "trip", "tour", "flight", "airline", "booking", "holiday", "journey")
        val salaryKeywords =
            listOf("salary", "payroll", "wages", "income", "earnings", "credit alert")
        val freelanceKeywords = listOf("freelance", "contract work", "project", "consulting", "gig")
        val investmentsKeywords =
            listOf("investment", "dividend", "returns", "profit", "stock", "equity")
        val bonusKeywords = listOf("bonus", "incentive", "reward", "performance bonus", "payout")
        val rentalIncomeKeywords =
            listOf("rental", "lease income", "tenant payment", "rent collected")
        val otherIncomeKeywords =
            listOf("income", "credited", "govt payment", "upi/p2a", "miscellaneous")
        val otherKeywords = listOf("miscellaneous", "other")

        // Convert to lowercase for comparison
        val lowerBody = body.lowercase()
        val lowerRecipient = recipientName.lowercase()

        return when {
            groceryKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_grocery
            )

            netflixKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_netflix
            )

            rentKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_rent
            )

            paypalKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_paypal
            )

            starbucksKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_starbucks
            )

            shoppingKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_shopping
            )

            transportKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_transport
            )

            utilitiesKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_utilities
            )

            diningOutKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_dining_out
            )

            entertainmentKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_entertainment
            )

            healthcareKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_healthcare
            )

            insuranceKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_insurance
            )

            subscriptionsKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_subscriptions
            )

            educationKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_education
            )

            debtKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_debt
            )

            giftsKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_gifts
            )

            travelKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_travel
            )

            salaryKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_income_category_salary
            )

            freelanceKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_income_category_freelance
            )

            investmentsKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_income_category_investments
            )

            bonusKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_income_category_bonus
            )

            rentalIncomeKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_income_category_rental_income
            )

            otherIncomeKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_income_category_other_income
            )

            otherKeywords.any { lowerBody.contains(it) || lowerRecipient.contains(it) } -> context.getString(
                R.string.text_expenses_category_other
            )

            else -> context.getString(R.string.text_expenses_category_other)
        }
    }
}