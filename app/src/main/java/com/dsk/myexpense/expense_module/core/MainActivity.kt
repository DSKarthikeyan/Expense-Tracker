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
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    // ViewModels
    private val smsViewModel: SmsViewModel by viewModels()
    private val appLoadingViewModel: AppLoadingViewModel by viewModels {
        GenericViewModelFactory { AppLoadingViewModel((application as ExpenseApplication).expenseRepository) }
    }

    // Binding and NavController
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var settingsDataStore: SettingsDataStore
    // Permissions and SMS Receiver
    private val permissionRequestCode = 101
    private lateinit var smsReceiver: SmsReceiver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the SettingsDataStore
        settingsDataStore = SettingsDataStore.getInstance(this)

        // Apply the dark mode setting
        applyDarkModeSetting()

        // Initialize binding and set content view
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);

        // Handle window insets for padding
        applyWindowInsets()

        // Setup navigation
        setupNavigation()

        // FAB action
        setupFabAction()

        // Observe necessary LiveData
        setupObservers()

        // Register SMS Receiver
        registerSmsReceiver()
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
        // Initialize the SMS Receiver
        smsReceiver = SmsReceiver()
        // Check and request permissions
        checkAndRequestPermissions()
        // Observe permission status
        smsViewModel.isPermissionGranted.observe(this) { granted ->
            if (!granted) {
                Toast.makeText(this, "Permissions not granted!", Toast.LENGTH_SHORT).show()
                checkAndRequestPermissions()
            } else {
                Log.d("MainActivity", "Permissions granted!")
            }
        }

        // Initialize categories via ViewModel
        appLoadingViewModel.initializeCategories(this)
        appLoadingViewModel.allCurrencies.observe(this) {
            if (it.isEmpty()) {
                appLoadingViewModel.fetchAndStoreCurrencies()
            }
        }
    }

    private fun registerSmsReceiver() {
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
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun checkAndRequestPermissions() {
        if (!smsViewModel.checkPermissions(this)) {
            val permissions = getRequiredPermissions()
            if (permissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this, permissions.toTypedArray(), permissionRequestCode
                )
            }
        } else {
            Log.d("MainActivity", "Permissions already granted")
        }
    }

    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()
        val permissionList = listOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
        )
        permissionList.forEach { permission ->
            if (ContextCompat.checkSelfPermission(
                    this, permission
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                permissions.add(permission)
            }
        }
        return permissions
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("MainActivity", "All permissions granted!")
            } else {
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
