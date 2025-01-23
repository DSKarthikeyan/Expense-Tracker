package com.dsk.myexpense.expense_module.ui.view.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.FragmentWalletAccountsBinding

class AccountsFragment : Fragment() {

    private var _binding: FragmentWalletAccountsBinding? = null
    private val fragmentWalletAccountsBinding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletAccountsBinding.inflate(inflater, container, false)
        return fragmentWalletAccountsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragmentWalletAccountsBinding.btnNext.setOnClickListener {
            Toast.makeText(requireContext(), "Not Implemented!", Toast.LENGTH_SHORT).show()
            // Add navigation or action logic here
        }
        
        // Set default selection
        updateItemSelection(
            fragmentWalletAccountsBinding.itemBankLink,
            fragmentWalletAccountsBinding.tvBankLinkTitle,
            fragmentWalletAccountsBinding.tvBankLinkDescription,
            fragmentWalletAccountsBinding.imgBankLinkCheck
        )

        // Add click listeners for each item
        fragmentWalletAccountsBinding.itemBankLink.setOnClickListener {
            updateItemSelection(
                fragmentWalletAccountsBinding.itemBankLink,
                fragmentWalletAccountsBinding.tvBankLinkTitle,
                fragmentWalletAccountsBinding.tvBankLinkDescription,
                fragmentWalletAccountsBinding.imgBankLinkCheck
            )
        }

        fragmentWalletAccountsBinding.itemMicroDeposits.setOnClickListener {
            updateItemSelection(
                fragmentWalletAccountsBinding.itemMicroDeposits,
                fragmentWalletAccountsBinding.tvMicrodepositsTitle,
                fragmentWalletAccountsBinding.tvMicrodepositsDescription,
                fragmentWalletAccountsBinding.imgMicrodepositsCheck
            )
        }

        fragmentWalletAccountsBinding.itemPaypal.setOnClickListener {
            updateItemSelection(
                fragmentWalletAccountsBinding.itemPaypal,
                fragmentWalletAccountsBinding.tvPaypalTitle,
                fragmentWalletAccountsBinding.tvPaypalDescription,
                fragmentWalletAccountsBinding.imgPaypalCheck
            )
        }
    }

    private fun updateItemSelection(
        selectedItem: View,
        selectedText: TextView,
        selectedDescriptionText: TextView,
        selectedCheckmark: View
    ) {
        // Reset all items to unselected state
        resetItem(
            fragmentWalletAccountsBinding.itemBankLink,
            fragmentWalletAccountsBinding.tvBankLinkTitle,
            fragmentWalletAccountsBinding.tvBankLinkDescription,
            fragmentWalletAccountsBinding.imgBankLinkCheck
        )
        resetItem(
            fragmentWalletAccountsBinding.itemMicroDeposits,
            fragmentWalletAccountsBinding.tvMicrodepositsTitle,
            fragmentWalletAccountsBinding.tvMicrodepositsDescription,
            fragmentWalletAccountsBinding.imgMicrodepositsCheck
        )
        resetItem(
            fragmentWalletAccountsBinding.itemPaypal,
            fragmentWalletAccountsBinding.tvPaypalTitle,
            fragmentWalletAccountsBinding.tvPaypalDescription,
            fragmentWalletAccountsBinding.imgPaypalCheck
        )

        // Set selected item to selected state
        selectedItem.setBackgroundResource(R.drawable.account_item_selected)
        selectedText.setTextColor(requireContext().getColor(R.color.green)) // Change text color to green
        selectedDescriptionText.setTextColor(requireContext().getColor(R.color.green)) // Change text color to green
        selectedCheckmark.visibility = View.VISIBLE
    }

    private fun resetItem(
        item: View, textView: TextView, textDescriptionView: TextView, checkmark: View
    ) {
        item.setBackgroundResource(R.drawable.account_item_unselected)
        textView.setTextColor(requireContext().getColor(R.color.black)) // Reset text color to black
        textDescriptionView.setTextColor(requireContext().getColor(R.color.black)) // Reset text color to black
        checkmark.visibility = View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

