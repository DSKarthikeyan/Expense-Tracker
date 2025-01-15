package com.dsk.myexpense.expense_module.ui.view.settings

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.viewModels
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.FragmentSettingsBinding
import com.dsk.myexpense.expense_module.core.ExpenseApplication
import com.dsk.myexpense.expense_module.ui.viewmodel.AppLoadingViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.CategoryViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.GenericViewModelFactory
import com.dsk.myexpense.expense_module.ui.viewmodel.HomeDetailsViewModel
import com.dsk.myexpense.expense_module.util.CommonDialog
import com.dsk.myexpense.expense_module.util.Utility
import com.dsk.myexpense.expense_module.util.headerbar.HeaderBarView
import com.dsk.myexpense.expense_module.util.headerbar.HeaderBarViewModel
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    // Flag to check if the dialog is already shown
    private var isCategoryDialogOpen = false
    private val homeDetailsViewModel: HomeDetailsViewModel by viewModels {
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

    private val settingsViewModel: SettingsViewModel by viewModels {
        GenericViewModelFactory {
            SettingsViewModel(
                ExpenseApplication.getSettingsRepository(requireContext()),
                ExpenseApplication.getExpenseRepository(requireContext())
            )
        }
    }

    private val categoryViewModel: CategoryViewModel by viewModels {
        GenericViewModelFactory {
            CategoryViewModel(
                ExpenseApplication.getExpenseRepository(
                    requireContext()
                )
            )
        }
    }

    private lateinit var headerBarViewModel: HeaderBarViewModel
    private lateinit var headerBarView: HeaderBarView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
                    requireContext(), appLoadingViewModel
                ) { selectedCurrency, currencyValue ->
                    settingsViewModel.setDefaultCurrency(
                        requireContext(), selectedCurrency, currencyValue
                    )
                }
            }

        }
        isCategoryDialogOpen = false
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
            CommonDialog().showCategorySelectionDialog(requireContext(),
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
                })
        }

        observeViewModel()
        prepareHeaderBarData()
        binding.switchCloudUpload.setOnClickListener { authenticateUser() }
//        binding.btnUploadFile.setOnClickListener {
//            val fileContent = "{ \"data\": \"Your JSON content\" }"
//            uploadFileToDrive("expenses.json", fileContent)
//        }
//        binding.btnFetchFiles.setOnClickListener { fetchFilesFromDrive() }
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
        setupImportFormatSpinner()
        setupButtons()
        setupCloudSyncSwitch()
        observeViewModel()
    }

    private fun setupExportFormatSpinner() {
        val formats = listOf("CSV", "JSON")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, formats)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerExportFormat.adapter = adapter
    }

    private fun setupImportFormatSpinner() {
        val formats = listOf("CSV", "JSON")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, formats)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerImportFormat.adapter = adapter

        binding.textImportOptionsLabel.isEnabled = false
        binding.spinnerImportFormat.isEnabled = false
    }

    private fun setupButtons() {
        binding.btnExport.setOnClickListener { handleExport() }
        binding.btnImport.setOnClickListener {
//            handleImport(binding.spinnerImportFormat.selectedItem.toString())
        }
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
            Toast.makeText(requireContext(), "Cloud Sync: Not implemented", Toast.LENGTH_SHORT).show()
            // Add logic for cloud sync, e.g., Firebase or Google Drive integration.
        } else {
            Toast.makeText(requireContext(), "Cloud Sync Disabled", Toast.LENGTH_SHORT).show()
        }
    }
    private val googleSignInClient by lazy {
//        GoogleSignIn.getClient(
//            requireActivity(),
//            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
//                .requestScopes(DriveScopes.DRIVE_FILE)
//                .requestEmail()
//                .build()
//        )
    }

//    private var driveService: Drive? = null
    private val REQUEST_CODE_SIGN_IN = 100

    fun authenticateUser() {
//        val signInIntent = googleSignInClient.signInIntent
//        startActivityForResult(signInIntent, REQUEST_CODE_SIGN_IN)
    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//        if (requestCode == REQUEST_CODE_SIGN_IN) {
//            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
//            val account = task.getResult(ApiException::class.java)
//            if (account != null) {
//                initializeDriveService(account)
//            }
//        }
//    }

//    private fun initializeDriveService(account: GoogleSignInAccount) {
//        val credential = GoogleAccountCredential.usingOAuth2(
//            requireContext(), listOf(DriveScopes.DRIVE_FILE)
//        )
//        credential.selectedAccount = account.account
//
//        val transport = AndroidHttp.newCompatibleTransport()
//        val jsonFactory = GsonFactory.getDefaultInstance()
//
//        driveService = Drive.Builder(transport, jsonFactory, credential)
//            .setApplicationName("Expense Tracker")
//            .build()
//
//        Toast.makeText(requireContext(), "Google Drive service initialized.", Toast.LENGTH_SHORT).show()
//    }

    fun uploadFileToDrive(fileName: String, fileContent: String) {
//        if (driveService == null) {
//            Toast.makeText(requireContext(), "Google Drive service is not initialized.", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val fileMetadata = com.google.api.services.drive.model.File()
//        fileMetadata.name = fileName
//        fileMetadata.mimeType = "application/json"
//
//        val fileStream = ByteArrayContent("application/json", fileContent.toByteArray())
//
//        val request = driveService!!.files().create(fileMetadata, fileStream)
//        request.fields = "id"
//
//        val file = request.execute()
//
//        Toast.makeText(requireContext(), "File uploaded successfully: ${file.id}", Toast.LENGTH_SHORT).show()
    }

//    fun fetchFilesFromDrive() {
//        if (driveService == null) {
//            Toast.makeText(requireContext(), "Google Drive service is not initialized.", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        try {
//            val result = driveService!!.files().list()
//                .setQ("mimeType='application/json'") // Specify MIME type if needed
//                .setFields("files(id, name)")
//                .setPageSize(10)
//                .execute()
//
//            val files = result.files
//            if (files.isNullOrEmpty()) {
//                Toast.makeText(requireContext(), "No files found in Google Drive.", Toast.LENGTH_SHORT).show()
//            } else {
//                files.forEach { file ->
//                    Log.d("GoogleDrive", "File ID: ${file.id}, File Name: ${file.name}")
//                }
//            }
//        } catch (e: Exception) {
//            Toast.makeText(requireContext(), "Failed to fetch files: ${e.message}", Toast.LENGTH_SHORT).show()
//        }
//    }

    private fun exportToCsv() {
        val expenseDetails = homeDetailsViewModel.getAllExpenses()
        val categories = homeDetailsViewModel.getAllCategories()
        val currencies = homeDetailsViewModel.getAllCurrency()

        if (expenseDetails != null && categories != null && currencies != null) {
            Utility.exportToCsv(requireContext(), expenseDetails, categories, currencies)
            Toast.makeText(
                requireContext(), "Data exported to CSV successfully", Toast.LENGTH_SHORT
            ).show()
        } else {
            Log.d(
                "DsK",
                " CSV export expenseDetails ${expenseDetails?.size}, categories ${categories?.size} currencies ${currencies?.size}"
            )
            Toast.makeText(
                requireContext(),
                "Failed to fetch data for CSV  export expenseDetails ${expenseDetails?.size}, categories ${categories?.size} currencies ${currencies?.size}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun exportToJson() {
        val expenseDetails = homeDetailsViewModel.getAllExpenses()
        val categories = homeDetailsViewModel.getAllCategories()
        val currencies = homeDetailsViewModel.getAllCurrency()

        if (expenseDetails != null && categories != null && currencies != null) {
            Utility.exportToJson(requireContext(), expenseDetails, categories, currencies)
            Toast.makeText(
                requireContext(), "Data exported to JSON successfully", Toast.LENGTH_SHORT
            ).show()
        } else {
            Log.d(
                "DsK",
                " JSON export expenseDetails ${expenseDetails?.size}, categories ${categories?.size} currencies ${currencies?.size}"
            )
            Toast.makeText(
                requireContext(),
                "Failed to fetch data for JSON export expenseDetails ${expenseDetails?.size}, categories ${categories?.size} currencies ${currencies?.size}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun importFromCsv() {
        try {
            val (expenseDetails, categories, currencies) = Utility.importFromCsv(requireContext())
            if (expenseDetails != null && categories != null && currencies != null) {
                homeDetailsViewModel.insertExpenseDetails(expenseDetails)
                homeDetailsViewModel.insertAllCategory(categories)
                homeDetailsViewModel.insertAllCurrencies(currencies)
                Toast.makeText(
                    requireContext(), "Data imported from CSV successfully", Toast.LENGTH_SHORT
                ).show()
            } else {
                Log.d(
                    "DsK",
                    " CSV import expenseDetails ${expenseDetails?.size}, categories ${categories?.size} currencies ${currencies?.size}"
                )
                Toast.makeText(
                    requireContext(),
                    "Failed to fetch data for import for CSV expenseDetails ${expenseDetails?.size}, categories ${categories?.size} currencies ${currencies?.size}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(), "Failed to import from CSV: ${e.message}", Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun importFromJson() {
        try {
            val (expenseDetails, categories, currencies) = Utility.importFromJson(requireContext())
            if (expenseDetails != null && categories != null && currencies != null) {
                homeDetailsViewModel.insertExpenseDetails(expenseDetails)
                homeDetailsViewModel.insertAllCategory(categories)
                homeDetailsViewModel.insertAllCurrencies(currencies)
                Toast.makeText(
                    requireContext(), "Data imported from JSON successfully", Toast.LENGTH_SHORT
                ).show()
            } else {
                Log.d(
                    "DsK",
                    " JSON import expenseDetails ${expenseDetails.size}, categories ${categories.size} currencies ${currencies.size}"
                )
                Toast.makeText(
                    requireContext(),
                    "Failed to fetch data for import for JSON expenseDetails ${expenseDetails.size}, categories ${categories.size} currencies ${currencies.size}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } catch (e: Exception) {
            Toast.makeText(
                requireContext(), "Failed to import from JSON: ${e.message}", Toast.LENGTH_SHORT
            ).show()
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
                requireContext(), "USD", 1.0
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


