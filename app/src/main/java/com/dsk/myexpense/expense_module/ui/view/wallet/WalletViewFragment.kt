package com.dsk.myexpense.expense_module.ui.view.wallet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModelProvider
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.FragmentWalletDetailsBinding
import com.dsk.myexpense.expense_module.util.headerbar.HeaderBarView
import com.dsk.myexpense.expense_module.util.headerbar.HeaderBarViewModel

class WalletViewFragment : Fragment() {

    private lateinit var fragmentManager: FragmentManager
    private var _binding: FragmentWalletDetailsBinding? = null
    private val fragmentWalletDetailsBinding get() = _binding!!
    private lateinit var headerBarViewModel: HeaderBarViewModel
    private lateinit var headerBarView: HeaderBarView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWalletDetailsBinding.inflate(inflater, container, false)
        return fragmentWalletDetailsBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Get the child fragment manager for dynamic fragment transactions
        fragmentManager = childFragmentManager

        // Set default fragment
        setFragment(CardsFragment())

        // Tab click listeners
        fragmentWalletDetailsBinding.tabCards.setOnClickListener {
            updateTabSelection(true)
            setFragment(CardsFragment())
        }

        fragmentWalletDetailsBinding.tabAccounts.setOnClickListener {
            updateTabSelection(false)
            setFragment(AccountsFragment())
        }

        prepareHeaderBarData()
    }

    private fun prepareHeaderBarData() {
        headerBarViewModel = ViewModelProvider(this)[HeaderBarViewModel::class.java]
        headerBarView = fragmentWalletDetailsBinding.headerBarLayout

        // Bind ViewModel LiveData to the HeaderBarView
        headerBarViewModel.headerTitle.observe(viewLifecycleOwner, { title ->
            headerBarView.setHeaderTitle(title)
        })

        headerBarViewModel.leftIconResource.observe(viewLifecycleOwner) { iconResId ->
            headerBarView.setLeftIcon(iconResId)
        }

        headerBarViewModel.rightIconResource.observe(viewLifecycleOwner) { iconResId ->
            headerBarView.setRightIcon(iconResId)
        }

        headerBarViewModel.isLeftIconVisible.observe(viewLifecycleOwner) { isVisible ->
            headerBarView.setLeftIconVisibility(isVisible)
        }

        headerBarViewModel.isRightIconVisible.observe(viewLifecycleOwner) { isVisible ->
            headerBarView.setRightIconVisibility(isVisible)
        }

        // Example: Updating the header dynamically
        headerBarViewModel.setHeaderTitle(getString(R.string.text_string_wallet))
        headerBarViewModel.setLeftIconResource(R.drawable.ic_arrow_left_24)
        headerBarViewModel.setRightIconResource(R.drawable.ic_download)
        headerBarViewModel.setLeftIconVisibility(true)
        headerBarViewModel.setRightIconVisibility(true)

        // Handle icon clicks
        headerBarView.setOnLeftIconClickListener {
            onLeftIconClick()
        }

        headerBarView.setOnRightIconClickListener {
            onRightIconClick()
        }
    }

    private fun onLeftIconClick() {
        activity?.onBackPressed()
    }

    private fun onRightIconClick() {
        // Logic for right icon click
    }

    private fun updateTabSelection(isCardsTabSelected: Boolean) {
        val tabCards = fragmentWalletDetailsBinding.tabCards
        val tabAccounts = fragmentWalletDetailsBinding.tabAccounts
        if (isCardsTabSelected) {
            tabCards.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.tab_selected_background)
            tabCards.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
            tabAccounts.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.transparent))
            tabAccounts.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        } else {
            tabAccounts.background =
                ContextCompat.getDrawable(requireContext(), R.drawable.tab_selected_background)
            tabAccounts.setTextColor(
                ContextCompat.getColor(
                    requireContext(),
                    android.R.color.black
                )
            )
            tabCards.setBackgroundColor(
                ContextCompat.getColor(requireContext(), R.color.transparent))
            tabCards.setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
        }
    }

    private fun setFragment(fragment: Fragment) {
        fragmentManager.beginTransaction()
            .replace(R.id.dynamicContent, fragment)
            .commit()
    }
}
