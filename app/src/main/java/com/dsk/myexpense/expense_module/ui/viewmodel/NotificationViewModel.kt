package com.dsk.myexpense.expense_module.ui.viewmodel

import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.dsk.myexpense.expense_module.ui.NotificationListener
import com.dsk.myexpense.expense_module.util.NotificationUtils

class NotificationViewModel(
    private val notificationManager: NotificationManager,
    private val hasNotificationPermission: () -> Boolean,
    context: Context
) : ViewModel() {

    init {
        // Initialize by fetching the unread notification count
        refreshUnreadNotificationCount(context)
    }

    /**
     * Fetches the current unread notification count and updates the NotificationListener.
     */
    private fun refreshUnreadNotificationCount(context: Context) {
        val unreadCount = try {
            if (hasNotificationPermission()) {
                getUnreadNotificationCount(context)
            } else {
                0 // Default to zero if no permission
            }
        } catch (e: Exception) {
            // Log the error if needed and return a default value
            e.printStackTrace()
            0
        }
        // Update the NotificationListener with the fetched count
        NotificationListener.notificationCount.postValue(unreadCount)
    }

    /**
     * Retrieves the unread notification count from the NotificationManager.
     */
    private fun getUnreadNotificationCount(context: Context): Int {
        var unreadCount = 0

        // Check if notification permission is granted
        if (NotificationUtils.hasNotificationPermission(context)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Use NotificationManager for API 23 and above
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                unreadCount = notificationManager.activeNotifications.size
            }
            // For below API 23, implement logic if necessary
        }

        return unreadCount
    }

    /**
     * Clears all notifications from the status bar and refreshes the count.
     */
    fun clearAllNotifications(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.cancelAll()
            } else {
                // Add logic for pre-API 23 if needed
                notificationManager.cancelAll() // Fallback behavior
            }
        } catch (e: Exception) {
            e.printStackTrace() // Log any potential issues with clearing notifications
        } finally {
            // Refresh the unread notification count after clearing
            refreshUnreadNotificationCount(context = context)
        }
    }
}

//class NotificationViewModelFactory(
//    private val context: Context
//) : ViewModelProvider.Factory {
//
//    override fun <T : ViewModel> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
//            @Suppress("UNCHECKED_CAST")
//            return NotificationViewModel(
//                context = context,
//                notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager,
//                hasNotificationPermission = { NotificationUtils.hasNotificationPermission(context) }
//            ) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel class")
//    }
//}
