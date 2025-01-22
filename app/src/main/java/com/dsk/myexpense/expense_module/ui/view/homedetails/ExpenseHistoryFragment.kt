package com.dsk.myexpense.expense_module.ui.view.homedetails

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.ActivityExpenseHistoryBinding
import com.dsk.myexpense.databinding.ContentExpenseHistoryBinding
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.ui.adapter.MyItemRecyclerViewAdapter
import com.dsk.myexpense.expense_module.ui.view.TransactionDetailsBottomView
import com.dsk.myexpense.expense_module.ui.viewmodel.AppLoadingViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.GenericViewModelFactory
import com.dsk.myexpense.expense_module.ui.viewmodel.HomeDetailsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.util.Calendar

class ExpenseHistoryFragment : BottomSheetDialogFragment(),
    MyItemRecyclerViewAdapter.ExpenseDetailClickListener {

    private lateinit var expenseHistoryBinding: ActivityExpenseHistoryBinding
    private lateinit var expenseAdapter: MyItemRecyclerViewAdapter
    private lateinit var filterAdapter: ArrayAdapter<String>
    private lateinit var binding: ContentExpenseHistoryBinding

    private val viewModel: HomeDetailsViewModel by viewModels() {
        GenericViewModelFactory {
            HomeDetailsViewModel(
                requireContext(),
                ExpenseApplication.getExpenseRepository(requireContext()),
                ExpenseApplication.getSettingsRepository(requireContext())
            )
        }
    }
    private val appLoadingViewModel: AppLoadingViewModel by viewModels {
        GenericViewModelFactory {
            AppLoadingViewModel(ExpenseApplication.getExpenseRepository(requireContext()))
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        expenseHistoryBinding = ActivityExpenseHistoryBinding.inflate(inflater, container, false)
        return expenseHistoryBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = expenseHistoryBinding.expenseHistory
        setupSpinner()
        setupRecyclerView()

        observeExpenses()
        applyCategoryFilter()
    }

    private fun setupSpinner() {
        val filterOptions = resources.getStringArray(R.array.filter_options_expense)
        filterAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDateFilter.adapter = filterAdapter

        binding.spinnerDateFilter.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parentView: AdapterView<*>, view: View?, position: Int, id: Long
                ) {
                    val selectedOption = filterOptions[position]
                    handleFilterSelection(selectedOption)
                }

                override fun onNothingSelected(parentView: AdapterView<*>) {}
            }

        // Add item selection listener to category filter spinner
        binding.spinnerCategoryFilter.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parentView: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedCategory = binding.spinnerCategoryFilter.selectedItem as String
                    handleCategorySelection(selectedCategory)
                }

                override fun onNothingSelected(parentView: AdapterView<*>) {}
            }
    }

    private fun handleFilterSelection(filter: String) {
        when (filter) {
            getString(R.string.month) -> applyMonthFilter()
            getString(R.string.day) -> applyDayFilter()
            getString(R.string.year) -> applyYearFilter()
        }
    }

    private fun applyMonthFilter() {
        val calendar = Calendar.getInstance()
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)
        val startDate = Calendar.getInstance().apply {
            set(year, month, 1) // Set to the first day of the current month
        }.timeInMillis

        val endDate = Calendar.getInstance().apply {
            set(
                year, month, getActualMaximum(Calendar.DAY_OF_MONTH)
            ) // Set to the last day of the current month
        }.timeInMillis

        filterExpenses(
            startDate,
            endDate,
            null
        ) // Pass the start and end date for the current month
    }

    private fun applyDayFilter() {
        val calendar = Calendar.getInstance()
        val dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH)
        val month = calendar.get(Calendar.MONTH)
        val year = calendar.get(Calendar.YEAR)

        val selectedDate = Calendar.getInstance().apply {
            set(year, month, dayOfMonth) // Set to today's date
        }.timeInMillis

        filterExpenses(selectedDate, selectedDate, null) // Filter expenses for today's date
    }

    private fun applyYearFilter() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val startDate = Calendar.getInstance().apply {
            set(year, 0, 1) // Set to the first day of the current year
        }.timeInMillis

        val endDate = Calendar.getInstance().apply {
            set(year, 11, 31) // Set to the last day of the current year
        }.timeInMillis

        filterExpenses(startDate, endDate, null) // Pass the start and end date for the current year
    }

    override fun onStart() {
        super.onStart()
        dialog?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)?.apply {
            layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        }
    }

    private fun setupRecyclerView() {
        expenseAdapter = MyItemRecyclerViewAdapter(
            appLoadingViewModel = appLoadingViewModel,
            this@ExpenseHistoryFragment
        )
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = expenseAdapter
        }
    }

    private fun observeExpenses() {
        viewModel.allExpenseDetails.observe(viewLifecycleOwner) { expenses ->
            expenseAdapter.updateList(expenses)
        }
    }

    // Filter the expenses based on the date range and category
    private fun filterExpenses(startDate: Long?, endDate: Long?, categoryId: Int?) {
        viewModel.allExpenseDetails.observe(viewLifecycleOwner) { expenses ->
            var filteredExpenses = expenses

            // Filter by date range if specified
            if (startDate != null && endDate != null) {
                filteredExpenses = filteredExpenses.filter {
                    it.expenseAddedDate in startDate..endDate
                }
            }

            // Filter by category if specified
            if (categoryId != null) {
                filteredExpenses = filteredExpenses.filter {
                    it.categoryId == categoryId
                }
            }

            expenseAdapter.updateList(filteredExpenses)
        }
    }

    // Apply category filter based on category selection from spinner
    private fun handleCategorySelection(selectedCategory: String) {
        if (selectedCategory == getString(R.string.text_all_categories)) {
            filterExpenses(null, null, null) // Show all expenses if "All Categories" is selected
        } else {
            viewModel.getAllCategoriesLiveData().observe(viewLifecycleOwner) { categories ->
                val selectedCategoryId = categories.find { it.name == selectedCategory }?.id
                filterExpenses(null, null, selectedCategoryId)
            }
        }
    }

    private fun applyCategoryFilter() {
        // Apply category filter logic here
        viewModel.getAllCategoriesLiveData().observe(viewLifecycleOwner) { categories ->
            val categoryNames = mutableListOf(getString(R.string.text_all_categories)) // Add "All Categories" as default
            categoryNames.addAll(categories.map { it.name })
            val categoryAdapter =
                ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategoryFilter.adapter = categoryAdapter
            // Set the default selection to "All Categories"
            binding.spinnerCategoryFilter.setSelection(0)
        }
    }

    override fun onItemClicked(expenseDetails: ExpenseDetails) {
        val transactionDetailsBottomSheet =
            context?.let { TransactionDetailsBottomView(it, expenseDetails) }
        transactionDetailsBottomSheet?.show(parentFragmentManager, "TransactionDetailsBottomSheet")
    }

    override fun onItemLongClicked(expenseDetails: ExpenseDetails) {
    }
}


