package com.dsk.myexpense.expense_module.ui.view.homedetails

import android.Manifest
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
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
import kotlinx.coroutines.launch
import java.util.Calendar

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
        updateTime()
    }

    private fun updateTime() {
        // Get the current hour
        val calendar = Calendar.getInstance()
        val currentHour = calendar.get(Calendar.HOUR_OF_DAY)

        // Set the greeting message based on the current time
        val greeting = when (currentHour) {
            in 0..11 -> getString(R.string.greeting_morning)
            in 12..17 -> getString(R.string.greeting_afternoon)
            else -> getString(R.string.greeting_evening)
        }

        // Update the TextView text with the appropriate greeting
        binding.tvGreeting.text = greeting
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
        adapter = MyItemRecyclerViewAdapter(appLoadingViewModel, this)
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
                val expenseDetailsBottomSheet = ExpenseHistoryFragment()
                expenseDetailsBottomSheet.show(
                    parentFragmentManager,  // Use this if you're inside a fragment
                    "ExpenseDetailsHistoryBottomSheet"
                )
            }
        }

        homeDetailsViewModel.userDetails.observe(viewLifecycleOwner) { user ->
            // Handle the collected user data
            if (user == null) {
            } else {
                updateUserDetails(user.name, Uri.parse(user.profilePicture))
            }
        }
    }

    override fun onResume() {
        super.onResume()
        homeDetailsViewModel.fetchUser() // Fetch user from SharedPreferences
    }

    private fun updateUserDetails(name: String, profilePictureUri: Uri?) {
        binding.tvUserName.text = name.ifEmpty { resources.getString(R.string.text_user_name) }
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
    }
}
