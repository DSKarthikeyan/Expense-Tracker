package com.dsk.myexpense.expense_module.ui.view.wallet

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.FragmentWalletCardsBinding
import com.dsk.myexpense.expense_module.data.model.CardEntity
import com.dsk.myexpense.expense_module.ui.adapter.CardAdapter
import com.dsk.myexpense.expense_module.ui.viewmodel.WalletViewModel

class CardsFragment : Fragment() {

    private var _binding: FragmentWalletCardsBinding? = null
    private val fragmentCardsBinding get() = _binding!!
    private val viewModel: WalletViewModel by viewModels()

    private lateinit var cardAdapter: CardAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletCardsBinding.inflate(inflater, container, false)
        return fragmentCardsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cardAdapter = CardAdapter { card ->
            onCardClicked(card)
        }

        val linearLayoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        fragmentCardsBinding.cardList.apply {
            layoutManager = linearLayoutManager
            adapter = cardAdapter
        }

        fragmentCardsBinding.addCardButton.setOnClickListener {
            handleAddCard()
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.cards.observe(viewLifecycleOwner) { cards ->
            if (!cards.isNullOrEmpty()) {
                Log.d("CardsFragment", "Loaded ${cards.size} cards.")
                updateCardList(cards)
                fragmentCardsBinding.cardList.visibility = View.VISIBLE
                fragmentCardsBinding.emptyText.visibility = View.GONE
            } else {
                Log.d("CardsFragment", "No cards found.")
                fragmentCardsBinding.cardList.visibility = View.GONE
                fragmentCardsBinding.emptyText.visibility = View.VISIBLE
                Toast.makeText(requireContext(), "No cards available", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateCardList(cards: List<CardEntity>) {
        cardAdapter.submitList(cards)
    }

    private fun onCardClicked(card: CardEntity) {
        Toast.makeText(requireContext(), "Clicked: ${card.nameOnCard}", Toast.LENGTH_SHORT).show()
        showCardDetails(card)
    }

    private fun handleAddCard() {
        val nameOnCard = fragmentCardsBinding.nameOnCardInputLayout.editText?.text.toString()
        val cardNumber = fragmentCardsBinding.cardNumberInputLayout.editText?.text.toString()
        val expiryDate = fragmentCardsBinding.expiryDateInputLayout.editText?.text.toString()
        val cvc = fragmentCardsBinding.cvcInputLayout.editText?.text.toString()
        val zip = fragmentCardsBinding.zipInputLayout.editText?.text.toString()

        // Validate the input fields
        if (nameOnCard.isBlank()) {
            Toast.makeText(requireContext(), "Name on Card is required", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidCardNumber(cardNumber)) {
            fragmentCardsBinding.cardNumberInputLayout.error = "Invalid Card Number"
            return
        } else {
            fragmentCardsBinding.cardNumberInputLayout.error = null
        }

        if (!isValidExpiryDate(expiryDate)) {
            Toast.makeText(requireContext(), "Invalid Expiry Date", Toast.LENGTH_SHORT).show()
            return
        }

        if (cvc.length != 3 || !cvc.all { it.isDigit() }) {
            Toast.makeText(requireContext(), "Invalid CVC", Toast.LENGTH_SHORT).show()
            return
        }

        if (zip.isBlank() || !zip.all { it.isDigit() }) {
            Toast.makeText(requireContext(), "Invalid Zip Code", Toast.LENGTH_SHORT).show()
            return
        }

        val newCard = CardEntity(
            nameOnCard = nameOnCard,
            cardNumber = cardNumber,
            expiryDate = expiryDate,
            cvc = cvc,
            zip = zip
        )

        viewModel.addCard(newCard)
        Toast.makeText(requireContext(), "Card added successfully!", Toast.LENGTH_SHORT).show()
        clearInputFields()
    }

    /**
     * Validates a card number using the Luhn algorithm.
     * @param cardNumber The card number to validate.
     * @return true if the card number is valid(4532015112830366), false otherwise(1234567812345678).
     */
    private fun isValidCardNumber(cardNumber: String): Boolean {
        // Ensure the card number contains only digits and has a valid length
        if (cardNumber.any { !it.isDigit() }) return false

        var sum = 0
        var shouldDouble = false

        // Process digits from right to left
        for (i in cardNumber.length - 1 downTo 0) {
            var digit = cardNumber[i].toString().toInt()

            if (shouldDouble) {
                digit *= 2
                if (digit > 9) digit -= 9
            }

            sum += digit
            shouldDouble = !shouldDouble
        }

        // Valid if the sum is a multiple of 10
        return sum % 10 == 0
    }


    private fun isValidExpiryDate(expiryDate: String): Boolean {
        val regex = Regex("^\\d{2}/\\d{2}$")
        if (!expiryDate.matches(regex)) return false

        val (month, year) = expiryDate.split("/").map { it.toIntOrNull() ?: 0 }
        return month in 1..12 && year >= 0
    }

    private fun clearInputFields() {
        fragmentCardsBinding.nameOnCardInputLayout.editText?.text?.clear()
        fragmentCardsBinding.cardNumberInputLayout.editText?.text?.clear()
        fragmentCardsBinding.expiryDateInputLayout.editText?.text?.clear()
        fragmentCardsBinding.cvcInputLayout.editText?.text?.clear()
        fragmentCardsBinding.zipInputLayout.editText?.text?.clear()
    }

    private fun showCardDetails(card: CardEntity) {
        val dialogBuilder = AlertDialog.Builder(requireContext())
        dialogBuilder.setTitle("Card Details")
            .setMessage(
                "Card Name: ${card.nameOnCard}\n" +
                        "Card Number: ${maskCardNumber(card.cardNumber)}\n" +
                        "Expiry Date: ${card.expiryDate}\n" +
                        "CVC: ${card.cvc}\n" +
                        "Zip Code: ${card.zip}"
            )
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        dialogBuilder.create().show()
    }

    private fun maskCardNumber(cardNumber: String): String {
        return cardNumber.replace(Regex("\\d(?=\\d{4})"), "*")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
