package com.dsk.myexpense.expense_module.ui.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.dsk.myexpense.R
import com.dsk.myexpense.expense_module.data.model.CardEntity
import androidx.recyclerview.widget.DiffUtil
import com.google.android.material.materialswitch.MaterialSwitch

class CardAdapter(
    private val onCardClick: (CardEntity) -> Unit
) : ListAdapter<CardEntity, CardAdapter.CardViewHolder>(CardDiffCallback()) {

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardHolderName: TextView = itemView.findViewById(R.id.cardHolderName)
        private val cardNumber: TextView = itemView.findViewById(R.id.cardNumber)
        private val cardExpiry: TextView = itemView.findViewById(R.id.cardExpiry)
        private val cardType: TextView = itemView.findViewById(R.id.cardType)
        private val switchShowCardNumber: MaterialSwitch = itemView.findViewById(R.id.switchShowCardNumber)

        private var isCardNumberVisible = false

        fun bind(card: CardEntity) {
            Log.d(
                "CardAdapter",
                "Binding card: ${card.nameOnCard}, ${card.cardNumber}, ${card.expiryDate}"
            )
            cardHolderName.text = card.nameOnCard
            cardNumber.text = maskCardNumber(card.cardNumber)
            cardExpiry.text = card.expiryDate


            // Set the Switch state based on whether the card number is visible or not
            switchShowCardNumber.isChecked = isCardNumberVisible

            // Listener for the switch toggle
            switchShowCardNumber.setOnCheckedChangeListener { _, isChecked ->
                isCardNumberVisible = isChecked
                cardNumber.text = if (isCardNumberVisible) {
                    card.cardNumber  // Show the full card number
                } else {
                    maskCardNumber(card.cardNumber)  // Mask the card number
                }
            }

            val cardTypeValue = determineCardType(card.cardNumber)
            cardType.text = cardTypeValue  // Set the card type text

            itemView.setOnClickListener {
                onCardClick(card)
            }
        }

        private fun maskCardNumber(cardNumber: String): String {
            return cardNumber.replace(Regex("\\d(?=\\d{4})"), "*")
        }
    }

    private fun determineCardType(cardNumber: String): String {
        return when {
            cardNumber.startsWith("4") -> "Visa"  // Visa cards typically start with '4'
            cardNumber.startsWith("5") -> "MasterCard"  // MasterCard cards typically start with '5'
            cardNumber.startsWith("3") && (cardNumber[1] == '4' || cardNumber[1] == '7') -> "American Express"  // American Express starts with '34' or '37'
            cardNumber.startsWith("6") -> "Discover"  // Discover cards typically start with '6'
            cardNumber.startsWith("35") -> "JCB"  // JCB cards typically start with '35'
            cardNumber.startsWith("2") -> "UnionPay"  // UnionPay cards typically start with '2'
            else -> "Test Card"  // For other cases
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