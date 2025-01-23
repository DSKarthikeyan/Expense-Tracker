package com.dsk.myexpense.expense_module.ui.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dsk.myexpense.expense_module.data.model.CardEntity
import com.dsk.myexpense.expense_module.data.repository.WalletAccountRepository
import kotlinx.coroutines.launch

class WalletViewModel(private val walletAccountRepository: WalletAccountRepository) : ViewModel() {

    // Use MutableLiveData to update the data, then expose it as LiveData.
    private val _cards = MutableLiveData<List<CardEntity>>()
    val cards: LiveData<List<CardEntity>> = _cards

    init {
        getAllCards()
    }

    private fun getAllCards() {
        viewModelScope.launch {
            // Update the MutableLiveData to trigger observers.
            _cards.postValue(walletAccountRepository.getAllCards())
        }
    }

    fun addCard(card: CardEntity) {
        viewModelScope.launch {
            walletAccountRepository.insertCardDetails(card)
        }
    }

    fun updateAccountStatus(accountType: String, isConnected: Boolean) {
        viewModelScope.launch {
            walletAccountRepository.updateAccountStatus(accountType, isConnected)
        }
    }
}
