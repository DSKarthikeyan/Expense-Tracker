package com.dsk.myexpense.expense_module.ui.view.statisticsdetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.PopupMenu
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.FragmentStatisticsDetailsBinding
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.data.source.local.DailyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.MonthlyExpenseWithTime
import com.dsk.myexpense.expense_module.data.source.local.WeeklyExpenseSum
import com.dsk.myexpense.expense_module.data.source.local.WeeklyExpenseWithTime
import com.dsk.myexpense.expense_module.ui.adapter.MyItemRecyclerViewAdapter
import com.dsk.myexpense.expense_module.ui.viewmodel.AppLoadingViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.HomeDetailsViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.GenericViewModelFactory
import com.dsk.myexpense.expense_module.util.headerbar.HeaderBarView
import com.dsk.myexpense.expense_module.util.headerbar.HeaderBarViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StatisticsDetailsFragment : Fragment(), MyItemRecyclerViewAdapter.ExpenseDetailClickListener {

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
    private lateinit var binding: FragmentStatisticsDetailsBinding
    private lateinit var headerBarViewModel: HeaderBarViewModel
    private lateinit var headerBarView: HeaderBarView

    private lateinit var selectedFilter: String // Default filter for dropdown

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
        setupDropdownFilter()
        setupObservers()
        updateChart(homeDetailsViewModel.getDailyExpenses()) { prepareDayChartData(it) }
        binding.toggleGroup.check(binding.dayButton.id)

        setupChartFilters()
        setupSortButton()
        prepareHeaderBarData()
    }

    private fun prepareHeaderBarData(){
        headerBarViewModel = ViewModelProvider(this)[HeaderBarViewModel::class.java]
        headerBarView = binding.headerBarLayout

        // Bind ViewModel LiveData to the HeaderBarView
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

        // Example: Updating the header dynamically
        headerBarViewModel.setHeaderTitle(getString(R.string.statistics))
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
    
    private fun initUI() {
        binding.apply {
            selectedFilter = resources.getString(R.string.text_all)
            // Initialize RecyclerView
            topSpendingRecycler.setHasFixedSize(true)
            topSpendingRecycler.layoutManager = LinearLayoutManager(context)
            adapter = MyItemRecyclerViewAdapter( appLoadingViewModel = appLoadingViewModel ,this@StatisticsDetailsFragment)
            topSpendingRecycler.adapter = adapter
        }
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
        // Fetch all data based on the selected toggle group button
        val rawData: List<Any> = when (binding.toggleGroup.checkedButtonId) {
            binding.dayButton.id -> homeDetailsViewModel.getDailyExpenses()
            binding.weekButton.id -> homeDetailsViewModel.getWeeklyExpenses()
            binding.monthButton.id -> homeDetailsViewModel.getMonthlyExpenses()
            binding.yearButton.id -> homeDetailsViewModel.getYearlyExpenses()
            else -> listOf()
        }

        // Filter data based on the dropdown selection
        val filteredData: List<Any> = when (selectedFilter) {
            getString(R.string.text_expense) -> rawData.filter { !it.isIncome() } // Only expenses
            getString(R.string.text_income) -> rawData.filter { it.isIncome() } // Only incomes
            else -> rawData // All data
        }

        // Update the chart based on the current toggle button selection
        updateChart(filteredData) { data ->
            when (binding.toggleGroup.checkedButtonId) {
                binding.dayButton.id -> prepareDayChartData(data as List<DailyExpenseWithTime>)
                binding.weekButton.id -> prepareWeekChartData(data as List<WeeklyExpenseSum>)
                binding.monthButton.id -> convertDayToWeekOfMonth(data as List<WeeklyExpenseSum>)
                binding.yearButton.id -> prepareYearChartData(data as List<MonthlyExpenseWithTime>)
                else -> listOf()
            }
        }
    }

    // Extension function to safely access `isExpense`
    private fun Any.isIncome(): Boolean {
        return when (this) {
            is DailyExpenseWithTime -> this.isIncome
            is WeeklyExpenseSum -> this.isIncome
            is WeeklyExpenseWithTime -> this.isIncome
            is MonthlyExpenseWithTime -> this.isIncome
            else -> throw IllegalArgumentException("Unknown data type: ${this::class.java.simpleName}")
        }
    }

    private fun setupChartFilters() {
        binding.apply {
            dayButton.setOnClickListener {
                updateChart(homeDetailsViewModel.getDailyExpenses()) { prepareDayChartData(it) }
            }
            weekButton.setOnClickListener {
                updateChart(homeDetailsViewModel.getWeeklyExpenses()) { prepareWeekChartData(it) }
            }
            monthButton.setOnClickListener {
                updateChart(homeDetailsViewModel.getMonthlyExpenses()) { convertDayToWeekOfMonth(it) }
            }
            yearButton.setOnClickListener {
                updateChart(homeDetailsViewModel.getYearlyExpenses()) { prepareYearChartData(it) }
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
        popupMenu.inflate(R.menu.sort_menu) // Create a menu resource file (sort_menu.xml)

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

    private fun prepareDayChartData(expenseData: List<DailyExpenseWithTime>): List<Pair<String, Int>> =
        expenseData.map { expense ->
            val formattedTime = SimpleDateFormat("hh a", Locale.getDefault()).format(Date(expense.time ?: 0L))
            formattedTime to (expense.amount ?: 0)
        }

    private fun prepareWeekChartData(expenseData: List<WeeklyExpenseSum>): List<Pair<String, Int>> =
        expenseData.map { expense ->
            val dayInMillis = expense.day ?: 0L
            val dateFormat = SimpleDateFormat("d", Locale.getDefault())

            // Convert milliseconds to Date and format them
            val dayFormatted = dateFormat.format(Date(dayInMillis))
            dayFormatted to (expense.sum ?: 0)
        }

    private fun prepareYearChartData(expenseData: List<MonthlyExpenseWithTime>): List<Pair<String, Int>> =
        expenseData.map { expense ->
            val formattedMonth = SimpleDateFormat("MMM yy", Locale.getDefault()).format(Date(expense.time ?: 0L))
            formattedMonth to (expense.amount ?: 0)
        }

    private fun convertDayToWeekOfMonth(data: List<WeeklyExpenseSum>): List<Pair<String, Int>> =
        data.map {
            val calendar = Calendar.getInstance().apply { timeInMillis = it.day ?: 0L }
            val weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH)
            val suffix = when (weekOfMonth) { 1 -> "st"; 2 -> "nd"; 3 -> "rd"; else -> "th" }
            "${weekOfMonth}${suffix} wk" to (it.sum ?: 0)
        }

    override fun onItemClicked(expenseDetails: ExpenseDetails) {
        // Handle item click
    }

    override fun onItemLongClicked(expenseDetails: ExpenseDetails) {
        // Handle item long-click
    }
}


