package com.dsk.myexpense.expense_module.ui.view.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.FragmentWalletCardsBinding
import com.dsk.myexpense.expense_module.data.model.CardEntity
import com.dsk.myexpense.expense_module.ui.adapter.CardAdapter
import com.dsk.myexpense.expense_module.ui.viewmodel.WalletViewModel
import com.dsk.myexpense.expense_module.util.swipeLayout.StackLayoutManager
import kotlin.math.abs
import kotlin.random.Random

class CardsFragment : Fragment() {

    private var _binding: FragmentWalletCardsBinding? = null
    private val fragmentCardsBinding get() = _binding!!
    private val viewModel: WalletViewModel by viewModels()

    private lateinit var cardAdapter: CardAdapter
    private lateinit var selectItems: Array<String>
    private val stackCount = 30
    private var randomPosition = 0
    private var stackLayoutManager: StackLayoutManager? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletCardsBinding.inflate(inflater, container, false)
        return fragmentCardsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView components
        stackLayoutManager = StackLayoutManager()

        cardAdapter = CardAdapter { card ->
            onCardClicked(card)
        }

//        binding.cardList.apply {
//            layoutManager = stackLayoutManager
//            adapter = cardAdapter
//        }

        // Add button click listener
//        binding.addCardButton.setOnClickListener {
//            handleAddCard()
//        }

        selectItems = resources.getStringArray(R.array.items)
        resetRandom()

        observeViewModel()
    }

    private fun observeViewModel() {
//        viewModel.cards.observe(viewLifecycleOwner) { cards ->
//            if (!cards.isNullOrEmpty()) {
//                Log.d("CardsFragment", "Loaded ${cards.size} cards.")
//                updateCardList(cards)
//                binding.cardList.visibility = View.VISIBLE
//                binding.emptyText.visibility = View.GONE
//            } else {
//                Log.d("CardsFragment", "No cards found.")
//                binding.cardList.visibility = View.GONE
//                binding.emptyText.visibility = View.VISIBLE
//                Toast.makeText(requireContext(), "No cards available", Toast.LENGTH_SHORT).show()
//            }
//        }
    }

    private fun updateCardList(cards: List<CardEntity>) {
        cardAdapter.submitList(cards)  // Use ListAdapter's submitList for updating data
    }

    private fun resetRandom() {
        randomPosition = abs(Random.nextInt() % stackCount)
        selectItems[0] = getString(R.string.smooth_scroll, randomPosition)
        selectItems[1] = getString(R.string.scroll, randomPosition)
    }

    private fun onCardClicked(card: CardEntity) {
        Toast.makeText(requireContext(), "Clicked: ${card.nameOnCard}", Toast.LENGTH_SHORT).show()
        showCardDetails(card)
    }

    private fun handleAddCard() {
        // Get the values from input fields
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
            Toast.makeText(requireContext(), "Invalid Card Number", Toast.LENGTH_SHORT).show()
            return
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

        // Create a new CardEntity
        val newCard = CardEntity(
            nameOnCard = nameOnCard,
            cardNumber = cardNumber,
            expiryDate = expiryDate,
            cvc = cvc,
            zip = zip
        )

        // Add the card to the database
        viewModel.addCard(newCard)

        Toast.makeText(requireContext(), "Card added successfully!", Toast.LENGTH_SHORT).show()

        // Clear the input fields
        clearInputFields()
    }

    private fun isValidCardNumber(cardNumber: String): Boolean {
        return cardNumber.length == 16 && cardNumber.all { it.isDigit() }
    }

    private fun isValidExpiryDate(expiryDate: String): Boolean {
        // Validate expiry date format MM/YY
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
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
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
