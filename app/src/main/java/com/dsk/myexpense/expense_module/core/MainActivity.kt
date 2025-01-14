package com.dsk.myexpense.expense_module.core

import android.Manifest
import android.content.Context
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
import android.content.Intent
import android.net.Uri
import android.provider.Settings

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
    private var shouldShowPermissionDialog = true // Flag to control dialog behavior

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the SettingsDataStore
        settingsDataStore = SettingsDataStore.getInstance(this)

        // Apply the dark mode setting
        applyDarkModeSetting()

        // Initialize binding and set content view
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)

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

        // Check and request permissions
        if (!arePermissionsGranted()) {
            requestPermissions()
        }
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
        // Observe permission status
        smsViewModel.isPermissionGranted.observe(this) { granted ->
            if (granted) {
                Log.d("MainActivity", "All permissions granted.")
            } else {
                val deniedPermissions = getRequiredPermissions().filter { permission ->
                    ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
                }
                if (shouldShowPermissionDialog && deniedPermissions.isNotEmpty()) {
                    showPermissionDialog(deniedPermissions)
                }
            }
        }

        // Initialize categories via ViewModel
        appLoadingViewModel.initializeCategories(this)

        appLoadingViewModel.allCurrencies.observe(this) {
            if (it.isEmpty()) {
                appLoadingViewModel.fetchAndStoreCurrencies(CurrencyUtils.loadCurrencyMapFromJSON(this))
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

    private fun arePermissionsGranted(): Boolean {
        val requiredPermissions = getRequiredPermissions()
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf<String>()

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13 and above
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES) // Access to images
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)  // Access to videos
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)  // Access to audio
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11 and above
            permissions.add(Manifest.permission.MANAGE_EXTERNAL_STORAGE) // Broad external storage access
        } else {
            // Below Android 11
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        // Common permissions for all versions
        permissions.addAll(
            listOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CAMERA
            )
        )

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

    private fun showPermissionDialog(deniedPermissions: List<String>) {
        val permissionDescriptions = deniedPermissions.map { permission ->
            when (permission) {
                Manifest.permission.RECEIVE_SMS -> "Receive SMS"
                Manifest.permission.READ_SMS -> "Read SMS"
                Manifest.permission.POST_NOTIFICATIONS -> "Post Notifications (Android 13+)"
                Manifest.permission.READ_MEDIA_IMAGES -> "Access Images (Android 13+)"
                Manifest.permission.READ_MEDIA_VIDEO -> "Access Videos (Android 13+)"
                Manifest.permission.READ_MEDIA_AUDIO -> "Access Audio Files (Android 13+)"
                Manifest.permission.MANAGE_EXTERNAL_STORAGE -> "Manage External Storage (Android 11+)"
                Manifest.permission.READ_EXTERNAL_STORAGE -> "Read External Storage"
                Manifest.permission.WRITE_EXTERNAL_STORAGE -> "Write External Storage"
                Manifest.permission.READ_PHONE_STATE -> "Read Phone State"
                Manifest.permission.CAMERA -> "Camera"
                else -> "Unknown Permission"
            }
        }

        val message = "The following permissions are required for the app to function properly:\n\n" +
                permissionDescriptions.joinToString("\n")

        AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage(message)
            .setPositiveButton("Grant") { _, _ ->
                val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = android.net.Uri.parse("package:$packageName")
                startActivity(intent)
            }
            .setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(this, "Permissions are required for proper functionality.", Toast.LENGTH_SHORT).show()
            }
            .show()
    }


    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            val deniedPermissions = permissions.zip(grantResults.toList())
                .filter { it.second != PackageManager.PERMISSION_GRANTED }
                .map { it.first }

            val allGranted = deniedPermissions.isEmpty()
            smsViewModel.setPermissionGranted(allGranted)

            if (!allGranted && shouldShowPermissionDialog) {
                showPermissionDialog(deniedPermissions)
            }
        }
    }

//    override fun onResume() {
//        super.onResume()
//        val deniedPermissions = getRequiredPermissions().filter { permission ->
//            ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
//        }
//
//        smsViewModel.setPermissionGranted(deniedPermissions.isEmpty())
//
//        if (deniedPermissions.isNotEmpty() && shouldShowPermissionDialog) {
//            showPermissionDialog(deniedPermissions)
//        }
//    }
}


