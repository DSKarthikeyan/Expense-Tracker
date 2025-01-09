package com.dsk.myexpense.expense_module.util

import android.net.Uri

sealed class InvoiceAction {
    data object Upload : InvoiceAction()
    data object Scan : InvoiceAction()
}