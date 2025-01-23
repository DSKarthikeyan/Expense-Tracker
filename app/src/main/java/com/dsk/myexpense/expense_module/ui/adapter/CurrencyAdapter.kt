package com.dsk.myexpense.expense_module.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.data.model.Currency

class CurrencyAdapter(
    private var currencies: List<Currency>,
    private val onCurrencySelected: (Currency) -> Unit
) : RecyclerView.Adapter<CurrencyAdapter.CurrencyViewHolder>() {

    private var filteredCurrencies: List<Currency> = currencies

    class CurrencyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textCurrencyName: TextView = view.findViewById(R.id.currencyName)
        val textCurrencyCode: TextView = view.findViewById(R.id.currencyCode)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CurrencyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_currency, parent, false)
        return CurrencyViewHolder(view)
    }

    override fun onBindViewHolder(holder: CurrencyViewHolder, position: Int) {
        val currency = filteredCurrencies[position]
        holder.textCurrencyName.text = currency.name
        holder.textCurrencyCode.text = "${currency.code}"

        holder.itemView.setOnClickListener {
            onCurrencySelected(currency)
        }
    }

    override fun getItemCount(): Int = filteredCurrencies.size

    fun filter(query: String) {
        filteredCurrencies = if (query.isEmpty()) {
            currencies
        } else {
            currencies.filter { it.name.contains(query, ignoreCase = true) }
        }
        notifyDataSetChanged()
    }

    fun updateCurrencies(newCurrencies: List<Currency>) {
        currencies = newCurrencies
        notifyDataSetChanged()
    }
}
