package com.dsk.myexpense.expense_module.ui.view.settings

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.dsk.myexpense.databinding.FragmentSettingsBinding
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.data.model.Category
import com.dsk.myexpense.expense_module.ui.viewmodel.AppLoadingViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.GenericViewModelFactory
import com.dsk.myexpense.expense_module.ui.viewmodel.CategoryViewModel
import com.dsk.myexpense.expense_module.util.CommonDialog

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

    private val categoryViewModel: CategoryViewModel by viewModels {
        GenericViewModelFactory { CategoryViewModel(expenseRepository) }
    }

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
                CommonDialog().showCurrencySelectionDialog(requireContext(), appLoadingViewModel) { selectedCurrency, currencyValue ->
                    settingsViewModel.setDefaultCurrency(requireContext(), selectedCurrency, currencyValue)
                }
            }

            iconBack.setOnClickListener {
                activity?.onBackPressed()
            }
        }

        // Category selection (no text display)
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
                        Log.d("SettingsFragment", "Category selection was canceled or no category selected.")
                    }
                }
            )

        }

        observeViewModel()
    }

    private fun initializeDefaultSettings() {
        val sharedPreferences = requireContext().getSharedPreferences("app_preferences", Context.MODE_PRIVATE)

        // Check if it's the first launch
        val isFirstLaunch = sharedPreferences.getBoolean("is_first_launch", true)
        if (isFirstLaunch) {
            // Assign default settings values
            settingsViewModel.setDarkModeEnabled(false) // Default to light mode
            settingsViewModel.setDefaultCurrency(requireContext(), "USD", 1.0) // Default currency: USD

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
        val mode = if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

