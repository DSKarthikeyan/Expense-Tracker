package com.dsk.myexpense.expense_module.ui.view.statisticsdetails

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.FragmentStatisticsDetailsBinding
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.data.source.local.db.DailyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.db.MonthlyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.db.WeeklyExpenseSum
import com.dsk.myexpense.expense_module.ui.adapter.MyItemRecyclerViewAdapter
import com.dsk.myexpense.expense_module.ui.view.TransactionDetailsBottomView
import com.dsk.myexpense.expense_module.ui.viewmodel.AppLoadingViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.HomeDetailsViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.GenericViewModelFactory
import com.dsk.myexpense.expense_module.util.SwipeToDeleteCallback
import com.dsk.myexpense.expense_module.util.Utility
import com.dsk.myexpense.expense_module.util.Utility.isIncome
import com.dsk.myexpense.expense_module.util.headerbar.HeaderBarView
import com.dsk.myexpense.expense_module.util.headerbar.HeaderBarViewModel

class StatisticsDetailsFragment : Fragment(), MyItemRecyclerViewAdapter.ExpenseDetailClickListener {

    private val homeDetailsViewModel: HomeDetailsViewModel by viewModels {
        GenericViewModelFactory {
            HomeDetailsViewModel(
                requireContext(),
                ExpenseApplication.getExpenseRepository(requireContext()),
                ExpenseApplication.getCategoryRepository(requireContext()),
                ExpenseApplication.getCurrencyRepository(requireContext()),
                ExpenseApplication.getSettingsRepository(requireContext())
            )
        }
    }

    private val appLoadingViewModel: AppLoadingViewModel by viewModels {
        GenericViewModelFactory {
            AppLoadingViewModel(ExpenseApplication.getCategoryRepository(requireContext()),
                ExpenseApplication.getCurrencyRepository(requireContext()))
        }
    }

    private lateinit var adapter: MyItemRecyclerViewAdapter
    private lateinit var binding: FragmentStatisticsDetailsBinding
    private lateinit var headerBarViewModel: HeaderBarViewModel
    private lateinit var headerBarView: HeaderBarView

    private lateinit var selectedFilter: String

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentStatisticsDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
    }

    private fun prepareHeaderBarData() {
        headerBarViewModel = ViewModelProvider(this)[HeaderBarViewModel::class.java]
        headerBarView = binding.headerBarLayout

        headerBarViewModel.headerTitle.observe(viewLifecycleOwner, { title ->
            headerBarView.setHeaderTitle(title)
        })

        headerBarViewModel.leftIconResource.observe(viewLifecycleOwner, { iconResId ->
            headerBarView.setLeftIcon(iconResId)
        })

        headerBarViewModel.rightIconResource.observe(viewLifecycleOwner, { iconResId ->
            headerBarView.setRightIcon(iconResId)
        })

        headerBarViewModel.isLeftIconVisible.observe(viewLifecycleOwner, { isVisible ->
            headerBarView.setLeftIconVisibility(isVisible)
        })

        headerBarViewModel.isRightIconVisible.observe(viewLifecycleOwner, { isVisible ->
            headerBarView.setRightIconVisibility(isVisible)
        })

        headerBarViewModel.setHeaderTitle(getString(R.string.statistics))
        headerBarViewModel.setLeftIconResource(R.drawable.ic_arrow_left_24)
        headerBarViewModel.setRightIconResource(R.drawable.ic_download)
        headerBarViewModel.setLeftIconVisibility(true)
        headerBarViewModel.setRightIconVisibility(true)

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

    private fun initUI() {
        setupRecyclerView()
        setupDropdownFilter()
        setupObservers()
        updateChart(homeDetailsViewModel.getDailyExpenses()) { Utility.prepareDayChartData(it) }
        binding.toggleGroup.check(binding.dayButton.id)

        setupChartFilters()
        setupSortButton()
        prepareHeaderBarData()
    }

    private fun setupRecyclerView() {
        selectedFilter = resources.getString(R.string.text_all)
        binding.topSpendingRecycler.layoutManager = LinearLayoutManager(context)
        adapter = MyItemRecyclerViewAdapter(appLoadingViewModel, this)
        binding.topSpendingRecycler.adapter = adapter

        // Adding swipe to delete functionality
        val swipeCallback = SwipeToDeleteCallback(binding.topSpendingRecycler, homeDetailsViewModel) { deletedItem ->
            Log.d("DsK", "Deleted: ${deletedItem.expenseSenderName} successfully")
        }
        val itemTouchHelper = ItemTouchHelper(swipeCallback)
        itemTouchHelper.attachToRecyclerView(binding.topSpendingRecycler)
    }

    private fun setupObservers() {
        // Observe RecyclerView data
        homeDetailsViewModel.allExpenseDetails.observe(viewLifecycleOwner) { list ->
            binding.apply {
                if (list.isNullOrEmpty()) {
                    topSpendingRecycler.visibility = View.GONE
                    noDataRecyclerText.visibility = View.VISIBLE
                    dropdownFilter.visibility = View.GONE
                } else {
                    dropdownFilter.visibility = View.VISIBLE
                    topSpendingRecycler.visibility = View.VISIBLE
                    noDataRecyclerText.visibility = View.GONE
                    adapter.updateList(list)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun setupDropdownFilter() {
        val filterOptions = listOf(getString(R.string.text_all), getString(R.string.text_income), getString(R.string.text_expense))
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        binding.dropdownFilter.adapter = adapter
        binding.dropdownFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedFilter = filterOptions[position]
                filterChartData()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun filterChartData() {
        val rawData: List<Any> = when (binding.toggleGroup.checkedButtonId) {
            binding.dayButton.id -> homeDetailsViewModel.getDailyExpenses()
            binding.weekButton.id -> homeDetailsViewModel.getWeeklyExpenses()
            binding.monthButton.id -> homeDetailsViewModel.getMonthlyExpenses()
            binding.yearButton.id -> homeDetailsViewModel.getYearlyExpenses()
            else -> listOf()
        }

        val filteredData: List<Any> = when (selectedFilter) {
            getString(R.string.text_expense) -> rawData.filter { !it.isIncome() }
            getString(R.string.text_income) -> rawData.filter { it.isIncome() }
            else -> rawData
        }

        updateChart(filteredData) { data ->
            when (binding.toggleGroup.checkedButtonId) {
                binding.dayButton.id -> Utility.prepareDayChartData(data as List<DailyExpenseWithTime>)
                binding.weekButton.id -> Utility.prepareWeekChartData(data as List<WeeklyExpenseSum>)
                binding.monthButton.id -> Utility.convertDayToWeekOfMonth(data as List<WeeklyExpenseSum>)
                binding.yearButton.id -> Utility.prepareYearChartData(data as List<MonthlyExpenseWithTime>)
                else -> listOf()
            }
        }
    }

    private fun setupChartFilters() {
        binding.apply {
            dayButton.setOnClickListener {
                updateChart(homeDetailsViewModel.getDailyExpenses()) { Utility.prepareDayChartData(it) }
            }
            weekButton.setOnClickListener {
                updateChart(homeDetailsViewModel.getWeeklyExpenses()) { Utility.prepareWeekChartData(it) }
            }
            monthButton.setOnClickListener {
                updateChart(homeDetailsViewModel.getMonthlyExpenses()) { Utility.convertDayToWeekOfMonth(it) }
            }
            yearButton.setOnClickListener {
                updateChart(homeDetailsViewModel.getYearlyExpenses()) { Utility.prepareYearChartData(it) }
            }
        }
    }

    private fun <T> updateChart(data: List<T>, transform: (List<T>) -> List<Pair<String, Int>>) {
        binding.apply {
            val transformedData = transform(data)
            if (transformedData.isEmpty()) {
                lineChartView.visibility = View.GONE
            } else {
                lineChartView.visibility = View.VISIBLE
                lineChartView.setData(transformedData)
            }
        }
    }

    private fun setupSortButton() {
        binding.expenseTypeSort.setOnClickListener { showSortPopup(it) }
    }

    private fun showSortPopup(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.inflate(R.menu.sort_menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.sort_name -> adapter.sortBy { it.expenseSenderName }
                R.id.sort_amount -> adapter.sortByAmount { it.amount }
                R.id.sort_date -> adapter.sortBy { it.expenseAddedDate }
            }
            true
        }

        popupMenu.show()
    }

    override fun onItemClicked(expenseDetails: ExpenseDetails) {
        val transactionDetailsBottomSheet =
            context?.let { TransactionDetailsBottomView(it, expenseDetails) }
        transactionDetailsBottomSheet?.show(parentFragmentManager, "TransactionDetailsBottomSheet")
    }

    override fun onItemLongClicked(expenseDetails: ExpenseDetails) {
        // Handle item long-click
    }
}