package com.dsk.myexpense.expense_module.util

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.ui.viewmodel.smshandler.SmsReceiverViewModel

class SmsActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AppConstants.NOTIFICATION_ACTION_ADD -> {
                handleAddAction(context, intent)
                val notificationId = intent.getIntExtra(BundleKeyValues.NOTIFICATION_ID, 0)
                dismissNotification(context, notificationId)
            }
            else -> {
                Log.d("SmsActionReceiver", "Unknown action received.")
                val notificationId = intent.getIntExtra(BundleKeyValues.NOTIFICATION_ID, 0)
                dismissNotification(context, notificationId)
            }
        }
    }

    private fun dismissNotification(context: Context, notificationId: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (ActivityCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManager.cancel(notificationId)
//            Log.d("SmsActionReceiver", "Notification dismissed with ID: $notificationId")
        } else {
            Log.e("SmsActionReceiver", "Permission to cancel notifications not granted")
        }
    }

    private fun handleAddAction(context: Context, intent: Intent) {
        try {
            val messageSender = intent.getStringExtra(BundleKeyValues.NOTIFICATION_KEY_EXPENSE_MESSAGE_SENDER) ?: AppConstants.EMPTY_STRING
            val description = intent.getStringExtra(BundleKeyValues.NOTIFICATION_KEY_EXPENSE_MESSAGE_DESCRIPTION) ?: AppConstants.EMPTY_STRING
            val amount = intent.getDoubleExtra(BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT, 0.0)
            val date = intent.getLongExtra(BundleKeyValues.NOTIFICATION_KEY_EXPENSE_DATE, -1L)
            val isIncome = intent.getBooleanExtra(BundleKeyValues.NOTIFICATION_KEY_EXPENSE_IS_INCOME, false)
            val receiverName = intent.getStringExtra(BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT_RECEIVER_NAME) ?: AppConstants.EMPTY_STRING
            val type = intent.getStringExtra(BundleKeyValues.NOTIFICATION_KEY_EXPENSE_TYPE) ?: AppConstants.EMPTY_STRING
            val senderName = intent.getStringExtra(BundleKeyValues.NOTIFICATION_KEY_EXPENSE_AMOUNT_SENDER_NAME) ?: AppConstants.EMPTY_STRING
            val categoryName =  intent.getStringExtra(BundleKeyValues.NOTIFICATION_KEY_EXPENSE_CATEGORY_NAME) ?: "Other Expenses"

            var currentDateTime = if (date != -1L) {
                date
            } else {
                System.currentTimeMillis()
            }

            if (currentDateTime == -1L) {
                currentDateTime = System.currentTimeMillis()
            }

//            Log.d("DsK", "handleAddAction Processing ADD action with details: messageSender=$messageSender, amount=$amount, date=$date")

            // Initialize ViewModel
            val expenseRepository = (context.applicationContext as ExpenseApplication).expenseRepository
            val viewModel = SmsReceiverViewModel(expenseRepository)

            // Save the transaction
            viewModel.saveTransaction(
                context,
                senderName = senderName,
                messageSender,
                receiverName,
                description,
                amount,
                currentDateTime,
                categoryName,
                null,
                isIncome = isIncome
            )
            Toast.makeText(context, "Transaction saved successfully", Toast.LENGTH_SHORT).show()
            // Start MainActivity
            val launchIntent = Intent().apply {
                component = ComponentName(context.packageName, AppConstants.NOTIFICATION_ACTION_MAIN_ACTIVITY)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(launchIntent)

        } catch (e: Exception) {
            Log.e("SmsActionReceiver", "Error handling ADD action", e)
        }
    }
}

