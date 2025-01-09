package com.dsk.myexpense.expense_module.ui

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.lifecycle.MutableLiveData

class NotificationListener : NotificationListenerService() {

    companion object {
        private val _notificationCount = MutableLiveData<Int>()
        val notificationCount: MutableLiveData<Int> get() = _notificationCount
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        updateNotificationCount()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        updateNotificationCount()
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        updateNotificationCount()
    }

    private fun updateNotificationCount() {
        val activeNotifications = activeNotifications
        _notificationCount.postValue(activeNotifications.size)
    }
}
