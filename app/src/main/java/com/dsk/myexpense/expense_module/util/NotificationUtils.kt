package com.dsk.myexpense.expense_module.util

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.core.MainActivity
import com.dsk.myexpense.expense_module.data.model.ExpenseMessageDetails
import com.dsk.myexpense.expense_module.ui.viewmodel.smshandler.SmsReceiverViewModel
import java.io.File

class NotificationUtils {
    companion object {

        private const val NOTIFICATION_EXPENSE_CHANNEL_ID = "sms_channel"
        private const val NOTIFICATION_EXPENSE_CHANNEL_NAME = "SMS Notifications"
        private const val NOTIFICATION_EXPENSE_CHANNEL_DESCRIPTION =
            "Channel for SMS transaction notifications"
        private const val NOTIFICATION_EXPENSE_PDF_DOWNLOAD_CHANNEL_ID = "pdf_channel"
        private const val NOTIFICATION_EXPENSE_PDF_DOWNLOAD_CHANNEL_NAME = "PDF Notifications"

        private fun createNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channelId = NOTIFICATION_EXPENSE_CHANNEL_ID
                val channelName = NOTIFICATION_EXPENSE_CHANNEL_NAME
                val channelDescription = NOTIFICATION_EXPENSE_CHANNEL_DESCRIPTION
                val importance = NotificationManager.IMPORTANCE_HIGH

                // Only create the channel if it doesn't already exist
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (notificationManager.getNotificationChannel(channelId) == null) {
                    val channel = NotificationChannel(channelId, channelName, importance).apply {
                        description = channelDescription
                    }
                    notificationManager.createNotificationChannel(channel)
                }
            }
        }

        fun showNotification(
            context: Context, messageDetails: ExpenseMessageDetails, messageBody: String
        ) {
            // Ensure the notification channel exists
            createNotificationChannel(context)

            val notificationId = 1
            val channelId = NOTIFICATION_EXPENSE_CHANNEL_ID

            val addIntent = Intent(context, SmsActionReceiver::class.java).apply {
                action = AppConstants.NOTIFICATION_ACTION_ADD
                putExtra(
                    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_CATEGORY_NAME,
                    messageDetails.categoryName
                )
                putExtra(
                    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_MESSAGE_SENDER,
                    messageDetails.expenseMessageSender
                )
                putExtra(BundleKeyValues.NOTIFICATION_KEY_EXPENSE_MESSAGE_DESCRIPTION, messageBody)
                putExtra(
                    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT, messageDetails.expenseAmount
                )
                putExtra(BundleKeyValues.NOTIFICATION_KEY_EXPENSE_TYPE, messageDetails.expenseType)
                putExtra(BundleKeyValues.NOTIFICATION_KEY_EXPENSE_DATE, messageDetails.expenseDate)
                putExtra(
                    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT_RECEIVER_NAME,
                    messageDetails.receiverName
                )
                putExtra(
                    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT_SENDER_NAME,
                    messageDetails.senderName
                )
                putExtra(
                    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_IS_INCOME, messageDetails.isIncome
                )
                putExtra(BundleKeyValues.NOTIFICATION_ID, notificationId)
            }
            val addPendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                addIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val denyIntent = Intent(context, SmsActionReceiver::class.java).apply {
                action = AppConstants.NOTIFICATION_ACTION_DENY
                putExtra(BundleKeyValues.NOTIFICATION_ID, notificationId)
            }

            val denyPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                denyIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Ensure the notification opens MainActivity and triggers showTransactionDialog
            val contentIntent = getNotificationContentIntent(context, messageDetails, messageBody)

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(context.getString(R.string.text_new_transaction_header))
                .setContentText("$messageBody on ${messageDetails.expenseDate}")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(contentIntent) // Set the content intent for the notification click action
                .addAction(
                    android.R.drawable.ic_input_add,
                    context.getString(R.string.text_add),
                    addPendingIntent
                ).addAction(
                    android.R.drawable.ic_delete,
                    context.getString(R.string.text_deny),
                    denyPendingIntent
                ).build()

            with(NotificationManagerCompat.from(context)) {
                if (ActivityCompat.checkSelfPermission(
                        context, Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(notificationId, notification)
                } else {
                    Log.e("DsK SmsReceiver", "Notification permission not granted")
                }
            }
        }

        private fun getNotificationContentIntent(
            context: Context, messageDetails: ExpenseMessageDetails, description: String
        ): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(
                    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_CATEGORY_NAME,
                    messageDetails.categoryName
                )
                putExtra(
                    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_MESSAGE_SENDER,
                    messageDetails.expenseMessageSender
                )
                putExtra(BundleKeyValues.NOTIFICATION_KEY_EXPENSE_MESSAGE_DESCRIPTION, description)
                putExtra(
                    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT, messageDetails.expenseAmount
                )
                putExtra(BundleKeyValues.NOTIFICATION_KEY_EXPENSE_TYPE, messageDetails.expenseType)
                putExtra(BundleKeyValues.NOTIFICATION_KEY_EXPENSE_DATE, messageDetails.expenseDate)
                putExtra(
                    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT_RECEIVER_NAME,
                    messageDetails.receiverName
                )
                putExtra(
                    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT_SENDER_NAME,
                    messageDetails.senderName
                )
                putExtra(
                    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_IS_INCOME, messageDetails.isIncome
                )
                putExtra(
                    BundleKeyValues.NOTIFICATION_KEY_EXPENSE_MESSAGE_DETAILS, messageDetails
                ) // Pass the object as Serializable
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            return PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        fun showPdfSavedNotification(context: Context, file: File) {
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create a notification channel for Android 8.0 and higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    NOTIFICATION_EXPENSE_PDF_DOWNLOAD_CHANNEL_ID,
                    NOTIFICATION_EXPENSE_PDF_DOWNLOAD_CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = context.getString(R.string.text_notification_pdf_info_description)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // Create an intent to open the PDF
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    FileProvider.getUriForFile(context, "${context.packageName}.provider", file),
                    AppConstants.APP_PDF_FORMAT
                )
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Build the notification
            val notification =
                NotificationCompat.Builder(context, NOTIFICATION_EXPENSE_PDF_DOWNLOAD_CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_pdf) // Replace with your app's PDF icon
                    .setContentTitle(context.getString(R.string.text_notification_pdf_saved_message_title))
                    .setContentText(context.getString(R.string.text_notification_pdf_saved_message_description))
                    .setContentIntent(pendingIntent).setAutoCancel(true).build()

            // Show the notification
            notificationManager.notify(1, notification)
        }

        fun hasNotificationPermission(context: Context): Boolean {
            return ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }

        // Show the transaction dialog when the notification is clicked
        fun showTransactionDialog(
            context: Context, messageDetails: ExpenseMessageDetails, description: String, date: Long
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
            builder.setTitle(R.string.text_new_transaction_header).setMessage(
                "Sender: ${messageDetails.senderName}\n" + "Receiver: ${messageDetails.receiverName}\n" + "Description: $description\n" + "Amount: ${messageDetails.expenseAmount}\n" + "Date: $date\n" + "Type: ${
                    if (messageDetails.isIncome!!) context.getString(R.string.text_income)
                    else context.getString(R.string.text_expense)
                }"
            ).setPositiveButton(R.string.text_add) { _, _ ->
                val expenseRepository = (context.application as ExpenseApplication).expenseRepository
                val isIncome = messageDetails.isIncome ?: false
                val categoryNameValue = messageDetails.categoryName ?: AppConstants.EMPTY_STRING
                val viewModel = SmsReceiverViewModel(expenseRepository)
                viewModel.saveTransaction(
                    context,
                    messageDetails.senderName,
                    messageDetails.expenseMessageSender ?: AppConstants.EMPTY_STRING,
                    messageDetails.receiverName ?: AppConstants.EMPTY_STRING,
                    description,
                    messageDetails.expenseAmount,
                    date,
                    categoryName = categoryNameValue,
                    isIncome = isIncome,
                    invoiceImage = null,
                )
            }.setNegativeButton(R.string.text_deny) { dialog, _ -> dialog.dismiss() }.show()
        }
    }
}


