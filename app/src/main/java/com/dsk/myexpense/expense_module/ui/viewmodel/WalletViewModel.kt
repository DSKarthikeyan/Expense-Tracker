package com.dsk.myexpense.expense_module.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.dsk.myexpense.expense_module.data.model.CardEntity
import com.dsk.myexpense.expense_module.data.source.local.ExpenseTrackerDB
import kotlinx.coroutines.launch

class WalletViewModel(application: Application) : AndroidViewModel(application) {
    private val database = ExpenseTrackerDB.getDatabase(application)
    private val cardDao = database.cardDao()
    private val accountDao = database.accountDao()

    // Use MutableLiveData to update the data, then expose it as LiveData.
    private val _cards = MutableLiveData<List<CardEntity>>()
    val cards: LiveData<List<CardEntity>> = _cards

    val accounts = accountDao.getAccounts()

    init {
        getAllCards()
    }

    private fun getAllCards() {
        viewModelScope.launch {
            // Update the MutableLiveData to trigger observers.
            _cards.postValue(cardDao.getAllCards())
        }
    }

    fun addCard(card: CardEntity) {
        viewModelScope.launch {
            cardDao.insertCard(card)
        }
    }

    fun updateAccountStatus(accountType: String, isConnected: Boolean) {
        viewModelScope.launch {
            accountDao.updateAccountStatus(accountType, isConnected)
        }
    }
}
