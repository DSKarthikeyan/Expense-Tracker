package com.dsk.myexpense.expense_module.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.data.model.CardEntity
import androidx.recyclerview.widget.DiffUtil

class CardAdapter(
    private val onCardClick: (CardEntity) -> Unit
) : ListAdapter<CardEntity, CardAdapter.CardViewHolder>(CardDiffCallback()) {

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardName: TextView = itemView.findViewById(R.id.cardName)
        private val cardNumber: TextView = itemView.findViewById(R.id.cardNumber)
        private val cardExpiry: TextView = itemView.findViewById(R.id.cardExpiry)

        fun bind(card: CardEntity) {
            Log.d("CardAdapter", "Binding card: ${card.nameOnCard}, ${card.cardNumber}, ${card.expiryDate}")
            cardName.text = card.nameOnCard
            cardNumber.text = maskCardNumber(card.cardNumber)
            cardExpiry.text = card.expiryDate

            itemView.setOnClickListener {
                onCardClick(card)
            }
        }

        private fun maskCardNumber(cardNumber: String): String {
            return cardNumber.replace(Regex("\\d(?=\\d{4})"), "*")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cards, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        holder.bind(getItem(position))  // Use getItem instead of directly accessing the list
    }

    // DiffUtil Callback for better performance
    class CardDiffCallback : DiffUtil.ItemCallback<CardEntity>() {
        override fun areItemsTheSame(oldItem: CardEntity, newItem: CardEntity): Boolean {
            return oldItem.cardNumber == newItem.cardNumber  // Compare based on unique ID (cardNumber in this case)
        }

        override fun areContentsTheSame(oldItem: CardEntity, newItem: CardEntity): Boolean {
            return oldItem == newItem  // Compare all fields if needed
        }
    }
}