package com.dsk.myexpense.expense_module.ui.view.settings

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.FragmentSettingsBinding
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.data.model.Currency
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.ui.viewmodel.AppLoadingViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.GenericViewModelFactory
import com.dsk.myexpense.expense_module.ui.viewmodel.CategoryViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.HomeDetailsViewModel
import com.dsk.myexpense.expense_module.util.CommonDialog
import com.dsk.myexpense.expense_module.util.Utility
import com.dsk.myexpense.expense_module.util.headerbar.HeaderBarView
import com.dsk.myexpense.expense_module.util.headerbar.HeaderBarViewModel

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // Flag to check if the dialog is already shown
    private var isCategoryDialogOpen = false
    private val settingsRepository by lazy {
        (requireActivity().application as ExpenseApplication).settingsRepository
    }

    private val expenseRepository by lazy {
        (requireActivity().application as ExpenseApplication).expenseRepository
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        GenericViewModelFactory { SettingsViewModel(settingsRepository, expenseRepository) }
    }

    private val appLoadingViewModel: AppLoadingViewModel by viewModels {
        GenericViewModelFactory {
            AppLoadingViewModel(expenseRepository)
        }
    }

    private val homeDetailsViewModel: HomeDetailsViewModel by viewModels {
        GenericViewModelFactory {
            HomeDetailsViewModel(
                requireContext(),
                (requireActivity().application as ExpenseApplication).expenseRepository,
                (requireActivity().application as ExpenseApplication).settingsRepository
            )
        }
    }

    private val categoryViewModel: CategoryViewModel by viewModels {
        GenericViewModelFactory { CategoryViewModel(expenseRepository) }
    }
    private lateinit var headerBarViewModel: HeaderBarViewModel
    private lateinit var headerBarView: HeaderBarView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize default settings on first app launch
        initializeDefaultSettings()

        binding.apply {
            switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
                settingsViewModel.setDarkModeEnabled(isChecked)
            }

            currencyLayout.setOnClickListener {
                CommonDialog().showCurrencySelectionDialog(
                    requireContext(),
                    appLoadingViewModel
                ) { selectedCurrency, currencyValue ->
                    settingsViewModel.setDefaultCurrency(
                        requireContext(),
                        selectedCurrency,
                        currencyValue
                    )
                }
            }

        }

        // Category selection (no text display)
        binding.iconCategory.setOnClickListener {
            // Prevent multiple clicks while the dialog is open
            if (isCategoryDialogOpen) {
                Log.d("SettingsFragment", "Category dialog already open, ignoring click.")
                return@setOnClickListener
            }

            Log.d("SettingsFragment", "categoryLayout: clicked")

            // Set the flag to indicate the dialog is open
            isCategoryDialogOpen = true

            // Show the category selection dialog
            CommonDialog().showCategorySelectionDialog(
                requireContext(),
                categoryViewModel,
                onCategorySelected = { selectedCategory ->
                    // Handle the selected category
                    selectedCategory?.let {
                        // Save the selected category to the ViewModel and DB
                        settingsViewModel.setSelectedCategory(it)
                    }
                },
                onDismissDialog = { isCategorySelected ->
                    // Reset the flag after the dialog is dismissed
                    isCategoryDialogOpen = false

                    // Optionally, log or perform actions based on whether a category was selected
                    if (isCategorySelected == true) {
                        Log.d("SettingsFragment", "Category selected successfully.")
                    } else {
                        Log.d(
                            "SettingsFragment",
                            "Category selection was canceled or no category selected."
                        )
                    }
                }
            )
        }

        observeViewModel()
        prepareHeaderBarData()
    }

    private fun prepareHeaderBarData() {
        headerBarViewModel = ViewModelProvider(this)[HeaderBarViewModel::class.java]
        headerBarView = binding.headerBarLayout

        // Bind ViewModel LiveData to the HeaderBarView
        headerBarViewModel.headerTitle.observe(viewLifecycleOwner) { title ->
            headerBarView.setHeaderTitle(title)
        }

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
        headerBarViewModel.setHeaderTitle(getString(R.string.text_settings))
        headerBarViewModel.setLeftIconResource(R.drawable.ic_arrow_left_24)
        headerBarViewModel.setLeftIconVisibility(true)
        headerBarViewModel.setRightIconVisibility(true)

        // Handle icon clicks
        headerBarView.setOnLeftIconClickListener {
            onLeftIconClick()
        }

        headerBarView.setOnRightIconClickListener {
            onRightIconClick()
        }
        setupExportFormatSpinner()
        setupButtons()
        setupCloudSyncSwitch()
    }

    private fun setupExportFormatSpinner() {
        val formats = listOf("CSV", "JSON")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, formats)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerExportFormat.adapter = adapter

        binding.spinnerExportFormat.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    Toast.makeText(
                        requireContext(),
                        "Selected format: ${formats[position]}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {
                    // No action needed
                }
            }
    }

    private fun setupButtons() {
        binding.btnExport.setOnClickListener {
            handleExport()
        }

//        binding.btnImport.setOnClickListener {
//            val format = binding.spinnerExportFormat.selectedItem.toString()
//            handleImport(format)
//        }
    }

    private fun setupCloudSyncSwitch() {
        binding.switchCloudUpload.setOnCheckedChangeListener { _, isChecked ->
            handleCloudSync(isChecked)
        }
    }

    private fun handleExport() {
        val format = binding.spinnerExportFormat.selectedItem.toString()
        when (format) {
            "CSV" -> exportToCsv()
            "JSON" -> exportToJson()
            else -> Toast.makeText(requireContext(), "Unsupported format", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun handleImport(format: String) {
        when (format) {
            "CSV" -> importFromCsv()
            "JSON" -> importFromJson()
            else -> Toast.makeText(requireContext(), "Unsupported format", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private fun handleCloudSync(isEnabled: Boolean) {
        if (isEnabled) {
            Toast.makeText(requireContext(), "Cloud Sync Enabled", Toast.LENGTH_SHORT).show()
            // Logic to enable cloud sync
        } else {
            Toast.makeText(requireContext(), "Cloud Sync Disabled", Toast.LENGTH_SHORT).show()
            // Logic to disable cloud sync
        }
    }

    private fun exportToCsv() {
        // Get data from ViewModel
        val expenseDetails = homeDetailsViewModel.allExpenseDetails?.value
        val categories = homeDetailsViewModel.getAllCategories()?.value
        val currencies = listOf<Currency>() // If applicable, populate with real data

        if (expenseDetails != null && categories != null) {
            Utility.exportToCsv(requireContext(), expenseDetails, categories, currencies)
            Toast.makeText(requireContext(), "Data exported to CSV successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Failed to fetch data for export", Toast.LENGTH_SHORT).show()
        }
    }

    private fun exportToJson() {
        // Get data from ViewModel
        val expenseDetails = homeDetailsViewModel.allExpenseDetails?.value
        val categories = homeDetailsViewModel.getAllCategories()?.value
        val currencies = listOf<Currency>() // If applicable, populate with real data

        if (expenseDetails != null && categories != null) {
            Utility.exportToJson(requireContext(), expenseDetails, categories, currencies)
            Toast.makeText(requireContext(), "Data exported to JSON successfully", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "Failed to fetch data for export", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importFromCsv() {
        try {
            val (expenseDetails, categories, currencies) = Utility.importFromCsv(requireContext())
            // Update ViewModel or use the imported data as needed
//            homeDetailsViewModel.updateExpense(requireContext(),expenseDetails, null, "")
//            homeDetailsViewModel.updateCategories(categories)

            Toast.makeText(requireContext(), "Data imported from CSV successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to import data from CSV: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importFromJson() {
        try {
            val (expenseDetails, categories, currencies) = Utility.importFromJson(requireContext())
            // Update ViewModel or use the imported data as needed
//            homeDetailsViewModel.updateExpenseDetails(expenseDetails)
//            homeDetailsViewModel.updateCategories(categories)

            Toast.makeText(requireContext(), "Data imported from JSON successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Failed to import data from JSON: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }



    private fun onLeftIconClick() {
        activity?.onBackPressed()
    }

    private fun onRightIconClick() {
        // Logic for right icon click
    }

    private fun initializeDefaultSettings() {
        val sharedPreferences =
            requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        // Check if it's the first launch
        val isFirstLaunch = sharedPreferences.getBoolean("is_first_launch", true)
        if (isFirstLaunch) {
            // Assign default settings values
            settingsViewModel.setDarkModeEnabled(false) // Default to light mode
            settingsViewModel.setDefaultCurrency(
                requireContext(),
                "USD",
                1.0
            ) // Default currency: USD

            // Mark the first launch as complete
            sharedPreferences.edit().putBoolean("is_first_launch", false).apply()
        }
    }

    private fun observeViewModel() {
        settingsViewModel.darkModeEnabled.observe(viewLifecycleOwner) { isEnabled ->
            binding.switchDarkMode.isChecked = isEnabled
            setDarkMode(isEnabled)
        }

        settingsViewModel.defaultCurrency.observe(viewLifecycleOwner) { currency ->
            binding.textDefaultCurrency.text = currency
        }
    }

    private fun setDarkMode(enabled: Boolean) {
        val mode =
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

