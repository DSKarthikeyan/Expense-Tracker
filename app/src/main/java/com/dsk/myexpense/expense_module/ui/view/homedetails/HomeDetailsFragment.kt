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
        // Inflate the layout for this fragment
        fragmentHomeDetailsListBinding = FragmentHomeDetailsListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        initUI()
        observeLiveData()

        updateTime() // Set greeting message
    }

    private fun setupRecyclerView() {
        binding.rvTransactions.layoutManager = LinearLayoutManager(context)
        adapter = MyItemRecyclerViewAdapter(appLoadingViewModel, this)
        binding.rvTransactions.adapter = adapter

        // Adding swipe to delete functionality
        val swipeCallback = SwipeToDeleteCallback(binding.rvTransactions, homeDetailsViewModel) { deletedItem ->
            Log.d("DsK", "Deleted: ${deletedItem.expenseSenderName} successfully")
        }
        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(binding.rvTransactions)
    }

    private fun initUI() {
        // Fetch the currency symbol
        homeDetailsViewModel.fetchCurrencySymbol(requireContext())

        // Observe notification count
        checkForNotificationCount()

        NotificationListener.notificationCount.observe(viewLifecycleOwner) { count ->
            if (count == 0) {
                binding.notificationIcon.setImageResource(R.drawable.ic_notification_unfilled)
            } else {
                binding.notificationIcon.setImageResource(R.drawable.ic_notification_filled)
            }
        }

        // Handle "See All" click to show bottom sheet
        binding.textViewSeeAll.setOnClickListener {
            val allExpenses = homeDetailsViewModel.allExpenseDetails.value
            if (!allExpenses.isNullOrEmpty()) {
                val expenseDetailsBottomSheet = ExpenseHistoryFragment()
                expenseDetailsBottomSheet.show(parentFragmentManager, "ExpenseDetailsHistoryBottomSheet")
            }
        }
    }

    private fun observeLiveData() {
        // Observe combinedLiveData for updating UI totals
        homeDetailsViewModel.combinedLiveData.observe(viewLifecycleOwner) { (currencySymbol, amounts) ->
            val (income, expense, balance) = amounts
            binding.totalIncomeAmount.text = formatAmount(currencySymbol, income)
            binding.totalExpenseAmount.text = formatAmount(currencySymbol, expense)
            binding.tvTotalBalance.text = formatAmount(currencySymbol, balance)
        }

        // Observe allExpenseDetails to update RecyclerView
        homeDetailsViewModel.allExpenseDetails.observe(viewLifecycleOwner) { list ->
            list?.let {
                adapter.updateList(list)
                adapter.notifyDataSetChanged() // Ensure the RecyclerView refreshes
                Log.d("DsK", "RecyclerView updated with ${list.size} items")
            }
        }

        // Observe user details to update UI
        homeDetailsViewModel.userDetails.observe(viewLifecycleOwner) { user ->
            user?.let {
                updateUserDetails(user.name, Uri.parse(user.profilePicture))
            }
        }
    }

    private fun updateTime() {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (currentHour) {
            in 0..11 -> getString(R.string.greeting_morning)
            in 12..17 -> getString(R.string.greeting_afternoon)
            else -> getString(R.string.greeting_evening)
        }
        binding.tvGreeting.text = greeting
    }

    private fun formatAmount(currencySymbol: String, amount: Double?): String {
        val formattedAmount = String.format("%.2f", amount ?: 0.0)
        return "$currencySymbol $formattedAmount"
    }

    private fun updateUserDetails(name: String, profilePictureUri: Uri?) {
        binding.tvUserName.text = name.ifEmpty { getString(R.string.text_user_name) }
    }

    private fun checkForNotificationCount() {
        if (context?.let { NotificationUtils.hasNotificationPermission(it) } != true) {
            ActivityCompat.requestPermissions(
                requireActivity(), arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1
            )
        }
    }

    override fun onResume() {
        super.onResume()
        // Fetch user and expenses on resume
        homeDetailsViewModel.fetchUser()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        fragmentHomeDetailsListBinding = null
    }

    // Handle item click events
    override fun onItemClicked(expenseDetails: ExpenseDetails) {
        val transactionDetailsBottomSheet =
            context?.let { TransactionDetailsBottomView(it, expenseDetails) }
        transactionDetailsBottomSheet?.show(parentFragmentManager, "TransactionDetailsBottomSheet")
    }

    // Handle long click events
    override fun onItemLongClicked(expenseDetails: ExpenseDetails) {
        // Placeholder for future implementation
    }
}

