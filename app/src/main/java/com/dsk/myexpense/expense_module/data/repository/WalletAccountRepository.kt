package com.dsk.myexpense.expense_module.data.repository

import com.dsk.myexpense.expense_module.data.model.CardEntity
import com.dsk.myexpense.expense_module.data.source.local.db.AccountDao
import com.dsk.myexpense.expense_module.data.source.local.db.CardDao

class WalletAccountRepository(private val cardDAO: CardDao,
    private val accountDao: AccountDao) {

    fun getAllCards() = cardDAO.getAllCards()

    suspend fun insertCardDetails(cardEntity: CardEntity) = cardDAO.insertCard(cardEntity)

    suspend fun updateAccountStatus(accountType: String, isConnected: Boolean) = accountDao.updateAccountStatus(accountType, isConnected)
}