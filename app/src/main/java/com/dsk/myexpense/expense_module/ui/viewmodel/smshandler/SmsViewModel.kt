package com.dsk.myexpense.expense_module.ui.viewmodel.smshandler

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dsk.myexpense.expense_module.util.PermissionManager

class SmsViewModel(application: Application) : AndroidViewModel(application) {

    private val _isPermissionGranted = MutableLiveData<Boolean>()
    val isPermissionGranted: LiveData<Boolean> get() = _isPermissionGranted

    /**
     * Checks if all necessary permissions are granted.
     */
    fun checkPermissions(context: Context): Boolean {
        val permissions = listOf(
            android.Manifest.permission.READ_SMS,
            android.Manifest.permission.RECEIVE_SMS,
            android.Manifest.permission.READ_PHONE_STATE
        )
        return permissions.all { PermissionManager.hasPermission(context, it) }
    }

}
