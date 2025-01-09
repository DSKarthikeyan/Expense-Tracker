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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ItemTouchHelper
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.FragmentHomeDetailsListBinding
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.util.NotificationUtils
import com.dsk.myexpense.expense_module.util.SwipeToDeleteCallback
import com.dsk.myexpense.expense_module.ui.NotificationListener
import com.dsk.myexpense.expense_module.ui.view.TransactionDetailsBottomView
import com.dsk.myexpense.expense_module.ui.adapter.MyItemRecyclerViewAdapter
import com.dsk.myexpense.expense_module.ui.viewmodel.AppLoadingViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.GenericViewModelFactory
import com.dsk.myexpense.expense_module.ui.viewmodel.HomeDetailsViewModel

/**
 * A fragment representing a list of Items.
 */
class HomeDetailsFragment : Fragment(), MyItemRecyclerViewAdapter.ExpenseDetailClickListener {

    private var fragmentHomeDetailsListBinding: FragmentHomeDetailsListBinding? = null
    private val binding get() = fragmentHomeDetailsListBinding!!
    private val homeDetailsViewModel: HomeDetailsViewModel by viewModels {
        GenericViewModelFactory { HomeDetailsViewModel((requireActivity().application as ExpenseApplication).expenseRepository) }
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

        homeDetailsViewModel.getTotalIncomeAmount.observe(viewLifecycleOwner) {
            binding.totalIncomeAmount.text = it?.toString() ?: "0"
        }

        homeDetailsViewModel.getTotalExpenseAmount.observe(viewLifecycleOwner) {
            binding.totalExpenseAmount.text = it?.toString() ?: "0"
        }

        homeDetailsViewModel.getTotalIncomeExpenseAmount.observe(viewLifecycleOwner) {
            binding.tvTotalBalance.text = it?.toString() ?: "0"
        }

        val swipeCallback =
            SwipeToDeleteCallback(binding.rvTransactions, homeDetailsViewModel) { deletedItem ->
                Log.d("DsK", "Deleted: ${deletedItem.expenseSenderName} SuccessFully")
            }
        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvTransactions)
    }

    private fun initUI() {
        binding.rvTransactions.setHasFixedSize(true)

        // Adding item divider decoration
        binding.rvTransactions.layoutManager = LinearLayoutManager(context)
        adapter = MyItemRecyclerViewAdapter(requireContext(), appLoadingViewModel, this)
        binding.rvTransactions.adapter = adapter

        checkForNotificationCount()
        NotificationListener.notificationCount.observe(viewLifecycleOwner) { count ->
            Log.d("DsK", "unreadNotificationCount $count")
            if (count == 0) {
                binding.notificationIcon.setImageResource(R.drawable.ic_notification_unfilled)
            } else {
                binding.notificationIcon.setImageResource(R.drawable.ic_notification_filled)
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
