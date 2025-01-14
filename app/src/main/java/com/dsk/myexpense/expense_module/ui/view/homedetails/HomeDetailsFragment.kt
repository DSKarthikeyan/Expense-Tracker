package com.dsk.myexpense.expense_module.ui.view.homedetails

import android.Manifest
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.FragmentHomeDetailsListBinding
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.ui.NotificationListener
import com.dsk.myexpense.expense_module.ui.adapter.MyItemRecyclerViewAdapter
import com.dsk.myexpense.expense_module.ui.view.TransactionDetailsBottomView
import com.dsk.myexpense.expense_module.ui.viewmodel.AppLoadingViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.GenericViewModelFactory
import com.dsk.myexpense.expense_module.ui.viewmodel.HomeDetailsViewModel
import com.dsk.myexpense.expense_module.util.NotificationUtils
import com.dsk.myexpense.expense_module.util.SwipeToDeleteCallback


/**
 * A fragment representing a list of Items.
 */
class HomeDetailsFragment : Fragment(), MyItemRecyclerViewAdapter.ExpenseDetailClickListener {

    private var fragmentHomeDetailsListBinding: FragmentHomeDetailsListBinding? = null
    private val binding get() = fragmentHomeDetailsListBinding!!
    private val homeDetailsViewModel: HomeDetailsViewModel by viewModels {
        GenericViewModelFactory {
            HomeDetailsViewModel(
                requireContext(),
                (requireActivity().application as ExpenseApplication).expenseRepository,
                (requireActivity().application as ExpenseApplication).settingsRepository
            )
        }
    }
    private val appLoadingViewModel: AppLoadingViewModel by viewModels {
        GenericViewModelFactory {
            AppLoadingViewModel((requireActivity().application as ExpenseApplication).expenseRepository)
        }
    }
    private lateinit var adapter: MyItemRecyclerViewAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        fragmentHomeDetailsListBinding =
            FragmentHomeDetailsListBinding.inflate(inflater, container, false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvTransactions.layoutManager = LinearLayoutManager(context)

        initUI()

        homeDetailsViewModel.allExpenseDetails.observe(viewLifecycleOwner) { list ->
            list?.let {
                adapter.updateList(list)
            }
        }

        // Observe the combined LiveData to update the UI
        homeDetailsViewModel.combinedLiveData.observe(viewLifecycleOwner) { (currencySymbol, amounts) ->
            val (income, expense, balance) = amounts
            // Format the amount and update UI elements
            binding.totalIncomeAmount.text = formatAmount(currencySymbol, income)
            binding.totalExpenseAmount.text = formatAmount(currencySymbol, expense)
            binding.tvTotalBalance.text = formatAmount(currencySymbol, balance)
        }

        val swipeCallback =
            SwipeToDeleteCallback(binding.rvTransactions, homeDetailsViewModel) { deletedItem ->
                Log.d("DsK", "Deleted: ${deletedItem.expenseSenderName} SuccessFully")
            }

        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvTransactions)
    }

    // Helper function to format the amount with the currency symbol
    private fun formatAmount(currencySymbol: String, amount: Double?): String {
        val formattedAmount = String.format("%.2f", amount ?: 0.0)
        return "$currencySymbol $formattedAmount"
    }

    private fun initUI() {
        homeDetailsViewModel.fetchCurrencySymbol(requireContext())

        binding.rvTransactions.setHasFixedSize(true)

        // Adding item divider decoration
        binding.rvTransactions.layoutManager = LinearLayoutManager(context)
        adapter = MyItemRecyclerViewAdapter(requireContext(), appLoadingViewModel, this)
        binding.rvTransactions.adapter = adapter

        checkForNotificationCount()
        NotificationListener.notificationCount.observe(viewLifecycleOwner) { count ->
            if (count == 0) {
                binding.notificationIcon.setImageResource(R.drawable.ic_notification_unfilled)
            } else {
                binding.notificationIcon.setImageResource(R.drawable.ic_notification_filled)
            }
        }
        binding.textViewSeeAll.setOnClickListener {
            // Check if allExpenseDetails is not null and has items in it
            val allExpenses = homeDetailsViewModel.allExpenseDetails.value
            if (!allExpenses.isNullOrEmpty()) {
                val expenseDetailsBottomSheet = ExpenseDetailsFragment()
                expenseDetailsBottomSheet.show(
                    parentFragmentManager,  // Use this if you're inside a fragment
                    expenseDetailsBottomSheet.tag
                )
            }
        }
    }

    private fun checkForNotificationCount() {
        if (context?.let { NotificationUtils.hasNotificationPermission(it) } != true) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
            )
        }
    }

    internal operator fun Int.plus(other: Int?): Int {
        return this + (other ?: 0)
    }

    override fun onItemClicked(expenseDetails: ExpenseDetails) {
//        Toast.makeText(context, expenseDetails.expenseSenderName, Toast.LENGTH_SHORT).show()
        val transactionDetailsBottomSheet =
            context?.let { TransactionDetailsBottomView(it, expenseDetails) }
        transactionDetailsBottomSheet?.show(parentFragmentManager, "TransactionDetailsBottomSheet")
    }

    // Handle item long click to delete
    override fun onItemLongClicked(expenseDetails: ExpenseDetails) {
        // Here, handle the deletion of the item
//        viewModel.deleteExpenseDetails(expenseDetails)
        Toast.makeText(context, "Expense Deleted", Toast.LENGTH_SHORT).show()
    }
}
