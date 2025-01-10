package com.dsk.myexpense.expense_module.ui.view.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.dsk.myexpense.databinding.FragmentSettingsBinding
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.ui.viewmodel.AppLoadingViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.GenericViewModelFactory
import com.dsk.myexpense.expense_module.util.CommonDialog

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val settingsRepository by lazy {
        (requireActivity().application as ExpenseApplication).settingsRepository
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        GenericViewModelFactory { SettingsViewModel(settingsRepository) }
    }

    private val appLoadingViewModel: AppLoadingViewModel by viewModels {
        GenericViewModelFactory {
            AppLoadingViewModel((requireActivity().application as ExpenseApplication).expenseRepository)
        }
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
