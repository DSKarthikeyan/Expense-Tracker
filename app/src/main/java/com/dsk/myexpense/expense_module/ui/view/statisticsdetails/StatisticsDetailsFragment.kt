package com.dsk.myexpense.expense_module.ui.view.statisticsdetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.Toast
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
import com.dsk.myexpense.expense_module.ui.adapter.MyItemRecyclerViewAdapter
import com.dsk.myexpense.expense_module.ui.viewmodel.AppLoadingViewModel
import com.dsk.myexpense.expense_module.util.CustomDividerItemDecoration
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
        GenericViewModelFactory { HomeDetailsViewModel(requireContext(),
            (requireActivity().application as ExpenseApplication).expenseRepository
            ,(requireActivity().application as ExpenseApplication).settingsRepository) }
    }
    private val appLoadingViewModel: AppLoadingViewModel by viewModels {
        GenericViewModelFactory {
            AppLoadingViewModel((requireActivity().application as ExpenseApplication).expenseRepository)
        }
    }
    private lateinit var adapter: MyItemRecyclerViewAdapter
    private lateinit var fragmentStatisticsDetailsFragment: FragmentStatisticsDetailsBinding
    private lateinit var headerBarViewModel: HeaderBarViewModel
    private lateinit var headerBarView: HeaderBarView
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        fragmentStatisticsDetailsFragment = FragmentStatisticsDetailsBinding.inflate(inflater, container, false)
        return fragmentStatisticsDetailsFragment.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initUI()
        setupObservers()
        updateChart(homeDetailsViewModel.getDailyExpenses()) { prepareDayChartData(it) }
        fragmentStatisticsDetailsFragment.toggleGroup.check(fragmentStatisticsDetailsFragment.dayButton.id)

        setupChartFilters()
        setupSortButton()
        prepareHeaderBarData()
    }

    private fun prepareHeaderBarData(){
        headerBarViewModel = ViewModelProvider(this)[HeaderBarViewModel::class.java]
        headerBarView = fragmentStatisticsDetailsFragment.headerBarLayout

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
        // Initialize RecyclerView
        fragmentStatisticsDetailsFragment.apply {
            topSpendingRecycler.setHasFixedSize(true)
            topSpendingRecycler.layoutManager = LinearLayoutManager(context)
            adapter = MyItemRecyclerViewAdapter(requireContext(), appLoadingViewModel ,this@StatisticsDetailsFragment)
            topSpendingRecycler.adapter = adapter
        }
    }

    private fun setupObservers() {
        // Observe RecyclerView data
        homeDetailsViewModel.allExpenseDetails.observe(viewLifecycleOwner) { list ->
            fragmentStatisticsDetailsFragment.apply {
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

    private fun setupChartFilters() {
        fragmentStatisticsDetailsFragment.apply {
            dayButton.setOnClickListener { updateChart(homeDetailsViewModel.getDailyExpenses()) { prepareDayChartData(it) } }
            weekButton.setOnClickListener { updateChart(homeDetailsViewModel.getWeeklyExpenses()) { prepareWeekChartData(it) } }
            monthButton.setOnClickListener { updateChart(homeDetailsViewModel.getMonthlyExpenses()) { convertDayToWeekOfMonth(it) } }
            yearButton.setOnClickListener { updateChart(homeDetailsViewModel.getYearlyExpenses()) { prepareYearChartData(it) } }
        }
    }

    private fun <T> updateChart(data: List<T>, transform: (List<T>) -> List<Pair<String, Int>>) {
        fragmentStatisticsDetailsFragment.apply {
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
        fragmentStatisticsDetailsFragment.expenseTypeSort.setOnClickListener { showSortPopup(it) }
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

    override fun onItemClicked(expenseDetails: ExpenseDetails) {
//        NA
    }

    override fun onItemLongClicked(expenseDetails: ExpenseDetails) {
        // Optional: Handle long click logic
    }

    private fun convertDayToWeekOfMonth(expenses: List<WeeklyExpenseSum>): List<Pair<String, Int>> {
        val resultList = mutableListOf<Pair<String, Int>>()

        expenses.forEach { expense ->
            val calendar = Calendar.getInstance().apply {
                timeInMillis = expense.day ?: 0L
            }

            // Get the week of the month (1-based index)
            val weekOfMonth = calendar.get(Calendar.WEEK_OF_MONTH)

            // Determine the ordinal suffix for the week number
            val suffix = when (weekOfMonth) {
                1 -> "st"
                2 -> "nd"
                3 -> "rd"
                else -> "th"
            }

            // Format week as "Xth w", "Xrd w", etc.
            val formattedWeek = "${weekOfMonth}${suffix} wk"

            // Add to result list with formatted week and the sum
            resultList.add(Pair(formattedWeek, expense.sum ?: 0))
        }

        return resultList
    }
}

