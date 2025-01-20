package com.dsk.myexpense.expense_module.core

import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI.setupWithNavController
import com.dsk.myexpense.R
import com.dsk.myexpense.databinding.ActivityMainBinding
import com.dsk.myexpense.expense_module.util.SmsReceiver
import com.dsk.myexpense.expense_module.ui.viewmodel.AppLoadingViewModel
import com.dsk.myexpense.expense_module.ui.viewmodel.smshandler.SmsViewModel
import com.dsk.myexpense.expense_module.ui.view.AddNewExpenseActivity
import com.dsk.myexpense.expense_module.ui.view.settings.SettingsDataStore
import com.dsk.myexpense.expense_module.ui.viewmodel.GenericViewModelFactory
import com.dsk.myexpense.expense_module.util.CurrencyUtils
import kotlinx.coroutines.launch
import android.app.AlertDialog
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import com.dsk.myexpense.expense_module.data.model.ExpenseDetails
import com.dsk.myexpense.expense_module.data.model.ExpenseMessageDetails
import com.dsk.myexpense.expense_module.ui.viewmodel.HomeDetailsViewModel
import com.dsk.myexpense.expense_module.util.CommonDialog

class MainActivity : AppCompatActivity() {

    // ViewModels
    private val smsViewModel: SmsViewModel by viewModels()
    private val appLoadingViewModel: AppLoadingViewModel by viewModels {
        GenericViewModelFactory { AppLoadingViewModel((application as ExpenseApplication).expenseRepository) }
    }
    private val homeDetailsViewModel: HomeDetailsViewModel by viewModels {
        GenericViewModelFactory {
            HomeDetailsViewModel(
                this,
                ExpenseApplication.getExpenseRepository(this),
                ExpenseApplication.getSettingsRepository(this)
            )
        }
    }

    // Binding and Navigation
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var settingsDataStore: SettingsDataStore

    private var selectedImageUri: Uri? = null
    private lateinit var smsReceiver: SmsReceiver

    private val permissionRequestCode = 101
    private var shouldShowPermissionDialog = true

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                handleImageSelection(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize settings and UI
        settingsDataStore = SettingsDataStore.getInstance(this)
        applyDarkModeSetting()

        // Setup binding and navigation
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
        setupNavigation()

        // Handle window insets for padding
        applyWindowInsets()

        // Setup Floating Action Button
        setupFabAction()

        // Observe ViewModel data
        setupObservers()

        // Register SMS Receiver
        registerSmsReceiver()

        // Request necessary permissions
        if (!arePermissionsGranted()) {
            requestPermissions()
        }

        // Handle intent extras
        processIntentExtras()
    }

    private fun applyWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController
        setupWithNavController(binding.bottomNavigationView, navController)
    }

    private fun setupFabAction() {
        binding.iconFabAddExpense.setOnClickListener {
            val addNewExpenseActivity = AddNewExpenseActivity()
            addNewExpenseActivity.show(supportFragmentManager, "AddNewExpenseBottomSheet")
        }
    }

    private fun setupObservers() {
        smsViewModel.isPermissionGranted.observe(this) { granted ->
            if (!granted && shouldShowPermissionDialog) {
                showPermissionDialog()
            }
        }

        appLoadingViewModel.allCurrencies.observe(this) { currencies ->
            if (currencies.isEmpty()) {
                appLoadingViewModel.fetchAndStoreCurrencies(CurrencyUtils.loadCurrencyMapFromJSON(this))
            }
        }

        homeDetailsViewModel.userDetails.observe(this) { user ->
            if (user == null) {
                showUserSetupDialog()
            }
        }
    }

    private fun registerSmsReceiver() {
        smsReceiver = SmsReceiver()
        val intentFilter = IntentFilter("android.provider.Telephony.SMS_RECEIVED")
        registerReceiver(smsReceiver, intentFilter)
    }

    private fun applyDarkModeSetting() {
        lifecycleScope.launch {
            val isDarkModeEnabled = settingsDataStore.getDarkModeSetting()
            setDarkMode(isDarkModeEnabled)
        }
    }

    private fun setDarkMode(enabled: Boolean) {
        AppCompatDelegate.setDefaultNightMode(
            if (enabled) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun processIntentExtras() {
        val extras = intent.extras ?: return
        val messageDetails: ExpenseMessageDetails? = extras.getParcelable("messageDetails")
        val categoryName = extras.getString("categoryName")
        val type = extras.getString("type")

        if (messageDetails != null && !categoryName.isNullOrEmpty() && !type.isNullOrEmpty()) {
            lifecycleScope.launch {
                try {
                    val expenseDetails = messageDetails.toExpenseDetails(categoryName, type)
                    navigateToAddExpenseActivity(expenseDetails)
                } catch (e: Exception) {
                    Log.e("DsK", "Error processing Intent extras: ${e.message}", e)
                }
            }
        }
    }

    private fun navigateToAddExpenseActivity(expenseDetails: ExpenseDetails) {
        val intent = Intent(this, AddNewExpenseActivity::class.java).apply {
            putExtra("expenseDetails", expenseDetails)  // Passing the ExpenseDetails object
        }

        startActivity(intent)  // Launch the Activity
    }

    private suspend fun ExpenseMessageDetails.toExpenseDetails(
        categoryName: String,
        type: String
    ): ExpenseDetails {
        val category = homeDetailsViewModel.getExpenseCategoryDetails(categoryName, type)
        return ExpenseDetails(
            expenseSenderName = this.senderName ?: "Unknown",
            expenseMessageSenderName = this.expenseMessageSender ?: "Unknown",
            expenseReceiverName = this.receiverName ?: "Unknown",
            expenseDescription = this.additionalDetails ?: "No Description",
            amount = this.expenseAmount,
            isIncome = this.isIncome ?: false,
            categoryId = category.id,
            expenseAddedDate = this.expenseDate
        )
    }

    private fun arePermissionsGranted(): Boolean {
        val requiredPermissions = getRequiredPermissions()
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.CAMERA
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            permissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE)
        }
        return permissions
    }

    private fun requestPermissions() {
        val requiredPermissions = getRequiredPermissions().filter { permission ->
            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
        if (requiredPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this, requiredPermissions.toTypedArray(), permissionRequestCode
            )
        }
    }

    private fun showPermissionDialog() {
        AlertDialog.Builder(this).setTitle("Permissions Required")
            .setMessage("Permissions are needed for the app to function properly.")
            .setPositiveButton("Grant") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(this, "Permissions are required for proper functionality.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun handleImageSelection(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            selectedImageUri = uri
        } catch (e: SecurityException) {
            Log.e("DsK", "Failed to persist URI permission: ${e.localizedMessage}")
        }
    }

    private fun showUserSetupDialog() {
        CommonDialog().showUserDialog(
            context = this,
            pickImageLauncher = pickImageLauncher,
            onSave = { name, profilePictureUri, _ ->
                if (name.isNotEmpty() && profilePictureUri != null) {
                    homeDetailsViewModel.saveUser(name, profilePictureUri.toString())
                } else {
                    Toast.makeText(this, "Details not saved", Toast.LENGTH_SHORT).show()
                }
            },
            preSelectedImageUri = selectedImageUri
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
    }
}




