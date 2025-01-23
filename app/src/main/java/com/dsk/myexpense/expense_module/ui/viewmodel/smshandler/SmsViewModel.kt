package com.dsk.myexpense.expense_module.ui.viewmodel.smshandler

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class SmsViewModel(application: Application) : AndroidViewModel(application) {

    private val _isPermissionGranted = MutableLiveData<Boolean>()
    val isPermissionGranted: LiveData<Boolean> get() = _isPermissionGranted

    fun setPermissionGranted(granted: Boolean) {
        _isPermissionGranted.value = granted
    }

}
