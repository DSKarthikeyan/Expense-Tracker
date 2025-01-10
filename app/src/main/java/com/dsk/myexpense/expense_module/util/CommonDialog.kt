package com.dsk.myexpense.expense_module.util

import android.content.Context
import android.view.LayoutInflater
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.ui.adapter.CurrencyAdapter
import com.dsk.myexpense.expense_module.ui.viewmodel.AppLoadingViewModel
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommonDialog {

    fun showCurrencySelectionDialog(
        context: Context,
        appLoadingViewModel: AppLoadingViewModel,
        onCurrencySelected: (String, Double) -> Unit
    ) {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_currency_selection, null)

        val dialog = AlertDialog.Builder(context)
            .setView(view)
            .setCancelable(true)
            .create()

        val recyclerView = view.findViewById<RecyclerView>(R.id.recyclerViewCurrencies)
        val searchEditText = view.findViewById<TextInputEditText>(R.id.searchEditText)

        // Fetch and filter unique currencies
        CoroutineScope(Dispatchers.IO).launch {
            val allCurrencies = appLoadingViewModel.getCurrenciesFromLocalDB()
            val distinctCurrencies = allCurrencies.distinctBy { it.name } // Assuming 'name' is a unique field
            withContext(Dispatchers.Main) {
                val adapter = CurrencyAdapter(distinctCurrencies) { selectedCurrency ->
                    onCurrencySelected(selectedCurrency.name, selectedCurrency.code)
                    dialog.dismiss()
                }

                recyclerView.adapter = adapter
                recyclerView.layoutManager = LinearLayoutManager(context)

                // Add search functionality
//                searchEditText.addTextChangedListener { editableQuery ->
//                    val query = editableQuery.toString()
//                    // Filter the adapter based on the search query
//                    val filteredList = distinctCurrencies.filter {
//                        it.name.contains(query, ignoreCase = true)
//                    }
//                    adapter.updateCurrencies(filteredList) // Ensure `updateCurrencies` is defined in your adapter
//                }

                searchEditText.addTextChangedListener { query ->
                    // Filter the adapter based on the search query
                    adapter.filter(query.toString())
//                    adapter.updateCurrencies(filteredList)
                }
            }
        }

        dialog.show()
    }


}

