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

    // Binding and NavController
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var settingsDataStore: SettingsDataStore

    private var selectedImageUri: Uri? = null

    // Permissions and SMS Receiver
    private val permissionRequestCode = 101
    private lateinit var smsReceiver: SmsReceiver
    private var shouldShowPermissionDialog = true // Flag to control dialog behavior

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                try {
                    // Persist permission for future access
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                    Log.d("DsK", "Persisted URI permission successfully.")

                    // Save or use the URI
                    selectedImageUri = it
                    Log.d("DsK", "Main Activity image selectedImageUri $selectedImageUri")

                    // Now use the URI, for example, load into an ImageView
//                    fragmentSettingsAccountDetailsBinding?.ivProfile?.let {
//                        Utility.loadImageIntoView(it, selectedImageUri!!, requireContext(), isCircular = true)
//                    }
                } catch (e: SecurityException) {
                    e.printStackTrace()
                    Log.e("DsK", "Failed to persist permission: ${e.localizedMessage}")
                }
            }
        }

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

        // Request manage storage permission if needed
        requestManageStoragePermission() // This will check and request the permission for managing files

        // Observe user state
        homeDetailsViewModel.fetchUser()
        lifecycleScope.launch {
            homeDetailsViewModel.userDetails.observe(this@MainActivity) { user ->
                // Handle the collected user data
                if (user == null) {
                    // App-provided images
                    CommonDialog().showUserDialog(
                        context = this@MainActivity,
                        pickImageLauncher = pickImageLauncher,
                        onSave = { name, profilePictureUri, imageView ->
                            Log.d("DsK", "Main Activity selectedImageUri $selectedImageUri profilePictureUri $profilePictureUri")
                            if (name.isNotEmpty() && profilePictureUri != null) {
                                homeDetailsViewModel.saveUser(name, profilePictureUri.toString())
                            } else {
                                // Show a toast message if validation fails
                                Toast.makeText(
                                    this@MainActivity, "Details not saved", Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        preSelectedImageUri = selectedImageUri // Initially no image
                    )
                } else {
                    //  No Action needed since data is already there
                }
            }
        }
    }

    private fun requestManageStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // For Android 13 and above, request permissions for specific media types
            val requiredPermissions = mutableListOf<String>()

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }

            // If permissions are not granted, request them
            if (requiredPermissions.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, requiredPermissions.toTypedArray(), permissionRequestCode)
            } else {
                Log.d("DsK", "All media permissions already granted.")
            }
        } else {
            // Handle for devices running lower than Android 13 (API 33)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    // If external storage permission is granted
                    Log.d("DsK", "External storage permission granted")
                } else {
                    // Request permission to manage external storage for devices below Android 13
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        startActivityForResult(intent, permissionRequestCode)
                    } catch (e: ActivityNotFoundException) {
                        // Handle when this activity is not found
                        Log.e("DsK", "ActivityNotFoundException: Unable to open manage storage settings.")
                        Toast.makeText(this, "This device does not support managing all files.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // For devices below Android 11 (API 30), request specific external storage permissions
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), permissionRequestCode)
                }
            }
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
                    ContextCompat.checkSelfPermission(
                        this, permission
                    ) != PackageManager.PERMISSION_GRANTED
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
                appLoadingViewModel.fetchAndStoreCurrencies(
                    CurrencyUtils.loadCurrencyMapFromJSON(
                        this
                    )
                )
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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 and above
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            permissions.add(Manifest.permission.READ_MEDIA_IMAGES) // Access to images
            permissions.add(Manifest.permission.READ_MEDIA_VIDEO)  // Access to videos
            permissions.add(Manifest.permission.READ_MEDIA_AUDIO)  // Access to audio
        } else if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
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

        val message =
            "The following permissions are required for the app to function properly:\n\n" + permissionDescriptions.joinToString(
                "\n"
            )

        AlertDialog.Builder(this).setTitle("Permissions Required").setMessage(message)
            .setPositiveButton("Grant") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }.setNegativeButton("Cancel") { _, _ ->
                Toast.makeText(
                    this, "Permissions are required for proper functionality.", Toast.LENGTH_SHORT
                ).show()
            }.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == permissionRequestCode) {
            val deniedPermissions = permissions.zip(grantResults.toList())
                .filter { it.second != PackageManager.PERMISSION_GRANTED }.map { it.first }

            val allGranted = deniedPermissions.isEmpty()
            smsViewModel.setPermissionGranted(allGranted)

            if (!allGranted && shouldShowPermissionDialog) {
                showPermissionDialog(deniedPermissions)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(smsReceiver)
    }
}





