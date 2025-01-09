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
import androidx.core.app.ActivityCompat
import com.dsk.myexpense.expense_module.ui.viewmodel.smshandler.SmsReceiverViewModel

class SmsActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            AppConstants.ACTION_ADD -> {
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
            Log.d("SmsActionReceiver", "Notification dismissed with ID: $notificationId")
        } else {
            Log.e("SmsActionReceiver", "Permission to cancel notifications not granted")
        }
    }

    private fun handleAddAction(context: Context, intent: Intent) {
        try {
            val messageSender = intent.getStringExtra("messageSender") ?: ""
            val description = intent.getStringExtra("description") ?: ""
            val amount = intent.getDoubleExtra("amount", 0.0)
            val date = intent.getLongExtra("date", -1L)
            val isIncome = intent.getBooleanExtra("isIncome", false)
            val receiverName = intent.getStringExtra("receiverName") ?: ""
            val type = intent.getStringExtra("type") ?: ""
            val senderName = intent.getStringExtra("senderName") ?: ""
            val categoryName =  intent.getStringExtra("categoryName") ?: "Other Expenses"

            var currentDateTime = if (date != -1L) {
                date
            } else {
                System.currentTimeMillis()
            }

            if (currentDateTime == -1L) {
                currentDateTime = System.currentTimeMillis()
            }


            Log.d("DsK", "handleAddAction Processing ADD action with details: messageSender=$messageSender, amount=$amount, date=$date")

            Log.d("DsK","handleAddAction receiverName $receiverName, type $type, senderName $senderName, messageSender $messageSender,description $description,amount $amount,currentDateTime $currentDateTime, \"Other Expenses\", isIncome = $isIncome date=$date")
            // Initialize ViewModel
            val application = context.applicationContext as Application
            val viewModel = SmsReceiverViewModel(application)

            // Save the transaction
            viewModel.saveTransaction(
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
            Log.d("SmsActionReceiver", "Transaction saved successfully")

            // Start MainActivity
            val launchIntent = Intent().apply {
                component = ComponentName(context.packageName, "com.dsk.myexpense.expense_module.core.MainActivity")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(launchIntent)

        } catch (e: Exception) {
            Log.e("SmsActionReceiver", "Error handling ADD action", e)
        }
    }
}

