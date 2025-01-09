package com.dsk.myexpense.expense_module.util.headerbar

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HeaderBarViewModel : ViewModel() {

    // LiveData for the header title
    private val _headerTitle = MutableLiveData<String>()
    val headerTitle: LiveData<String> get() = _headerTitle

    // LiveData for the left icon resource
    private val _leftIconResource = MutableLiveData<Int?>()
    val leftIconResource: LiveData<Int?> get() = _leftIconResource

    // LiveData for the right icon resource
    private val _rightIconResource = MutableLiveData<Int?>()
    val rightIconResource: LiveData<Int?> get() = _rightIconResource

    // LiveData for left icon visibility
    private val _isLeftIconVisible = MutableLiveData<Boolean>()
    val isLeftIconVisible: LiveData<Boolean> get() = _isLeftIconVisible

    // LiveData for right icon visibility
    private val _isRightIconVisible = MutableLiveData<Boolean>()
    val isRightIconVisible: LiveData<Boolean> get() = _isRightIconVisible

    // Functions to update values
    fun setHeaderTitle(title: String) {
        _headerTitle.value = title
    }

    fun setLeftIconResource(resourceId: Int?) {
        _leftIconResource.value = resourceId
    }

    fun setRightIconResource(resourceId: Int?) {
        _rightIconResource.value = resourceId
    }

    fun setLeftIconVisibility(isVisible: Boolean) {
        _isLeftIconVisible.value = isVisible
    }

    fun setRightIconVisibility(isVisible: Boolean) {
        _isRightIconVisible.value = isVisible
    }
}


