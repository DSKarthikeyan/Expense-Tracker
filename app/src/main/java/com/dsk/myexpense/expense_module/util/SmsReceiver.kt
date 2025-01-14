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
                val messageDetails = extractDetails(
                    regexHelper = RegexHelper(), smsMessage = SMSMessage(
                        address = sender,
                        body = messageBody,
                        time = dateString,
                    ), application
                )

                Log.d("DsK", "onReceive messageDetails $messageDetails")
                if (messageDetails.expenseType != "Not Valid") {
                    messageDetails.apply {
                        expenseMessageSender = sender
                        isIncome = expenseType == application.getString(R.string.text_income)
                        categoryName =
                            determineCategory(context, messageBody, messageDetails.receiverName)
                        expenseDate = dateString
                    }
                    Log.d("DsK SmsReceiver", "categoryName: ${messageDetails.categoryName}")
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

    private fun extractDetails(
        regexHelper: RegexHelper, smsMessage: SMSMessage, application: Application
    ): ExpenseMessageDetails {
        suggestionDetector = SuggestionDetectorImpl(regexHelper, application)
        val messageSuggestion =
            (suggestionDetector as SuggestionDetectorImpl).detectSuggestions(smsMessage)
        if (messageSuggestion != null) {
            return ExpenseMessageDetails(
                "",
                expenseType = messageSuggestion.expenseType,
                expenseAmount = messageSuggestion.amount,
                receiverName = messageSuggestion.paidTo ?: "",
                senderName = messageSuggestion.referenceMessageSender,
                additionalDetails = "Location: ${messageSuggestion.referenceMessage}",
                expenseDate = messageSuggestion.time,
                isIncome = messageSuggestion.isExpense,
                categoryName = messageSuggestion.referenceMessageSender
            )
        }
        return ExpenseMessageDetails(
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