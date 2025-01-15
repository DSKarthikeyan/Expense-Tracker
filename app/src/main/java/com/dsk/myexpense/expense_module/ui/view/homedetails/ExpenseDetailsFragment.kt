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
import com.dsk.myexpense.databinding.FragmentExpenseDetailsBinding
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.ui.adapter.ExpenseAdapter
import com.dsk.myexpense.expense_module.ui.viewmodel.GenericViewModelFactory
import com.dsk.myexpense.expense_module.ui.viewmodel.HomeDetailsViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.filter
import java.util.Calendar

class ExpenseDetailsFragment : BottomSheetDialogFragment() {

    private lateinit var binding: FragmentExpenseDetailsBinding
    private val viewModel: HomeDetailsViewModel by viewModels() {
        GenericViewModelFactory {
            HomeDetailsViewModel(
                requireContext(),
                (requireActivity().application as ExpenseApplication).expenseRepository,
                (requireActivity().application as ExpenseApplication).settingsRepository
            )
        }
    }
    private lateinit var expenseAdapter: ExpenseAdapter
    private lateinit var filterAdapter: ArrayAdapter<String>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentExpenseDetailsBinding.inflate(inflater, container, false)

        setupSpinner()
        setupRecyclerView()

        observeExpenses()
        applyCategoryFilter()

        return binding.root
    }

    override fun onStart() {
        super.onStart()

        // Make the bottom sheet full-screen
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            (resources.displayMetrics.heightPixels * 0.8).toInt() // Set height to 80% of screen height
        )
    }

    private fun setupSpinner() {
        val filterOptions = resources.getStringArray(R.array.filter_options_expense)
        filterAdapter =
            ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDateFilter.adapter = filterAdapter

        binding.spinnerDateFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parentView: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                val selectedOption = filterOptions[position]
                handleFilterSelection(selectedOption)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {}
        }

        // Add item selection listener to category filter spinner
        binding.spinnerCategoryFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedCategory = binding.spinnerCategoryFilter.selectedItem as String
                handleCategorySelection(selectedCategory)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {}
        }
    }

    private fun handleFilterSelection(filter: String) {
        when (filter) {
            "Month" -> applyMonthFilter()
            "Day" -> applyDayFilter()
            "Year" -> applyYearFilter()
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

        filterExpenses(startDate, endDate, null) // Pass the start and end date for the current month
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

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter()
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = expenseAdapter
        }
    }

    private fun observeExpenses() {
        viewModel.getAllExpensesLiveData().observe(viewLifecycleOwner) { expenses ->
            expenseAdapter.submitList(expenses)
        }
    }

    // Filter the expenses based on the date range and category
    private fun filterExpenses(startDate: Long?, endDate: Long?, categoryId: Int?) {
        viewModel.getAllExpensesLiveData().observe(viewLifecycleOwner) { expenses ->
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

            expenseAdapter.submitList(filteredExpenses)
        }
    }

    // Apply category filter based on category selection from spinner
    private fun handleCategorySelection(selectedCategory: String) {
        if (selectedCategory == "All Categories") {
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
            val categoryNames = mutableListOf("All Categories") // Add "All Categories" as default
            categoryNames.addAll(categories.map { it.name })
            val categoryAdapter =
                ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, categoryNames)
            categoryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            binding.spinnerCategoryFilter.adapter = categoryAdapter
            // Set the default selection to "All Categories"
            binding.spinnerCategoryFilter.setSelection(0)
        }
    }
}


